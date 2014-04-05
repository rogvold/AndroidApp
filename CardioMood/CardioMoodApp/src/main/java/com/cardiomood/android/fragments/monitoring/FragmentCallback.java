package com.cardiomood.android.fragments.monitoring;

/**
 * Created by danon on 04.04.2014.
 */
public interface FragmentCallback {

    void notifyBPM(int bpm);

    void notifyRRIntervals(short rr[]);

    void notifyConnectionStatus(int oldStatus, int newStatus);

    void notifyProgress(double progress, int count, long duration);

}
