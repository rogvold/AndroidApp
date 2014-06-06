package com.cardiomood.android.gps;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.entity.GPSLocationEntity;
import com.cardiomood.android.db.entity.GPSSessionEntity;
import com.cardiomood.android.db.entity.SessionStatus;
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
import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by danon on 01.06.2014.
 */
public class GPSDataCollector implements CardioMoodGPSService.DataCollector {

    private static final String TAG = GPSDataCollector.class.getSimpleName();

    protected DataServiceHelper dataService;
    protected CardioSessionWithData cardioSession = null;

    private RuntimeExceptionDao<GPSSessionEntity, Long> sessionDAO;
    private RuntimeExceptionDao<GPSLocationEntity, Long> gpsLocationDAO;
    private DatabaseHelper databaseHelper;

    private final PreferenceHelper preferenceHelper;

    private GPSSessionEntity currentSession = null;
    private List<GPSLocationEntity> dataItems = null;
    private List<CardioDataItem> pendingData = null;
    private WorkerThread<CardioDataItem> serverSyncWorker;
    private int count;

    private Listener listener;

    private Status status = Status.NOT_STARTED;
    private boolean creatingSession = false;


    public GPSDataCollector(Context context, DatabaseHelper helper) {
        preferenceHelper = new PreferenceHelper(context, true);
        this.databaseHelper = helper;

        this.dataService = new DataServiceHelper(CardioMoodServer.INSTANCE.getService(), this.preferenceHelper);
        sessionDAO = helper.getRuntimeExceptionDao(GPSSessionEntity.class);
        gpsLocationDAO = helper.getRuntimeExceptionDao(GPSLocationEntity.class);
    }

    public void onStartCollecting() {
        // do nothing
        currentSession = new GPSSessionEntity();
        currentSession.setDateStarted(null);
        currentSession.setStatus(SessionStatus.NEW);
        Long userId = preferenceHelper.getLong(ConfigurationConstants.USER_ID, -1);
        if (userId < 0)
            userId = null;
        currentSession.setUserId(userId);

        dataItems = new ArrayList<GPSLocationEntity>();
        pendingData = Collections.synchronizedList(new ArrayList<CardioDataItem>());

        // call listener
        if (listener != null)
            listener.onStart();
    }

    public void onFirstDataReceived() {
        if (currentSession.getDateStarted() == null) {
            currentSession.setDateStarted(new Date());
            currentSession.setStatus(SessionStatus.IN_PROGRESS);
            currentSession = sessionDAO.createIfNotExists(currentSession);
            // send to server
            attemptCreateCardiomoodSession();
        }
    }

    private void attemptCreateCardiomoodSession() {
        if (!creatingSession) {
            creatingSession = true;
            dataService.createSession("JsonGPS", new ServerResponseCallbackRetry<CardioSession>() {
                @Override
                public void retry() {
                    dataService.createSession("JsonGPS", this);
                }

                @Override
                public void onResult(CardioSession result) {
                    Log.w(TAG, "createSession().onResult(): " + result);
                    if (result != null) {
                        result.setCreationTimestamp(currentSession.getDateStarted().getTime());
                        cardioSession = new CardioSessionWithData(result);
                        Log.w(TAG, "attemptCreateCardiomoodSession() -> onResult(): externalId=" + cardioSession.getId());
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

        if (serverSyncWorker != null) {
            serverSyncWorker.finishWork();
        }

        if (getStatus() == Status.COLLECTING && enoughDataCollected()) {
            if (currentSession != null) {
                currentSession.setDateEnded(new Date());
                currentSession.setStatus(SessionStatus.COMPLETED);
                processCollectedData();
                // call listener
                if (listener != null)
                    listener.onDataSaved(currentSession);
            }
        } else {
            if (currentSession != null) {
                sessionDAO.deleteById(currentSession.getId());
            }
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
        }
    }

    protected void processCollectedData() {
        sessionDAO.update(currentSession);
    }

    @Override
    public void onConnected() {
        startCollecting();
    }

    public void addData(Location location) {
        if (getStatus() == Status.COLLECTING) {
            if (count == 0)
                onFirstDataReceived();

            GPSLocationEntity gpsItem = new GPSLocationEntity();
            dataItems.add(gpsItem);
            gpsItem.setLat(location.getLatitude());
            gpsItem.setLon(location.getLongitude());
            if (location.hasAltitude())
                gpsItem.setAlt(location.getAltitude());
            if (location.hasSpeed())
                gpsItem.setSpeed((double) location.getSpeed());
            if (location.hasBearing())
                gpsItem.setBearing((double) location.getBearing());
            if (location.hasAccuracy())
                gpsItem.setAccuracy((double) location.getAccuracy());
            gpsItem.setSessionId(currentSession.getId());
            gpsLocationDAO.create(gpsItem);

            CardioDataItem dataItem = new CardioDataItem();
            dataItem.setCreationTimestamp(gpsItem.getTimestamp().getTime());
            dataItem.setNumber((long) count++);
            dataItem.setDataItem(gpsItem.toJsonGPS().toString());
            pendingData.add(dataItem);
            sendPendingData();
            if (listener != null)
                listener.onDataAdded(location);
        } else {
            Log.d(TAG, "addData() - ignored, status = " + getStatus());
        }
    }

    @Override
    public void onDisconnected() {
        stopCollecting();
    }


    private void sendPendingData() {
        if (serverSyncWorker != null) {
            for (CardioDataItem dataItem: pendingData) {
                serverSyncWorker.put(dataItem);
            }
            pendingData.clear();
        } else {
            attemptCreateCardiomoodSession();
        }
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

    public Status getStatus() {
        return status;
    }

    public void startCollecting() {
        setStatus(Status.COLLECTING);
    }

    public void stopCollecting() {
        setStatus(Status.COMPLETED);
    }

    public boolean enoughDataCollected() {
        return count > 0;
    }


    public static enum Status {
        NOT_STARTED,
        COLLECTING,
        COMPLETED
    }

    public static interface Listener {
        void onComplete();
        void onStart();
        void onDataAdded(Location location);
        void onDataSaved(GPSSessionEntity session);
    }

    public static class SimpleListener implements Listener {

        @Override
        public void onComplete() {

        }

        @Override
        public void onStart() {

        }

        @Override
        public void onDataAdded(Location location) {

        }

        @Override
        public void onDataSaved(GPSSessionEntity session) {

        }
    }

    private class ServerSyncWorkerThread extends WorkerThread<CardioDataItem> {

        private CardioSession cardioSession;
        // private List<CardioDataItem> internetQueue = Collections.synchronizedList(new ArrayList<CardioDataItem>());

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
            if (enoughDataCollected()) {
                currentSession.setStatus(SessionStatus.SYNCHRONIZED);
                sessionDAO.update(currentSession);
            }
        }
    }

}
