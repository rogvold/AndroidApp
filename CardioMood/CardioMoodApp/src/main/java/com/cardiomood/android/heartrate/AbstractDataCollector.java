package com.cardiomood.android.heartrate;

import android.util.Log;

import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;
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
import com.cardiomood.data.json.JsonError;
import com.cardiomood.data.json.JsonRRInterval;
import com.cardiomood.heartrate.bluetooth.HeartRateLeService;
import com.cardiomood.math.HeartRateMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by danon on 05.03.14.
 */
public abstract class AbstractDataCollector implements HeartRateLeService.DataCollector {

    private static final String TAG = AbstractDataCollector.class.getSimpleName();

    protected CardioMoodHeartRateLeService service = null;
    protected HeartRateMath math = new HeartRateMath(); // cache for data
    protected DataServiceHelper dataService;
    protected CardioSessionWithData cardioSession = null;

    private final HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO();

    private final PreferenceHelper preferenceHelper;

    private HeartRateSession currentSession = null;
    private List<HeartRateDataItem> heartRateDataItems = null;
    private int count = 0;
    private WorkerThread<CardioDataItem> serverSyncWorker;

    private Listener listener;

    private Status status = Status.NOT_STARTED;


    public AbstractDataCollector(CardioMoodHeartRateLeService service) {
        if (service == null) {
            throw new IllegalArgumentException("Service object is null");
        }
        this.service = service;
        this.preferenceHelper = new PreferenceHelper(service.getApplicationContext(), true);
        this.dataService = new DataServiceHelper(CardioMoodServer.INSTANCE.getService(), this.preferenceHelper);
    }

    @Override
    public void onConnected() {
        // do nothing
    }

    public void onStartCollecting() {
        // do nothing
        currentSession = new HeartRateSession();
        currentSession.setDateStarted(null);
        currentSession.setStatus(SessionStatus.NEW);
        Long userId = preferenceHelper.getLong(ConfigurationConstants.USER_ID, -1);
        if (userId < 0)
            userId = null;
        currentSession.setUserId(userId);

        heartRateDataItems = new ArrayList<HeartRateDataItem>();

        // call listener
        if (listener != null)
            listener.onStart();
    }

    public void onFirstDataRecieved() {
        currentSession.setDateStarted(new Date());

        // send to server
        dataService.createSession(new ServerResponseCallbackRetry<CardioSession>() {
            @Override
            public void retry() {
                dataService.createSession(this);
            }

            @Override
            public void onResult(CardioSession result) {
                if (result != null) {
                    result.setCreationTimestamp(currentSession.getDateStarted().getTime());
                    cardioSession = new CardioSessionWithData(result);
                    currentSession.setExternalId(cardioSession.getId());

                    serverSyncWorker = new ServerSyncWorkerThread(cardioSession);
                    serverSyncWorker.start();
                }
            }

            @Override
            public void onError(JsonError error) {
                Log.d(TAG, "createSession() failed: " + error);
            }
        });
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

        if (serverSyncWorker != null) {
            serverSyncWorker.finishWork();
        }

        if (getStatus() == Status.COLLECTING && enoughDataCollected()) {
            currentSession.setDateEnded(new Date());
            processCollectedData();
            // call listener
            if (listener != null)
                listener.onDataSaved(currentSession);
        } else {
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
                    public void onError(JsonError error) {

                    }
                });
            }
        }
        CommonTools.vibrate(service, 1000);
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
        if (getStatus() == Status.COLLECTING) {
            if (getIntervalsCount() == 0)
                onFirstDataRecieved();
            for (short rr: rrIntervals) {
                heartRateDataItems.add(new HeartRateDataItem(bpm, rr));
                math.addIntervals(rr);
                if (needToStopCollecting()) {
                    stopCollecting();
                    break;
                }
            }
            if (listener != null)
                listener.onDataAdded(bpm, rrIntervals);

            if (cardioSession != null) {
                for (short rr : rrIntervals) {
                    CardioDataItem dataItem = new CardioDataItem();
                    dataItem.setCreationTimestamp(System.currentTimeMillis());
                    dataItem.setSessionId(cardioSession.getId());
                    dataItem.setNumber((long) count++);
                    dataItem.setDataItem(new JsonRRInterval((int) rr).toString());
                    sendPendingData(dataItem);
                }
            }
        } else {
            Log.d(TAG, "addData() - ignored, status = " + getStatus());
        }
    }

    private void sendPendingData(CardioDataItem item) {
        if (serverSyncWorker != null) {
            serverSyncWorker.put(item);
        }
    }

    public HeartRateMath getData() {
        return math;
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
        currentSession = sessionDAO.insert(currentSession, heartRateDataItems);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int getIntervalsCount() {
        return math.getCount();
    }

    public double getDuration() {
        if (currentSession != null && currentSession.getDateStarted() != null) {
            return System.currentTimeMillis() - currentSession.getDateStarted().getTime();
        } else return 0;
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
        void onDataSaved(HeartRateSession session);
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
        public void onDataSaved(HeartRateSession session) {

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
            buffer.add(item);
            final CardioSessionWithData session = new CardioSessionWithData(cardioSession);
            session.setDataItems(buffer);
            dataService.appendDataToSession(session, new ServerResponseCallbackRetry<String>() {
                @Override
                public void retry() {
                    dataService.appendDataToSession(session, this);
                }

                @Override
                public void onResult(String result) {
                    buffer.clear();
                }

                @Override
                public void onError(JsonError error) {

                }
            });
        }

        @Override
        public void onStop() {
            currentSession.setStatus(SessionStatus.SYNCHRONIZED);
            currentSession = sessionDAO.merge(currentSession);
        }
    }
}
