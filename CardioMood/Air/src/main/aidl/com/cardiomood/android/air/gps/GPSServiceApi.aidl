// GPSServiceApi.aidl
package com.cardiomood.android.air.gps;

import com.cardiomood.android.air.gps.GPSServiceListener;

interface GPSServiceApi {

    /**
     * Add listener to be notified about things going in the server
     */
    void addListener(GPSServiceListener listener);

    /**
     * Remove previously added listener
     */
    void removeListener(GPSServiceListener listener);

    /**
     * Creates a tracking session and starts tracking for specified user and aircraft
     */
    void startTrackingSession(String userId, String aircraftId);

    /**
     * Finishes tracking session and stops service
     */
    void stopTrackingSession();

    /**
     * Checks whether the service is running (has ongoing tracking session)
     */
    boolean isRunning();

    /**
     * Show notification
     */
    void showNotification();

    /**
     * Hide notification
     */
    void hideNotification();

    boolean initBLE();

    void connectHRMonitor(String address);

    void disconnectHRMonitor();

    boolean isHRMonitorConnected();

    int getHrmStatus();

    String getHrmAddress();

    String getHrmName();

    int getCurrentHeartRate();

    String getAirSessionId();

    String getAircraftId();

    String getUserId();

}
