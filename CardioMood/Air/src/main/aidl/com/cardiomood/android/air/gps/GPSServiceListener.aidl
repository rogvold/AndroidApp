// GPSServiceListener.aidl
package com.cardiomood.android.air.gps;

import android.location.Location;

interface GPSServiceListener {

    void onLocationChanged(in Location location);

    void onHeartRateChanged(int heartRate);

    void onStressChanged(double stress);

    void onTrackingSessionStarted(String userId, String aircraftId, String airSessionId);

    void onTrackingSessionFinished();

}
