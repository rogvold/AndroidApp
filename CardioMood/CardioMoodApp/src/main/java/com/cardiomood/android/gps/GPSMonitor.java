package com.cardiomood.android.gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.cardiomood.android.tools.ConfigurationManager;
import com.cardiomood.android.tools.config.ConfigurationConstants;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 23:59
 */
public class GPSMonitor implements ConfigurationConstants {

    private static final String TAG = GPSMonitor.class.getSimpleName();

    private final ConfigurationManager config = ConfigurationManager.getInstance();
    private final Context context;
    private final LocationManager locationManager;
    private LocationListener listener;
    private boolean running = false;

    private Location currentLocation = null;
    private long currentLocationTimestamp = 0;
    private final LocationListener _listener = new Listener();

    public GPSMonitor(Context context) {
        this(context, null);
    }

    public GPSMonitor(Context context, LocationListener listener) {
        this.listener = listener;
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public Location getLastKnownLocation() {
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    public Context getContext() {
        return context;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public LocationListener getListener() {
        return listener;
    }

    public void setListener(LocationListener listener) {
        this.listener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    public long getCurrentLocationTimestamp() {
        return currentLocationTimestamp;
    }

    public void start() {
        if (running)
            return;
        if (isGPSEnabled()) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    config.getInt(GPS_UPDATE_TIME, 1000),
                    config.getFloat(GPS_UPDATE_DISTANCE, 5.0f),
                    _listener
            );
            running = true;
        } else {
            stop();
            Toast.makeText(context, "Please, allow access to GPS location provider.", Toast.LENGTH_SHORT).show();
        }
    }

    public void stop() {
        running = false;
        locationManager.removeUpdates(_listener);
    }

    private class Listener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged(): lat="+location.getLatitude() + ", long=" + location.getLongitude());
            if (listener != null)
                listener.onLocationChanged(location);
            currentLocation = location;
            currentLocationTimestamp = System.currentTimeMillis();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (listener != null)
                listener.onStatusChanged(provider, status, extras);
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (listener != null)
                listener.onProviderEnabled(provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (listener != null)
                listener.onProviderDisabled(provider);
        }
    }
}
