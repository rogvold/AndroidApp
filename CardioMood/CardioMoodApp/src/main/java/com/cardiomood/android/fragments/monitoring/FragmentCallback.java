package com.cardiomood.android.fragments.monitoring;

import com.cardiomood.android.heartrate.CardioMoodHeartRateLeService;

/**
 * Created by danon on 04.04.2014.
 */
public interface FragmentCallback {

    void notifyBPM(CardioMoodHeartRateLeService service, int bpm);

    void notifyConnectionStatus(CardioMoodHeartRateLeService service, int oldStatus, int newStatus);

    void notifyProgress(CardioMoodHeartRateLeService service, double progress, int count, long duration);

}
