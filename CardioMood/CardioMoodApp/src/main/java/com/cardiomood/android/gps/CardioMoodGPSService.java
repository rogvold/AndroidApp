package com.cardiomood.android.gps;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Created by danon on 06.06.2014.
 */
public class CardioMoodGPSService extends Service {

    private static final String TAG = CardioMoodGPSService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();
    private GPSMonitor monitor;
    private DataCollector dataCollector;

    public class LocalBinder extends Binder {
        public CardioMoodGPSService getService() {
            return CardioMoodGPSService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void close() {
        stop();
        monitor = null;
    }


    public void stop() {
        if (monitor == null)
            return;
        if (monitor.isRunning()) {
            monitor.stop();
            if (dataCollector != null)
                dataCollector.onDisconnected();
        }
    }

    public void start() {
        if (monitor == null)
            monitor = createGPSMonitor();
        if (!monitor.isRunning()) {
            monitor.start();
            if (monitor.isRunning() && dataCollector != null)
                dataCollector.onConnected();
        }
    }

    public boolean isRunning() {
        return monitor.isRunning();
    }

    public boolean isGPSEnabled() {
        return monitor.isGPSEnabled();
    }

    public Location getLastKnownLocation() {
        return monitor.getLastKnownLocation();
    }

    public Context getContext() {
        return monitor.getContext();
    }

    public Location getCurrentLocation() {
        return monitor.getCurrentLocation();
    }

    public long getCurrentLocationTimestamp() {
        return monitor.getCurrentLocationTimestamp();
    }

    public DataCollector getDataCollector() {
        return dataCollector;
    }

    public void setDataCollector(DataCollector dataCollector) {
        this.dataCollector = dataCollector;
    }

    public static interface DataCollector {

        void onConnected();

        void addData(Location location);

        void onDisconnected();

    }

    private GPSMonitor createGPSMonitor() {
        final GPSMonitor gpsMonitor = new GPSMonitor(this);
        gpsMonitor.setListener(new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (dataCollector != null)
                    dataCollector.addData(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
        return gpsMonitor;
    }

}
