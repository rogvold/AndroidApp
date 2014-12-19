package com.cardiomood.android.air.service;

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
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cardiomood.android.air.R;
import com.cardiomood.android.air.TrackingActivity;
import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.CardioItemDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.LocationDAO;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.CardioItemEntity;
import com.cardiomood.android.air.db.entity.LocationEntity;
import com.cardiomood.android.air.gps.GPSMonitor;
import com.cardiomood.android.tools.thread.WorkerThread;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.filter.ArtifactFilter;
import com.cardiomood.math.filter.PisarukArtifactFilter;
import com.cardiomood.math.window.DataWindow;
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import bolts.Task;

/**
 * Created by antondanhsin on 14/11/14.
 */
public class TrackingService extends Service {

    private static final String TAG = TrackingService.class.getSimpleName();

    private static final int SERVICE_NOTIFICATION_ID = 4545;

    /* Commands to the service */
    public static final int MSG_UNREGISTER_CLIENT = 0;
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_GET_STATUS = 2;
    public static final int MSG_INIT_HRM = 3;
    public static final int MSG_CONNECT_HRM = 4;
    public static final int MSG_DISCONNECT_HRM = 5;
    public static final int MSG_RESET_HRM = 6;
    public static final int MSG_START_SESSION = 7;
    public static final int MSG_END_SESSION = 8;

    public static final int MSG_HR_DATA = 10;
    public static final int MSG_CONNECTION_STATUS_CHANGED = 11;
    public static final int MSG_BATTERY_LEVEL = 12;
    public static final int MSG_LOCATION_DATA = 13;
    public static final int MSG_RECONNECTING = 14;

    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_FAIL = 0;

    final Object clientsLock = new Object();

    Handler mHandler;
    LeHRMonitor hrMonitor;
    String hrmDeviceAddress;
    GPSMonitor gpsMonitor;
    List<Messenger> mClients;
    NotificationManager mNotificationManager;
    NotificationCompat.Builder mNotificationBuilder;

    volatile AirSessionEntity mAirSession = null;
    volatile WorkerThread<CardioDataPackage> cardioWorkerThread;
    volatile WorkerThread<Location> gpsWorkerThread;

    Pubnub pubnub = new Pubnub("pub-c-a86ef89b-7858-4b4c-8f89-c4348bfc4b79", "sub-c-e5ae235a-4c3e-11e4-9e3d-02ee2ddab7fe");

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    synchronized (clientsLock) {
                        mClients.add(msg.replyTo);
                    }
                    break;
                case MSG_UNREGISTER_CLIENT:
                    synchronized (clientsLock) {
                        mClients.remove(msg.replyTo);
                    }
                    break;
                case MSG_GET_STATUS:
                    handleStatusRequest(msg.replyTo);
                    break;
                case MSG_INIT_HRM:
                    handleInitHrmRequest(msg.replyTo);
                    break;
                case MSG_CONNECT_HRM:
                    String address = msg.getData().getString("deviceAddress");
                    handleConnectHrmRequest(address, msg.replyTo);
                    break;
                case MSG_DISCONNECT_HRM:
                    handleDisconnectHrmRequest(msg.replyTo);
                    break;
                case MSG_RESET_HRM:
                    handleResetHrmRequest(msg.replyTo);
                    break;
                case MSG_START_SESSION:
                    String parseUserId = msg.getData().getString("parseUserId");
                    String aircraftId = msg.getData().getString("aircraftId");
                    handleStartSessionRequest(parseUserId, aircraftId, msg.replyTo);
                    break;
                case MSG_END_SESSION:
                    handleEndSessionRequest(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void handleStatusRequest(Messenger replyTo) {
        if (replyTo != null) {
            Message response = Message.obtain(
                    null,
                    MSG_GET_STATUS
            );
            if (hrMonitor == null) {
                response.arg1 = LeHRMonitor.INITIAL_STATUS;
            } else {
                response.arg1 = hrMonitor.getConnectionStatus();
                if (hrMonitor.isConnectingOrConnected()) {
                    response.getData().putString("hrmDeviceAddress", hrmDeviceAddress);
                }
            }
            if (mAirSession != null) {
                response.getData().putLong("sessionId", mAirSession.getId());
                response.getData().putLong("startTimestamp", mAirSession.getCreationDate().getTime());
                response.getData().putString("aircraftId", mAirSession.getSyncAircraftId());
                response.getData().putString("userId", mAirSession.getSyncUserId());
            }
            try {
                replyTo.send(response);
            } catch (RemoteException ex) {
                Log.d(TAG, "handleStatusRequest() failed to reply", ex);
            }
        }
    }

    private void handleInitHrmRequest(Messenger replyTo) {
        boolean result = initHrm();
        if (replyTo != null) {
            try {
                replyTo.send(Message.obtain(null, MSG_INIT_HRM, result ? RESULT_SUCCESS : RESULT_FAIL, 0));
            } catch (RemoteException ex) {
                Log.d(TAG, "handleInitHrmRequest() failed", ex);
            }
        }
    }

    private void handleConnectHrmRequest(String address, Messenger replyTo) {
        boolean result = connectHrm(address);
        if (replyTo != null) {
            try {
                replyTo.send(Message.obtain(null, MSG_CONNECT_HRM, result ? RESULT_SUCCESS : RESULT_FAIL, 0));
            } catch (RemoteException ex) {
                Log.d(TAG, "handleConnectHrmRequest() failed", ex);
            }
        }
    }

    private void handleDisconnectHrmRequest(Messenger replyTo) {
        boolean result = disconnectHrm();
        if (replyTo != null) {
            try {
                replyTo.send(Message.obtain(null, MSG_DISCONNECT_HRM, result ? RESULT_SUCCESS : RESULT_FAIL, 0));
            } catch (RemoteException ex) {
                Log.d(TAG, "handleDisconnectHrmRequest() failed", ex);
            }
        }
    }

    private void handleResetHrmRequest(Messenger replyTo) {
        disconnectHrm();
        reloadHrm();
        if (replyTo != null) {
            try {
                replyTo.send(Message.obtain(null, MSG_RESET_HRM, RESULT_SUCCESS));
            } catch (RemoteException ex) {
                Log.d(TAG, "handleResetHrmRequest() failed", ex);
            }
        }
    }

    private void handleStartSessionRequest(String parseUserId, String aircraftId, Messenger replyTo) {
        if (mAirSession != null) {
            // already started!
            return;
        }

        try {
            // create AirSession
            AirSessionDAO sessionDao = HelperFactory.getHelper().getAirSessionDao();
            AirSessionEntity airSession = new AirSessionEntity();
            airSession.setCreationDate(new Date());
            airSession.setSyncDate(new Date());
            airSession.setSyncUserId(parseUserId);
            airSession.setSyncAircraftId(aircraftId);
            sessionDao.create(airSession);
            mAirSession = airSession;

            cardioWorkerThread = new CardioDataCollector(mAirSession);
            cardioWorkerThread.start();

            gpsWorkerThread = new LocationDataCollector(mAirSession);
            gpsWorkerThread.start();

            // start GPS tracking
            gpsMonitor.start();

            // SUCCESS
            if (replyTo != null) {
                Message response = Message.obtain(null, MSG_START_SESSION, RESULT_SUCCESS, 0);
                response.getData().putLong("sessionId", mAirSession.getId());
                response.getData().putLong("startTimestamp", mAirSession.getCreationDate().getTime());
                response.getData().putString("aircraftId", mAirSession.getSyncAircraftId());
                response.getData().putString("userId", mAirSession.getSyncUserId());
                try {
                    replyTo.send(response);
                } catch (RemoteException ex) {
                    Log.d(TAG, "handleStartSessionRequest() failed to reply", ex);
                }
            }
        } catch (SQLException ex) {
            Log.e(TAG, "handleStartSessionRequest() failed to create session", ex);
            if (replyTo != null) {
                Message response = Message.obtain(null, MSG_START_SESSION, RESULT_FAIL, 0);
                response.getData().putString("errorMessage", ex.getMessage());
                try {
                    replyTo.send(response);
                } catch (RemoteException rex) {
                    Log.d(TAG, "handleStartSessionRequest() failed to reply", rex);
                }
            }
        }
    }

    private void handleEndSessionRequest(Messenger replyTo) {
        if (mAirSession == null) {
            // not started
            return;
        }
        // 0. stop monitoring
        if (gpsMonitor != null) {
            gpsMonitor.stop();
        }

        if (hrMonitor != null) {
            if (hrMonitor.isConnectingOrConnected()) {
                disconnectHrm();
                reloadHrm();
            }
        }

        // 1. save session
        try {
            AirSessionDAO sessionDao = HelperFactory.getHelper().getAirSessionDao();
            sessionDao.refresh(mAirSession);
            mAirSession.setEndDate(System.currentTimeMillis());
            mAirSession.setSyncDate(new Date(mAirSession.getEndDate()));
            sessionDao.update(mAirSession);

            // 1.1. notify the client
            if (replyTo != null) {
                Message response = Message.obtain(null, MSG_END_SESSION, RESULT_SUCCESS, 0);
                response.getData().putLong("sessionId", mAirSession.getId());
                response.getData().putLong("startTimestamp", mAirSession.getCreationDate().getTime());
                response.getData().putLong("endTimestamp", mAirSession.getEndDate());
                response.getData().putString("aircraftId", mAirSession.getSyncAircraftId());
                response.getData().putString("userId", mAirSession.getSyncUserId());
                try {
                    replyTo.send(response);
                } catch (RemoteException ex) {
                    Log.d(TAG, "handleEndSessionRequest() failed to reply", ex);
                }
            }
        } catch (SQLException ex) {
            Log.e(TAG, "handleEndSessionRequest() failed to update session", ex);
            if (replyTo != null) {
                Message response = Message.obtain(null, MSG_END_SESSION, RESULT_FAIL, 0);
                response.getData().putString("errorMessage", ex.getMessage());
                try {
                    replyTo.send(response);
                } catch (RemoteException rex) {
                    Log.d(TAG, "handleEndSessionRequest() failed to reply", rex);
                }
            }
        }
        // 2. terminate worker-threads
        if (cardioWorkerThread != null) {
            cardioWorkerThread.finishWork();
        }
        if (gpsWorkerThread != null) {
            gpsWorkerThread.finishWork();
        }
    }

    public void showNotification() {
        if (mNotificationManager == null || mNotificationBuilder == null) {
            // onCreate() wasn't invoked????
            return;
        }

        if (mAirSession != null) {
            startForeground(SERVICE_NOTIFICATION_ID, mNotificationBuilder.build());
        }
    }

    public void hideNotification() {
        stopForeground(true);
    }

    private final LeHRMonitor.Callback hrCallback = new LeHRMonitor.Callback() {
        @Override
        public void onBPMChanged(int bpm) {

        }

        @Override
        public void onConnectionStatusChanged(final int oldStatus, final int newStatus) {
            if (hrMonitor != null && newStatus == LeHRMonitor.CONNECTED_STATUS) {
                hrMonitor.setAutoReconnect(true);
            }

            synchronized (clientsLock) {
                for (Messenger m : mClients) {
                    try {
                        Message msg = Message.obtain(null, MSG_CONNECTION_STATUS_CHANGED, oldStatus, newStatus);
                        msg.getData().putString("deviceAddress", hrmDeviceAddress);
                        m.send(msg);
                    } catch (RemoteException ex) {
                        Log.w(TAG, "onConnectionStatusChanged() failed", ex);
                    }
                }
            }
        }

        @Override
        public void onDataReceived(int bpm, short[] rr) {
            long timestamp = System.currentTimeMillis();
            CardioDataPackage data = new CardioDataPackage(timestamp, bpm, toIntArray(rr));
            if (mAirSession != null && cardioWorkerThread != null) {
                cardioWorkerThread.put(data);
            }
            synchronized (clientsLock) {
                for (Messenger m : mClients) {
                    try {
                        Message msg = Message.obtain(null, MSG_HR_DATA, bpm, 0);
                        msg.getData().setClassLoader(CardioDataPackage.class.getClassLoader());
                        msg.getData().putParcelable("data", data);
                        msg.getData().putString("deviceAddress", hrmDeviceAddress);
                        if (mAirSession != null) {
                            msg.getData().putLong("t", data.getTimestamp() - mAirSession.getCreationDate().getTime());
                        }
                        m.send(msg);
                    } catch (RemoteException ex) {
                        Log.w(TAG, "onDataReceived() failed", ex);
                    }
                }
            }
        }

        @Override
        public void onBatteryLevelReceived(int level) {
            synchronized (clientsLock) {
                for (Messenger m : mClients) {
                    try {
                        Message msg = Message.obtain(null, MSG_BATTERY_LEVEL, level, 0);
                        msg.getData().putString("deviceAddress", hrmDeviceAddress);
                        m.send(msg);
                    } catch (RemoteException ex) {
                        Log.w(TAG, "onBatteryLevelReceived() failed", ex);
                    }
                }
            }
        }

        @Override
        public void onReconnecting(int attemptNumber, int maxAttempts) {
            synchronized (clientsLock) {
                for (Messenger m: mClients) {
                    try {
                        Message msg = Message.obtain(null, MSG_RECONNECTING, attemptNumber, maxAttempts);
                        msg.getData().putString("deviceAddress", hrmDeviceAddress);
                        m.send(msg);
                    } catch (RemoteException ex) {
                        Log.w(TAG, "notifyReconnecting() failed", ex);
                    }
                }
            }
        }

        private int[] toIntArray(short[] data) {
            final int[] result = new int[data.length];
            for (int i=0; i<data.length; i++) {
                result[i] = data[i];
            }
            return result;
        }
    };

    private final LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (gpsWorkerThread != null) {
                gpsWorkerThread.put(location);
            }

            // notify all connected clients
            synchronized (clientsLock) {
                Iterator<Messenger> it = mClients.iterator();
                while (it.hasNext()) {
                    Messenger client = it.next();
                    Message msg = Message.obtain(null, MSG_LOCATION_DATA);
                    msg.getData().putParcelable("location", location);
                    try {
                        client.send(msg);
                    } catch (RemoteException ex) {
                        Log.w(TAG, "gpsListener.onLocationChanged() failed to notify client", ex);
                        if (ex instanceof DeadObjectException) {
                            it.remove();
                        }
                    }
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO: we must handle this
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO: we must handle this
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO: we must handle this
        }
    };

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        hrMonitor = null;
        mClients = new ArrayList<Messenger>();

        // create NotificationManager
        mNotificationBuilder = setupNotificationBuilder();
        // create notification
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        gpsMonitor = new GPSMonitor(this);
        gpsMonitor.setListener(gpsListener);

        HelperFactory.setHelper(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        try {
            if (gpsMonitor != null && gpsMonitor.isRunning()) {
                gpsMonitor.stop();
            }
            if (hrMonitor != null) {
                if (hrMonitor.isConnectingOrConnected()) {
                    hrMonitor.disconnect();
                }
                hrMonitor.close();
            }

        } catch (Exception ex) {
            Log.w(TAG, "onDestroy(): failed to close hrMonitor", ex);
        }

        HelperFactory.releaseHelper();
        super.onDestroy();
    }

    private NotificationCompat.Builder setupNotificationBuilder() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("CardioMood Air is running")
                        .setContentText("Click this to open the flight tracker")
                        .setOngoing(true);

        Intent resultIntent = new Intent(this, TrackingActivity.class);

        PendingIntent resultPendingIntent = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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

    private void reloadHrm() {
        if (hrMonitor != null) {
            hrMonitor.setAutoReconnect(false);
            if (hrMonitor.isConnectingOrConnected())
                hrMonitor.disconnect();
            hrMonitor.setCallback(null);
            hrMonitor.close();
            hrMonitor = null;
        }
        hrmDeviceAddress = null;

        hrMonitor = LeHRMonitor.getMonitor(this);
        hrMonitor.enableBroadcasts(false);

        if (hrMonitor.getConnectionStatus() != LeHRMonitor.INITIAL_STATUS) {
            throw new IllegalStateException("Incorrect monitor status!");
        }
        hrMonitor.setCallback(hrCallback);
    }

    private boolean initHrm() {
        if (hrMonitor != null && hrMonitor.getConnectionStatus() == LeHRMonitor.INITIAL_STATUS) {
            return hrMonitor.initialize();
        }
        reloadHrm();
        return hrMonitor.initialize();
    }

    private boolean connectHrm(String address) {
        if (hrMonitor == null) {
            throw new IllegalStateException("Not initialized!");
        }

        if (hrMonitor.getConnectionStatus() == LeHRMonitor.READY_STATUS) {
            boolean result = hrMonitor.connect(address);
            if (result) {
                hrmDeviceAddress = address;
            } else {
                hrmDeviceAddress = null;
            }
            return result;
        } else {
            throw new IllegalStateException("Incorrect monitor state: " + hrMonitor.getConnectionStatus());
        }
    }

    private boolean disconnectHrm() {
        try {
            hrmDeviceAddress = null;
            if (hrMonitor != null) {
                hrMonitor.setAutoReconnect(false);
                if (hrMonitor.isConnectingOrConnected()) {
                    hrMonitor.disconnect();
                }
                hrMonitor.close();
                hrMonitor.setCallback(null);
            }
            hrMonitor = null;
            initHrm();
            return true;
        } catch (Exception ex) {
            Log.w(TAG, "disconnect() failed", ex);
        }
        return false;
    }


    private class PubnubWorkerThread extends WorkerThread<JSONObject> {

        private String channelName;
        private Queue<JSONObject> buffer = new LinkedList<JSONObject>();

        private PubnubWorkerThread(String channelName) {
            this.channelName = channelName;
        }

        @Override
        public void onStart() {
            super.onStart();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        private Task publish(JSONObject item) {
            String msg = item.toString();
            Log.d(TAG, "Pubnub.publish() -> " + msg);
            final Task.TaskCompletionSource task = Task.create();
            pubnub.publish(channelName, item, false, new Callback() {
                @Override
                public void successCallback(String s, Object o) {
                    task.setResult(null);
                    Log.d(TAG, "Pubnub.publish() -> published");
                }

                @Override
                public void errorCallback(String s, PubnubError pubnubError) {
                    Log.w(TAG, "Pubnub.publish() -> failed");
                    if (pubnubError != null) {
                        task.setError(new RuntimeException(pubnubError.toString()));
                    } else {
                        task.setError(new RuntimeException("Unknown error"));
                    }
                }
            });

            return task.getTask();
        }

        @Override
        public void processItem(JSONObject item) {
            buffer.add(item);
            while (!buffer.isEmpty()) {
                Task task = publish(buffer.peek());
                try {
                    task.waitForCompletion();
                    if (task.isFaulted()) {
                        Log.w(TAG, "failed to publish message", task.getError());
                        break;
                    } else if (task.isCompleted()) {
                        buffer.remove();
                    }
                } catch (InterruptedException ex) {
                    interrupt();
                    return;
                }
            }
        }
    }

    private class CardioDataCollector extends WorkerThread<CardioDataPackage> {

        private AirSessionEntity airSession;
        private AirSessionDAO sessionDAO;
        private CardioItemDAO itemDao;
        private long T = -1;

        private DataWindow.Timed stressDataWindow = new DataWindow.Timed(120 * 1000, 5000);
        private DataWindow.IntervalsCount sdnnDataWindow = new DataWindow.IntervalsCount(20, 5);
        private final ArtifactFilter filter = new PisarukArtifactFilter();

        private Double currentSDNN = null;
        private Integer currentStress = null;

        private WorkerThread<JSONObject> pubnubWorkerThread = new PubnubWorkerThread("HeartRate");

        private CardioDataCollector(AirSessionEntity airSession) throws SQLException {
            this.airSession = airSession;
            this.sessionDAO = HelperFactory.getHelper().getAirSessionDao();
            this.itemDao = HelperFactory.getHelper().getCardioItemDao();

            sdnnDataWindow.setCallback(new DataWindow.CallbackAdapter<DataWindow.IntervalsCount>() {

                @Override
                public void onStep(DataWindow.IntervalsCount window, int index, double t, double value) {
                    double[] elements = window.getIntervals().getElements();
                    elements = filter.doFilter(elements);
                    currentSDNN = HeartRateUtils.getSDNN(elements);
                }
            });

            stressDataWindow.setCallback(new DataWindow.CallbackAdapter<DataWindow.Timed>() {

                @Override
                public void onStep(DataWindow.Timed window, int index, double t, double value) {
                    double[] elements = window.getIntervals().getElements();
                    elements = filter.doFilter(elements);
                    currentStress = (int) HeartRateUtils.getSI(elements);
                }
            });
        }

        @Override
        public void onStop() {
            super.onStop();
            pubnubWorkerThread.finishWork();
            T = -1;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    hideNotification();
                    mAirSession = null;
                    disconnectHrm();
                }
            });
            currentSDNN = null;
            currentStress = null;
        }

        @Override
        public void onStart() {
            super.onStart();
            stressDataWindow.clear();
            sdnnDataWindow.clear();
            currentSDNN = null;
            currentStress = null;
            pubnubWorkerThread.start();
            if (airSession != null) {
                // this should always be TRUE
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showNotification();
                    }
                });
            }
        }

        private void publishItem(CardioDataPackage item) {
            try {
                JSONObject message = new JSONObject();
                message.put("HR", item.getBpm());
                message.put("t", item.getTimestamp());
                message.put("aircraftId", airSession.getSyncAircraftId());
                message.put("userId", airSession.getSyncUserId());
                message.put("s", airSession.getCreationDate().getTime());
                message.put("localId", airSession.getId());
                if (currentStress != null) {
                    message.put("stress", currentStress);
                }
                if (currentSDNN != null) {
                    message.put("SDNN", currentSDNN);
                }
                pubnubWorkerThread.put(message);
            } catch (JSONException ex) {
                Log.e(TAG, "publishItem() failed with exception", ex);
            }
        }

        @Override
        public void processItem(CardioDataPackage item) {
            if (airSession == null) {
                return;
            }
            publishItem(item);
            if (item.getRr().length == 0) {
                // ignore package without interval
                return;
            }
            try {
                if (T < 0) {
                    // save start timestamp of the session
                    T = item.getTimestamp();
                }

                // t = milliseconds from the beginning
                long t = item.getTimestamp() - T;
                for (int rr : item.getRr()) {
                    stressDataWindow.add(rr);
                    sdnnDataWindow.add(rr);
                    CardioItemEntity entity = new CardioItemEntity();
                    entity.setSession(airSession);
                    entity.setT(t);
                    entity.setRr(rr);
                    entity.setBpm(item.getBpm());
                    t += rr;

                    itemDao.create(entity);
                }

                sessionDAO.refresh(airSession);
                airSession.setSyncDate(new Date());
                sessionDAO.update(airSession);
            } catch (SQLException ex) {
                Log.w(TAG, "workerThread.processItem() failed", ex);
            }
        }
    }

    private class LocationDataCollector extends WorkerThread<Location> {
        private AirSessionEntity airSession;
        private AirSessionDAO sessionDAO;
        private LocationDAO itemDao;
        private long T = -1;

        private WorkerThread<JSONObject> pubnubWorkerThread = new PubnubWorkerThread("GPS");

        private LocationDataCollector(AirSessionEntity airSession) throws SQLException {
            this.airSession = airSession;
            this.sessionDAO = HelperFactory.getHelper().getAirSessionDao();
            this.itemDao = HelperFactory.getHelper().getLocationDao();
        }

        @Override
        public void onStop() {
            super.onStop();
            pubnubWorkerThread.finishWork();
            T = -1;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    hideNotification();
                    mAirSession = null;
                }
            });
        }

        @Override
        public void onStart() {
            super.onStart();
            pubnubWorkerThread.start();
            if (airSession != null) {
                // this should always be TRUE
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showNotification();
                    }
                });
            }
        }

        private void publishItem(Location item) {
            try {
                JSONObject message = new JSONObject();
                message.put("lat", item.getLatitude());
                message.put("lon", item.getLongitude());
                if (item.hasAccuracy()) {
                    message.put("acc", item.getAccuracy());
                }
                if (item.hasAltitude()) {
                    message.put("alt", item.getAltitude());
                }
                if (item.hasSpeed()) {
                    message.put("vel", item.getSpeed());
                }
                if (item.hasBearing()) {
                    message.put("bea", item.getBearing());
                }
                message.put("t", item.getTime());
                message.put("aircraftId", airSession.getSyncAircraftId());
                message.put("userId", airSession.getSyncUserId());
                message.put("s", airSession.getCreationDate().getTime());
                message.put("localId", airSession.getId());
                pubnubWorkerThread.put(message);
            } catch (JSONException ex) {
                Log.e(TAG, "publishItem() failed with exception", ex);
            }
        }

        @Override
        public void processItem(Location item) {
            if (airSession == null) {
                return;
            }
            publishItem(item);
            try {
                if (T < 0) {
                    // save start timestamp of the session
                    T = item.getTime();
                }

                // t = milliseconds from the beginning
                long t = item.getTime() - T;
                LocationEntity entity = new LocationEntity();
                entity.setSession(airSession);
                entity.setT(t);
                entity.setLatitude(item.getLatitude());
                entity.setLongitude(item.getLongitude());
                if (item.hasAccuracy())
                    entity.setAccuracy(item.getAccuracy());
                if (item.hasAltitude())
                    entity.setAltitude(item.getAltitude());
                if (item.hasSpeed())
                    entity.setVelocity(item.getSpeed());
                if (item.hasBearing())
                    entity.setBearing(item.getBearing());

                itemDao.create(entity);

                sessionDAO.refresh(airSession);
                airSession.setSyncDate(new Date());
                sessionDAO.update(airSession);
            } catch (SQLException ex) {
                Log.w(TAG, "workerThread.processItem() failed", ex);
            }
        }
    }

}
