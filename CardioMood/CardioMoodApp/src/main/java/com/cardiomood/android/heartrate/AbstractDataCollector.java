package com.cardiomood.android.heartrate;

import android.util.Log;

import com.cardiomood.heartrate.bluetooth.HeartRateLeService;
import com.cardiomood.math.HeartRateMath;

/**
 * Created by danon on 05.03.14.
 */
public abstract class AbstractDataCollector implements HeartRateLeService.DataCollector {

    private static final String TAG = AbstractDataCollector.class.getSimpleName();

    protected CardioMoodHeartRateLeService service = null;
    protected HeartRateMath math = new HeartRateMath();

    private Status status = Status.NOT_STARTED;


    public AbstractDataCollector(CardioMoodHeartRateLeService service) {
        if (service == null) {
            throw new IllegalArgumentException("Service object is null");
        }
        this.service = service;
    }

    @Override
    public void onConnected() {
        // do nothing
    }

    public void onStartCollecting() {
        // do nothing
    }

    public void onCompleteCollecting() {
        try {
            service.disconnect();
        } catch (Exception ex) {
            Log.w(TAG, "Failed to disconnect from service.", ex);
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
                math.addIntervals(rr);
                if (needToStopCollecting()) {
                    stopCollecting();
                    break;
                }
            }
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


    public static enum Status {
        NOT_STARTED,
        COLLECTING,
        COMPLETED
    }
}
