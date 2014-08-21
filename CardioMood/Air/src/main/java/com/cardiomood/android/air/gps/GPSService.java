package com.cardiomood.android.air.gps;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cardiomood.android.air.R;
import com.cardiomood.android.air.TrackingActivity;
import com.cardiomood.android.air.data.AirSession;
import com.cardiomood.android.air.data.Aircraft;
import com.cardiomood.android.air.data.DataPoint;
import com.cardiomood.android.tools.thread.WorkerThread;
import com.google.gson.Gson;
import com.parse.ParseCloud;
import com.parse.ParseException;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by danon on 06.06.2014.
 */
public class GPSService extends Service {

    private static final String TAG = GPSService.class.getSimpleName();
    private static final int SERVICE_NOTIFICATION_ID = 4242;

    public static boolean isServiceStarted = false;

    private final IBinder mBinder = new LocalBinder();
    private GPSMonitor monitor;
    private AirSession airSession;
    private Aircraft aircraft;

    private DataCollectorThread workerThread;
    private NotificationCompat.Builder mBuilder;

    public class LocalBinder extends Binder {
        public GPSService getService() {
            return GPSService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");
        return super.onUnbind(intent);
    }




    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        close();
        super.onDestroy();
    }

    public void close() {
        stop();
        stopSelf();
        mBuilder = null;
        monitor = null;
        workerThread = null;
        airSession = null;
        aircraft = null;
    }


    public void stop() {
        Log.d(TAG, "stop()");

        isServiceStarted = false;

        // cancel notification
        hideNotification();

        if (monitor == null)
            return;

        if (workerThread != null && workerThread.isAlive()) {
            workerThread.finishWork();
        }

        if (monitor.isRunning()) {
            monitor.stop();
        }
    }

    public void start() {
        Log.d(TAG, "start()");

        if (monitor == null)
            monitor = createGPSMonitor();
        try {
            if (!monitor.isRunning()) {
                monitor.start();
                workerThread = new DataCollectorThread();
                workerThread.setSession(airSession);
                workerThread.start();
            }

            // set up notification builder
            if (mBuilder == null)
                mBuilder = setupNotificationBuilder();

            isServiceStarted = true;
        } catch (Exception ex) {
            Log.e(TAG, "failed to start GPS tracking!", ex);
        }

    }

    public void showNotification() {
        if (isServiceStarted && mBinder != null) {
            // create notification
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(SERVICE_NOTIFICATION_ID, mBuilder.build());
        }
    }

    public void hideNotification() {
        // create notification
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.cancel(SERVICE_NOTIFICATION_ID);
    }

    private NotificationCompat.Builder setupNotificationBuilder() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(aircraft.getName() + " :: " + getAircraft().getAircraftId())
                        .setContentText("Click to open flight tracker")
                        .setOngoing(true);

        Intent resultIntent = new Intent(this, TrackingActivity.class);
        resultIntent.putExtra(TrackingActivity.GET_PLANE_FROM_SERVICE, true);
        //resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = null;

        if (Build.VERSION.SDK_INT >= 16) {
            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(TrackingActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
        } else {
            resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
        }
        mBuilder.setContentIntent(resultPendingIntent);

        return mBuilder;
    }

    public boolean isRunning() {
        return monitor == null ? false : monitor.isRunning();
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

    public AirSession getAirSession() {
        return airSession;
    }

    public void setAirSession(AirSession airSession) {
        this.airSession = airSession;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
    }

    private GPSMonitor createGPSMonitor() {
        final GPSMonitor gpsMonitor = new GPSMonitor(this);
        gpsMonitor.setListener(new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
               if (workerThread != null)
                   workerThread.put(getDataPoint(location));
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

    private static DataPoint getDataPoint(Location loc) {
        DataPoint dp = new DataPoint();
        dp.setLat(loc.getLatitude());
        dp.setLon(loc.getLongitude());
        dp.setAlt(loc.hasAltitude() ? loc.getAltitude() : null);
        dp.setAcc(loc.hasAccuracy() ? loc.getAccuracy() : null);
        dp.setBea(loc.hasBearing() ? loc.getBearing() : null);
        dp.setVel(loc.hasSpeed() ? loc.getSpeed() : null);
        dp.setT(loc.getTime());
        dp.setHR(72);
        dp.setStress(94);
        return dp;
    }

    private class DataCollectorThread extends WorkerThread<DataPoint> {

        private final Gson GSON = new Gson();
        private final List<DataPoint> portion = new ArrayList<DataPoint>();

        private final long INTERVAL = 2000L;

        private long lastSuccessTime;
        private AirSession session = null;

        public void setSession(AirSession session) {
            this.session = session;
        }

        @Override
        public void onStop() {
            if (session != null) {
                session.setEndDate(getLastItemTime());
                session.saveEventually();
            }
        }

        @Override
        protected void onCycle() {
            if (!portion.isEmpty()) {
                if (System.currentTimeMillis() - lastSuccessTime >= INTERVAL) {
                    long time = System.currentTimeMillis();
                    if (processItems(portion)) {
                        lastSuccessTime = time;
                        portion.clear();
                    }
                }
            }
        }

        @Override
        public void processItem(DataPoint item) {
            portion.add(item);
            if (hasMoreElements() || session == null)
                return;
        }

        private boolean processItems(List<DataPoint> items) {
            if (session == null)
                return false;

            Map<String, Object> params = new HashMap<String, Object>();
            try {
                params.put("sessionId", session.getObjectId());
                params.put("points", new JSONArray(GSON.toJson(items)));
            } catch (Exception ex) {
                throw new RuntimeException("Failed to prepare params", ex);
            }

            try {
                ParseCloud.callFunction("saveNewPoints", params);
            } catch (ParseException ex) {
                Log.w(TAG, "processItems() -> saveNewPoints() cloud function call failed", ex);
                return false;
            }

            return true;
        }
    }

}
