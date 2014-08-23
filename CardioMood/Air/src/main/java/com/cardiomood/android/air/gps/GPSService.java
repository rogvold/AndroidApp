package com.cardiomood.android.air.gps;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cardiomood.android.air.AirApplication;
import com.cardiomood.android.air.R;
import com.cardiomood.android.air.TrackingActivity;
import com.cardiomood.android.air.data.AirSession;
import com.cardiomood.android.air.data.Aircraft;
import com.cardiomood.android.air.data.DataPoint;
import com.cardiomood.android.tools.thread.WorkerThread;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.google.gson.Gson;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by danon on 06.06.2014.
 */
public class GPSService extends Service {

    private static final String TAG = GPSService.class.getSimpleName();
    private static final int SERVICE_NOTIFICATION_ID = 4242;

    public static volatile boolean isServiceStarted = false;

    private GPSMonitor gpsMonitor;
    private LeHRMonitor hrMonitor;
    private AirSession airSession;

    private DataCollectorThread workerThread;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private Handler mHandler;
    private Task<Void> mFinishingTask;

    private final Object lock = new Object();
    private final List<GPSServiceListener> listeners = new ArrayList<GPSServiceListener>();

    private GPSServiceApi.Stub apiEndpoint = new GPSServiceApi.Stub() {

        @Override
        public void addListener(GPSServiceListener listener) throws RemoteException {
            if (listener == null)
                return;
            synchronized (listeners) {
                listeners.add(listener);
            }
        }

        @Override
        public void removeListener(GPSServiceListener listener) throws RemoteException {
            if (listener == null)
                return;
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        @Override
        public void startTrackingSession(String userId, String aircraftId) throws RemoteException {
            synchronized (lock) {
                createTrackingSession(userId, aircraftId);
            }
        }

        @Override
        public void stopTrackingSession() throws RemoteException {
            synchronized (lock) {
                if (isHRMonitorConnected())
                    disconnectHRMonitor();
                GPSService.this.stop();
            }
        }

        @Override
        public boolean isRunning() throws RemoteException {
            synchronized (lock) {
                return isServiceStarted;
            }
        }

        @Override
        public void showNotification() throws RemoteException {
            synchronized (lock) {
                GPSService.this.showNotification();
            }
        }

        @Override
        public void hideNotification() throws RemoteException {
            synchronized (lock) {
                GPSService.this.hideNotification();
            }
        }

        @Override
        public boolean initBLE() throws RemoteException {
            synchronized (lock) {
                if (hrMonitor == null)
                    hrMonitor = LeHRMonitor.getMonitor(GPSService.this);
                if (hrMonitor.getConnectionStatus() == LeHRMonitor.INITIAL_STATUS)
                    return hrMonitor.initialize();
                if (hrMonitor.getConnectionStatus() == LeHRMonitor.READY_STATUS)
                    return true;

                // reset the monitor
                hrMonitor.close();
                hrMonitor = null;

                return initBLE();
            }
        }

        @Override
        public void connectHRMonitor(String address) throws RemoteException {
            synchronized (lock) {
                hrMonitor.connect(address);
            }
        }

        @Override
        public void disconnectHRMonitor() throws RemoteException {
            synchronized (lock) {
                hrMonitor.disconnect();
            }
        }

        @Override
        public boolean isHRMonitorConnected() throws RemoteException {
            synchronized (lock) {
                return hrMonitor == null ? false : (hrMonitor.getConnectionStatus() == LeHRMonitor.CONNECTED_STATUS);
            }
        }

        @Override
        public String getAirSessionId() throws RemoteException {
            synchronized (lock) {
                return airSession == null ? null : airSession.getObjectId();
            }
        }

        @Override
        public String getAircraftId() throws RemoteException {
            synchronized (lock) {
                return airSession == null ? null : airSession.getAircraftId();
            }
        }

        @Override
        public String getUserId() throws RemoteException {
            synchronized (lock) {
                return airSession == null ? null : airSession.getUserId();
            }
        }

        private void createTrackingSession(final String userId, final String aircraftId) {
            Task.callInBackground(new Callable<AirSession>() {

                @Override
                public AirSession call() throws Exception {
                    AirSession airSession = ParseObject.create(AirSession.class);
                    airSession.setAircraftId(aircraftId);
                    airSession.setUserId(userId);
                    airSession.save();
                    return airSession;
                }
            }).continueWith(new Continuation<AirSession, AirSession>() {

                @Override
                public AirSession then(Task<AirSession> task) throws Exception {
                    AirSession session = null;
                    if (task.isFaulted()) {
                        // error: something went wrong
                        Exception ex = task.getError();
                        Log.e(TAG, "createSession failed", ex);
                    } else if (task.isCompleted()) {
                        // tracking session created
                        session = task.getResult();
                    }
                    synchronized (lock) {
                        onSessionCreated(session);
                    }
                    return session;
                }
            });
        }

        private void onSessionCreated(AirSession session) {
            airSession = session;

            // start monitoring
            mHandler.post(new Runnable() {
                public void run() {
                    GPSService.this.start();
                }
            });

            // notify listeners
            synchronized (listeners) {
                for (GPSServiceListener l: listeners) {
                    try {
                        l.onTrackingSessionStarted(session.getUserId(), session.getAircraftId(), session.getObjectId());
                    } catch (RemoteException ex) {
                        Log.w(TAG, "onSessionCreated() -> failed to notify listener", ex);
                    }
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // init Parse SDK
        ParseObject.registerSubclass(Aircraft.class);
        ParseObject.registerSubclass(AirSession.class);
        Parse.initialize(this, AirApplication.PARSE_APPLICATION_ID, AirApplication.PARSE_CLIENT_KEY);

        // create NotificationManager
        mBuilder = setupNotificationBuilder();
        // create notification
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // create GPS monitor
        gpsMonitor = createGPSMonitor();

        mHandler = new Handler();

        hrMonitor = LeHRMonitor.getMonitor(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");

        return apiEndpoint;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        stop();
        isServiceStarted = false;
        super.onDestroy();
    }


    public void stop() {
        Log.d(TAG, "stop()");

        if (gpsMonitor == null)
            return;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (gpsMonitor.isRunning()) {
                    gpsMonitor.stop();
                    gpsMonitor = null;
                }
            }
        });

        if (hrMonitor.getConnectionStatus() == LeHRMonitor.CONNECTED_STATUS) {
            hrMonitor.disconnect();
        }

        if (workerThread != null && workerThread.isAlive()) {
            workerThread.finishWork();
            mFinishingTask = Task.callInBackground(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    workerThread.join();
                    workerThread = null;
                    mFinishingTask = null;
                    isServiceStarted = false;
                    stopForeground(true);
                    stopSelf();
                    return null;
                }
            });
        }
    }

    public void start() {
        Log.d(TAG, "start()");

        try {
            if (airSession != null) {
                gpsMonitor.start();
            }
        } catch (Exception ex) {
            Log.e(TAG, "failed to start GPS tracking!", ex);
            return;
        }

        workerThread = new DataCollectorThread();
        workerThread.setSession(airSession);
        workerThread.start();

        if (hrMonitor.initialize()) {
            hrMonitor.connect("00:22:D0:00:00:CC");
        }

        isServiceStarted = true;
    }

    public void showNotification() {
        if (mNotificationManager == null || mBuilder == null) {
            // onCreate() wasn't invoked????
            return;
        }

        if (isServiceStarted && mBuilder != null) {
            startForeground(SERVICE_NOTIFICATION_ID, mBuilder.build());
        }
    }

    public void hideNotification() {
        stopForeground(true);
    }

    public boolean isRunning() {
        return gpsMonitor == null ? false : gpsMonitor.isRunning();
    }

    public boolean isGPSEnabled() {
        return gpsMonitor.isGPSEnabled();
    }

    public Location getLastKnownLocation() {
        return gpsMonitor.getLastKnownLocation();
    }

    public Context getContext() {
        return gpsMonitor.getContext();
    }

    public Location getCurrentLocation() {
        return gpsMonitor.getCurrentLocation();
    }

    public long getCurrentLocationTimestamp() {
        return gpsMonitor.getCurrentLocationTimestamp();
    }

    private GPSMonitor createGPSMonitor() {
        final GPSMonitor gpsMonitor = new GPSMonitor(this);
        gpsMonitor.setListener(new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
               if (workerThread != null)
                   workerThread.put(getDataPoint(location));
               synchronized (listeners) {
                   for (GPSServiceListener l: listeners) {
                       try {
                           l.onLocationChanged(location);
                       } catch (Exception ex) {
                           Log.w(TAG, "gpsMonitor.onLocationChanged() -> failed to notify listener", ex);
                       }
                   }
               }
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

    private DataPoint getDataPoint(Location loc) {
        DataPoint dp = new DataPoint();
        dp.setLat(loc.getLatitude());
        dp.setLon(loc.getLongitude());
        dp.setAlt(loc.hasAltitude() ? loc.getAltitude() : null);
        dp.setAcc(loc.hasAccuracy() ? loc.getAccuracy() : null);
        dp.setBea(loc.hasBearing() ? loc.getBearing() : null);
        dp.setVel(loc.hasSpeed() ? loc.getSpeed() : null);
        dp.setT(loc.getTime());
        dp.setHR(hrMonitor.getConnectionStatus() == LeHRMonitor.CONNECTED_STATUS ? hrMonitor.getLastBPM() : null);
        dp.setStress(94);
        return dp;
    }

    private NotificationCompat.Builder setupNotificationBuilder() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("CardioMood Air is running")
                        .setContentText("Click this to open the flight tracker")
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

                try {
                    session.save();
                } catch (ParseException ex) {
                    Log.w(TAG, "workerThread.onStop() -> failed to save session", ex);
                    session.saveEventually();
                }

                // notify listeners
                synchronized (listeners) {
                    for (GPSServiceListener l: listeners) {
                        try {
                            l.onTrackingSessionFinished();
                        } catch (RemoteException ex) {
                            Log.w(TAG, "stopTrackingSession() -> failed to notify listener", ex);
                        }
                    }
                }
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
