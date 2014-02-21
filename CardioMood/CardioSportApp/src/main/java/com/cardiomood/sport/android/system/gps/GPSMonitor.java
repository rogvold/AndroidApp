package com.cardiomood.sport.android.system.gps;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.cardiomood.android.tools.ConfigurationManager;
import com.cardiomood.sport.android.tools.config.ConfigurationConstants;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 23:59
 */
public class GPSMonitor implements ConfigurationConstants {

    private final ConfigurationManager config = ConfigurationManager.getInstance();
    private final Activity activity;
    private final LocationManager locationManager;
    private LocationListener listener;
    private boolean collecting = false;

    private Location currentLocation = null;
    private final LocationListener _listener = new Listener();

    public GPSMonitor(Activity activity) {
        this(activity, null);
    }

    public GPSMonitor(Activity activity, LocationListener listener) {
        this.listener = listener;
        this.activity = activity;
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public Location getLastKnownLocation() {
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    public Activity getActivity() {
        return activity;
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

    public boolean isCollecting() {
        return collecting;
    }

    public void start() {
        if (collecting)
            return;
        if (isGPSEnabled()) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    config.getInt(GPS_UPDATE_TIME, 500),
                    config.getFloat(GPS_UPDATE_DISTANCE, 1.0f),
                    _listener
            );
            collecting = true;
        } else {
            stop();
            Toast.makeText(activity, "Please, allow access to GPS location provider.", Toast.LENGTH_SHORT).show();
        }
    }

    public void stop() {
        collecting = false;
        locationManager.removeUpdates(_listener);
    }

    private class Listener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (listener != null)
                listener.onLocationChanged(location);
            currentLocation = location;
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
