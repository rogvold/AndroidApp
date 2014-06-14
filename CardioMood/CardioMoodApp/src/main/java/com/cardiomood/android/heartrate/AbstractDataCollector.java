package com.cardiomood.android.heartrate;

import android.util.Log;

import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.entity.ContinuousSessionEntity;
import com.cardiomood.android.db.entity.RRIntervalEntity;
import com.cardiomood.android.db.entity.SessionStatus;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.WorkerThread;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.data.CardioMoodServer;
import com.cardiomood.data.DataServiceHelper;
import com.cardiomood.data.async.ServerResponseCallbackRetry;
import com.cardiomood.data.json.CardioDataItem;
import com.cardiomood.data.json.CardioSession;
import com.cardiomood.data.json.CardioSessionWithData;
import com.cardiomood.data.json.JSONError;
import com.cardiomood.data.json.JSONResponse;
import com.cardiomood.heartrate.bluetooth.HeartRateLeService;
import com.cardiomood.math.DataWindowSet;
import com.cardiomood.math.window.DataWindow;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by danon on 05.03.14.
 */
public abstract class AbstractDataCollector implements HeartRateLeService.DataCollector {

    private static final String TAG = AbstractDataCollector.class.getSimpleName();
    private static final String DATA_CLASS_NAME = "JsonRRInterval";

    protected CardioMoodHeartRateLeService service = null;
    protected DataWindowSet math = new DataWindowSet(); // cache for data
    protected DataServiceHelper dataService;
    protected CardioSessionWithData cardioSession = null;

    private RuntimeExceptionDao<ContinuousSessionEntity, Long> sessionDAO;
    private RuntimeExceptionDao<RRIntervalEntity, Long> hrItemDAO;
    private DatabaseHelper databaseHelper;

    private final PreferenceHelper preferenceHelper;

    private ContinuousSessionEntity currentSession = null;
    private List<RRIntervalEntity> heartRateDataItems = null;
    private List<CardioDataItem> pendingData = null;
    private int count = 0;
    private WorkerThread<CardioDataItem> serverSyncWorker;

    private Listener listener;

    private Status status = Status.NOT_STARTED;
    private volatile boolean creatingSession = false;


    public AbstractDataCollector(CardioMoodHeartRateLeService service, DatabaseHelper helper) {
        if (service == null) {
            throw new IllegalArgumentException("Service object is null");
        }
        this.databaseHelper = helper;
        this.service = service;
        this.preferenceHelper = new PreferenceHelper(service, true);
        this.dataService = new DataServiceHelper(CardioMoodServer.INSTANCE.getService(), this.preferenceHelper);

        sessionDAO = helper.getRuntimeExceptionDao(ContinuousSessionEntity.class);
        hrItemDAO = helper.getRuntimeExceptionDao(RRIntervalEntity.class);
    }

    @Override
    public void onConnected() {
        // do nothing
    }

    public void onStartCollecting() {
        // do nothing
        currentSession = new ContinuousSessionEntity();
        currentSession.setDateStarted(null);
        currentSession.setStatus(SessionStatus.NEW);
        Long userId = preferenceHelper.getLong(ConfigurationConstants.USER_ID, -1);
        if (userId < 0)
            userId = null;
        currentSession.setUserId(userId);
        currentSession.setDataClassName(DATA_CLASS_NAME);

        heartRateDataItems = new ArrayList<RRIntervalEntity>();
        pendingData = Collections.synchronizedList(new ArrayList<CardioDataItem>());

        // call listener
        if (listener != null)
            listener.onStart();
    }

    public void onFirstDataRecieved() {
        currentSession.setDateStarted(new Date());
        currentSession.setStatus(SessionStatus.IN_PROGRESS);
        currentSession = sessionDAO.createIfNotExists(currentSession);
    }

    private void attemptCreateCardioSession() {
        if (preferenceHelper.getBoolean(ConfigurationConstants.SYNC_DISABLE_REAL_TIME, false, true))
            return;

        if (!creatingSession) {
            creatingSession = true;
            dataService.createSession(DATA_CLASS_NAME, currentSession.getDateStarted().getTime(), new ServerResponseCallbackRetry<CardioSession>() {
                @Override
                public void retry() {
                    dataService.createSession(DATA_CLASS_NAME, currentSession.getDateStarted().getTime(), this);
                }

                @Override
                public void onResult(CardioSession result) {
                    Log.w(TAG, "createSession().onResult(): " + result);
                    if (result != null) {
                        result.setCreationTimestamp(currentSession.getDateStarted().getTime());
                        cardioSession = new CardioSessionWithData(result);
                        currentSession.setExternalId(cardioSession.getId());
                        sessionDAO.update(currentSession);

                        serverSyncWorker = new ServerSyncWorkerThread(cardioSession);
                        serverSyncWorker.start();
                    }
                }

                @Override
                public void onError(JSONError error) {
                    creatingSession = false;
                    Log.d(TAG, "createSession() failed: " + error);
                }
            });
        }
    }


    public void onCompleteCollecting() {
        // call listener
        if (listener != null)
            listener.onComplete();

        try {
            service.disconnect();
        } catch (Exception ex) {
            Log.w(TAG, "Failed to disconnect from service.", ex);
        }

        if (getStatus() == Status.COLLECTING && enoughDataCollected()) {
            // set COMPLETED status and call notify listener
            if (currentSession != null) {
                currentSession.setDateEnded(new Date());
                currentSession.setStatus(SessionStatus.COMPLETED);
                processCollectedData();
                // call listener
                if (listener != null)
                    listener.onDataSaved(currentSession);
            }
            // safely finish online synchronization thread
            if (serverSyncWorker != null) {
                serverSyncWorker.finishWork();
            }
            CommonTools.vibrate(service, 1000);
        } else {
            // interrupt synchronization thread
            if (serverSyncWorker != null) {
                serverSyncWorker.interrupt();
            }
            // delete session from the server
            if (cardioSession != null) {
                dataService.deleteSession(cardioSession.getId(), new ServerResponseCallbackRetry<String>() {
                    @Override
                    public void retry() {
                        dataService.deleteSession(cardioSession.getId(), this);
                    }

                    @Override
                    public void onResult(String result) {
                        cardioSession = null;
                    }

                    @Override
                    public void onError(JSONError error) {

                    }
                });
            }
            // delete session locally
            if (currentSession != null) {
                sessionDAO.deleteById(currentSession.getId());
            }
        }
        math.clear();
    }

    @Override
    public void onDisconnected() {
        try {
            service.close();
        } catch (Exception ex) {
            Log.w(TAG, "Failed to close service after disconnect.", ex);
        }
    }

    @Override
    public void addData(int bpm, short[] rrIntervals) {
        if (rrIntervals.length == 0)
            return;
        synchronized (this) {
            if (getStatus() == Status.COLLECTING) {
                if (getIntervalsCount() == 0)
                    onFirstDataRecieved();
                long timestamp = System.currentTimeMillis();
                for (short rr : rrIntervals)
                    timestamp -= rr;
                for (short rr : rrIntervals) {
                    math.addIntervals(rr);
                    RRIntervalEntity hrItem = new RRIntervalEntity(timestamp, bpm, rr);
                    hrItem.setSession(currentSession);
                    heartRateDataItems.add(hrItem);
                    hrItemDAO.create(hrItem);
                    currentSession.setLastModified(timestamp);
                    sessionDAO.update(currentSession);

                    CardioDataItem dataItem = hrItem.toCardioDataItem();
                    dataItem.setNumber((long) count++);
                    pendingData.add(dataItem);

                    timestamp += rr;
                    if (needToStopCollecting()) {
                        sendPendingData();
                        stopCollecting();
                        break;
                    }
                }
                if (listener != null)
                    listener.onDataAdded(bpm, rrIntervals);
                sendPendingData();
            } else {
                for (short rr : rrIntervals) {
                    math.addIntervals(rr);
                }
                Log.d(TAG, "addData() - ignored, status = " + getStatus());
            }
        }
    }

    private void sendPendingData() {
        if (serverSyncWorker != null) {
            for (CardioDataItem dataItem: pendingData) {
                serverSyncWorker.put(dataItem);
            }
            pendingData.clear();
        } else {
            attemptCreateCardioSession();
        }
    }

    public List<RRIntervalEntity> getData() {
        return heartRateDataItems;
    }

    public Status getStatus() {
        return status;
    }

    protected void setStatus(Status status) {
        if (status != this.status) {
            if (this.status == Status.NOT_STARTED && status == Status.COLLECTING)
                onStartCollecting();
            else if (this.status == Status.COLLECTING && status == Status.COMPLETED)
                onCompleteCollecting();
            else if (this.status == Status.NOT_STARTED && status == Status.COMPLETED)
                onCompleteCollecting();
            else throw new IllegalStateException("Illegal status change: " + this.status + " => " + status);
            this.status = status;
        }
    }

    protected abstract boolean needToStopCollecting();

    public abstract double getProgress();

    public void startCollecting() {
        setStatus(Status.COLLECTING);
    }

    public void stopCollecting() {
        setStatus(Status.COMPLETED);
    }

    public boolean enoughDataCollected() {
        return getIntervalsCount() > 30;
    }

    protected void processCollectedData() {
        sessionDAO.update(currentSession);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int getIntervalsCount() {
        return heartRateDataItems == null ? 0 : heartRateDataItems.size();
    }

    public double getDuration() {
        if (currentSession != null && currentSession.getDateStarted() != null) {
            return System.currentTimeMillis() - currentSession.getDateStarted().getTime();
        } else return 0;
    }

    public void addWindow(DataWindow window) {
        math.addWindow(window);
    }

    public void removeWindow(DataWindow window) {
        math.removeWindow(window);
    }

    public static enum Status {
        NOT_STARTED,
        COLLECTING,
        COMPLETED
    }

    public static interface Listener {
        void onComplete();
        void onStart();
        void onDataAdded(int bpm, short rr[]);
        void onDataSaved(ContinuousSessionEntity session);
    }

    public static class SimpleListener implements Listener {

        @Override
        public void onComplete() {

        }

        @Override
        public void onStart() {

        }

        @Override
        public void onDataAdded(int bpm, short[] rr) {

        }

        @Override
        public void onDataSaved(ContinuousSessionEntity session) {

        }
    }

    private class ServerSyncWorkerThread extends WorkerThread<CardioDataItem> {

        private CardioSession cardioSession;
        private List<CardioDataItem> buffer = Collections.synchronizedList(new ArrayList<CardioDataItem>());

        private ServerSyncWorkerThread(CardioSession cardioSession) {
            this.cardioSession = cardioSession;
        }

        @Override
        public void processItem(CardioDataItem item) {
            item.setSessionId(cardioSession.getId());
            buffer.add(item);
            if (getQueueSize() > 0)
                return;
            cardioSession.setLastModificationTimestamp(currentSession.getLastModified());
            final CardioSessionWithData session = new CardioSessionWithData(cardioSession);
            session.setDataItems(buffer);
            JSONResponse<String> response = dataService.appendDataToSession(session);
            if (response != null) {
                if (response != null && response.isOk()) {
                    buffer.clear();
                } else {
                    JSONError error = response.getError();
                    if (error != null && error.getCode() == JSONError.INVALID_TOKEN_ERROR) {
                        dataService.refreshToken(true);
                        response = dataService.appendDataToSession(session);
                        if (response != null || response.isOk()) {
                            buffer.clear();
                        }
                    }
                }
            }
        }

        @Override
        public void onStop() {
            if (isFinished()) {
                // update session status locally
                currentSession.setStatus(SessionStatus.SYNCHRONIZING);
                sessionDAO.update(currentSession);

                // mark session as finished remotely
                JSONResponse<String> response =  dataService.finishSession(currentSession.getExternalId(), currentSession.getDateEnded().getTime());
                Log.w(TAG, "finishSession(): response = " + response);
                if (response.isOk()) {
                    currentSession.setStatus(SessionStatus.SYNCHRONIZED);
                    sessionDAO.update(currentSession);
                } else {
                    currentSession.setStatus(SessionStatus.COMPLETED);
                    sessionDAO.update(currentSession);
                }
            }
        }
    }
}
