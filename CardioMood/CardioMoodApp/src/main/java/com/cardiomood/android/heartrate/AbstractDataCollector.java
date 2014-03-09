package com.cardiomood.android.heartrate;

import android.util.Log;

import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.heartrate.bluetooth.HeartRateLeService;
import com.cardiomood.math.HeartRateMath;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by danon on 05.03.14.
 */
public abstract class AbstractDataCollector implements HeartRateLeService.DataCollector {

    private static final String TAG = AbstractDataCollector.class.getSimpleName();

    protected CardioMoodHeartRateLeService service = null;
    protected HeartRateMath math = new HeartRateMath(); // cache for data

    private final HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO();
    private final HeartRateDataItemDAO hrItemDAO = new HeartRateDataItemDAO();

    private final PreferenceHelper preferenceHelper;

    private HeartRateSession currentSession = null;
    private List<HeartRateDataItem> heartRateDataItems = null;

    private Listener listener;

    private Status status = Status.NOT_STARTED;


    public AbstractDataCollector(CardioMoodHeartRateLeService service) {
        if (service == null) {
            throw new IllegalArgumentException("Service object is null");
        }
        this.service = service;
        this.preferenceHelper = new PreferenceHelper(service.getApplicationContext(), true);
    }

    @Override
    public void onConnected() {
        // do nothing
    }

    public void onStartCollecting() {
        // do nothing
        currentSession = new HeartRateSession();
        currentSession.setDateStarted(new Date());
        currentSession.setStatus(SessionStatus.NEW);
        Long userId = preferenceHelper.getLong(ConfigurationConstants.USER_ID, -1);
        if (userId < 0)
            userId = null;
        currentSession.setUserId(userId);

        heartRateDataItems = new ArrayList<HeartRateDataItem>();

        // save to DB????
        if (listener != null)
            listener.onStart();
    }

    public void onCompleteCollecting() {
        if (listener != null)
            listener.onComplete();

        try {
            service.disconnect();
        } catch (Exception ex) {
            Log.w(TAG, "Failed to disconnect from service.", ex);
        }

        if (enoughDataCollected()) {
            currentSession.setDateEnded(new Date());
            processCollectedData();
            if (listener != null)
                listener.onDataSaved(currentSession);
        }
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
        } else {
            Log.d(TAG, "addData() - ignored, status = " + getStatus());
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
        return true;
    }

    protected void processCollectedData() {
        currentSession = sessionDAO.insert(currentSession, heartRateDataItems);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
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
}
