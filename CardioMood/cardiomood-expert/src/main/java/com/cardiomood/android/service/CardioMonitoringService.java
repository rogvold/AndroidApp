package com.cardiomood.android.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cardiomood.android.MainActivity;
import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.db.entity.CardioItemDAO;
import com.cardiomood.android.db.entity.CardioItemEntity;
import com.cardiomood.android.db.entity.SessionDAO;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.expert.R;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.tools.thread.WorkerThread;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.filter.ArtifactFilter;
import com.cardiomood.math.filter.PisarukArtifactFilter;
import com.cardiomood.math.window.DataWindow;
import com.j256.ormlite.misc.TransactionManager;
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;

import bolts.Task;

public class CardioMonitoringService extends Service {

    private static final String TAG = CardioMonitoringService.class.getSimpleName();

    private static final int SERVICE_NOTIFICATION_ID = 4343;
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    /* Commands to the service */
    public static final int MSG_UNREGISTER_CLIENT = 0;
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_GET_STATUS = 2;
    public static final int MSG_INIT = 3;
    public static final int MSG_CONNECT = 4;
    public static final int MSG_DISCONNECT = 5;
    public static final int MSG_RESET = 6;
    public static final int MSG_START_SESSION = 7;
    public static final int MSG_END_SESSION = 8;
    public static final int MSG_UPDATE_INFO = 9;

    public static final int MSG_HR_DATA = 10;
    public static final int MSG_CONNECTION_STATUS_CHANGED = 11;
    public static final int MSG_BATTERY_LEVEL = 12;
    public static final int MSG_RECONNECTING = 13;
    public static final int MSG_PROGRESS = 14;


    /* Response messages */
    public static final int RESP_STATUS = 1001;
    public static final int RESP_INIT_RESULT = 1002;
    public static final int RESP_CONNECT_RESULT = 1003;
    public static final int RESP_DISCONNECT_RESULT = 1004;
    public static final int RESP_START_SESSION_RESULT = 1005;
    public static final int RESP_END_SESSION_RESULT = 1006;

    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_FAIL = 0;

    Handler mHandler;
    LeHRMonitor hrMonitor;
    String mDeviceAddress;
    List<Messenger> mClients;
    NotificationManager mNotificationManager;
    NotificationCompat.Builder mNotificationBuilder;

    volatile SessionEntity mCardioSession = null;
    WorkerThread<CardioDataPackage> mWorkerThread;

    private Object sessionLock = new Object();
    private PreferenceHelper prefHelper;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_GET_STATUS:
                    handleConnectionStatusRequest(msg);
                    break;
                case MSG_INIT:
                    handleInitRequest(msg);
                    break;
                case MSG_CONNECT:
                    handleConnectRequest(msg);
                    break;
                case MSG_DISCONNECT:
                    handleDisconnectRequest(msg);
                    break;
                case MSG_RESET:
                    handleResetRequest(msg);
                    break;
                case MSG_START_SESSION:
                    handleStartSessionRequest(msg);
                    break;
                case MSG_END_SESSION:
                    handleEndSessionRequest(msg);
                    break;
                case MSG_UPDATE_INFO:
                    handleUpdateInfoRequest(msg);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    final LeHRMonitor.Callback hrCallback = new LeHRMonitor.Callback() {
        @Override
        public void onBPMChanged(int bpm) {

        }

        @Override
        public void onConnectionStatusChanged(final int oldStatus, final int newStatus) {
            if (hrMonitor != null && newStatus == LeHRMonitor.CONNECTED_STATUS) {
                hrMonitor.setAutoReconnect(prefHelper.getBoolean(ConfigurationConstants.CONNECTION_AUTO_RECONNECT, false, true));
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (hrMonitor != null && hrMonitor.isConnected()) {
                            hrMonitor.requestBatteryLevel();
                        }
                    }
                }, 500);
            }

            if (newStatus != LeHRMonitor.CONNECTED_STATUS) {
                if (mCardioSession != null && mWorkerThread != null) {
                    handleEndSessionRequest(Message.obtain(null, MSG_END_SESSION));
                }
            }

            Iterator<Messenger> it = mClients.iterator();
            while(it.hasNext()) {
                Messenger m = it.next();
                try {
                    Message msg = Message.obtain(null, MSG_CONNECTION_STATUS_CHANGED, oldStatus, newStatus);
                    msg.getData().putString("deviceAddress", mDeviceAddress);
                    m.send(msg);
                } catch (RemoteException ex) {
                    Log.w(TAG, "onConnectionStatusChanged() failed", ex);
                    if (ex instanceof DeadObjectException) {
                        it.remove();
                    }
                }
            }
        }

        @Override
        public void onDataReceived(int bpm, short[] rr) {
            long timestamp = System.currentTimeMillis();
            CardioDataPackage data = new CardioDataPackage(timestamp, bpm, toIntArray(rr));
            if (mCardioSession != null && mWorkerThread != null) {
                mWorkerThread.put(data);
            }
            Iterator<Messenger> it = mClients.iterator();
            while(it.hasNext()) {
                Messenger m = it.next();
                try {
                    Message msg = Message.obtain(null, MSG_HR_DATA, bpm, 0);
                    msg.getData().setClassLoader(CardioDataPackage.class.getClassLoader());
                    msg.getData().putParcelable("data", data);
                    msg.getData().putString("deviceAddress", mDeviceAddress);
                    if (mCardioSession != null) {
                        msg.getData().putLong("t", data.getTimestamp() - mCardioSession.getCreationDate().getTime());
                    }
                    m.send(msg);
                } catch (RemoteException ex) {
                    Log.w(TAG, "onDataReceived() failed", ex);
                    if (ex instanceof DeadObjectException) {
                        it.remove();
                    }
                }
            }
        }

        @Override
        public void onBatteryLevelReceived(int level) {
            for (Messenger m: mClients) {
                try {
                    Message msg = Message.obtain(null, MSG_BATTERY_LEVEL, level, 0);
                    msg.getData().putString("deviceAddress", mDeviceAddress);
                    m.send(msg);
                } catch (RemoteException ex) {
                    Log.w(TAG, "onBatteryLevelReceived() failed", ex);
                }
            }
        }

        @Override
        public void onReconnecting(int attemptNumber, int maxAttempts) {
            for (Messenger m: mClients) {
                try {
                    Message msg = Message.obtain(null, MSG_RECONNECTING, attemptNumber, maxAttempts);
                    msg.getData().putString("deviceAddress", mDeviceAddress);
                    m.send(msg);
                } catch (RemoteException ex) {
                    Log.w(TAG, "notifyReconnecting() failed", ex);
                }
            }
        }
    };

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        hrMonitor = null;
        mClients = new ArrayList<>();
        prefHelper = new PreferenceHelper(this, true);

        // create NotificationManager
        mNotificationBuilder = setupNotificationBuilder();
        // create notification
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        DatabaseHelperFactory.initialize(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        try {
            if (hrMonitor != null) {
                hrMonitor.setAutoReconnect(false);
                if (hrMonitor.isConnectingOrConnected()) {
                    hrMonitor.disconnect();
                }
                hrMonitor.close();
            }
        } catch (Exception ex) {
            Log.w(TAG, "onDestroy(): failed to close hrMonitor", ex);
        }

        DatabaseHelperFactory.releaseHelper();
        super.onDestroy();
    }

    private NotificationCompat.Builder setupNotificationBuilder() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("CardioMood Tracking is running")
                        .setContentText("Click this to open the app")
                        .setOngoing(true);

        // setup notification intents
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = null;
        if (Build.VERSION.SDK_INT >= 16) {
            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
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

    private void reloadMonitor() {
        if (hrMonitor != null) {
            hrMonitor.setAutoReconnect(false);
            if (hrMonitor.isConnectingOrConnected())
                hrMonitor.disconnect();
            hrMonitor.setCallback(null);
            hrMonitor.close();
            hrMonitor = null;
        }
        mDeviceAddress = null;

        hrMonitor = LeHRMonitor.getMonitor(this);
        hrMonitor.enableBroadcasts(false);

        if (hrMonitor.getConnectionStatus() != LeHRMonitor.INITIAL_STATUS) {
            throw new IllegalStateException("Incorrect monitor status!");
        }
        hrMonitor.setCallback(hrCallback);
    }

    private boolean init() {
        if (hrMonitor != null && hrMonitor.getConnectionStatus() == LeHRMonitor.INITIAL_STATUS) {
            return hrMonitor.initialize();
        }
        reloadMonitor();
        return hrMonitor.initialize();
    }

    private int[] toIntArray(short[] data) {
        final int[] result = new int[data.length];
        for (int i=0; i<data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }

    private boolean connect(String address) {
        if (hrMonitor == null) {
            throw new IllegalStateException("Not initialized!");
        }

        if (hrMonitor.getConnectionStatus() == LeHRMonitor.READY_STATUS) {
            boolean result = hrMonitor.connect(address);
            if (result) {
                mDeviceAddress = address;
            } else {
                mDeviceAddress = null;
            }
            return result;
        } else {
            throw new IllegalStateException("Incorrect monitor state: " + hrMonitor.getConnectionStatus());
        }
    }

    private void disconnect() {
        try {
            mDeviceAddress = null;
            if (hrMonitor != null) {
                hrMonitor.setAutoReconnect(false);
                if (hrMonitor.isConnectingOrConnected()) {
                    hrMonitor.disconnect();
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        hrMonitor.setCallback(null);
                        hrMonitor.close();
                        hrMonitor = null;
                    }
                });
            }
        } catch (Exception ex) {
            Log.w(TAG, "disconnect() failed", ex);
        }
    }

    private void handleConnectionStatusRequest(Message msg) {
        try {
            if (msg.replyTo != null) {
                Message response = Message.obtain(null, RESP_STATUS,
                        hrMonitor == null ? LeHRMonitor.INITIAL_STATUS : hrMonitor.getConnectionStatus(), 0);
                response.getData().putString("deviceAddress", mDeviceAddress);
                if (mCardioSession != null) {
                    response.getData().putLong("sessionId", mCardioSession.getId());
                }
                msg.replyTo.send(response);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "handleConnectionStatusRequest() failed", ex);
        }
    }

    private void handleInitRequest(Message msg) {
        try {
            boolean result = init();
            if (msg.replyTo != null) {
                Message response = Message.obtain(null, RESP_INIT_RESULT, result ? RESULT_SUCCESS : RESULT_FAIL, 0);
                msg.replyTo.send(response);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "handleInitRequest() failed", ex);
        }
    }

    private void handleConnectRequest(Message msg) {
        try {
            String address = msg.getData().getString("deviceAddress");
            boolean result = connect(address);
            if (msg.replyTo != null) {
                Message response = Message.obtain(null, RESP_CONNECT_RESULT, result ? RESULT_SUCCESS : RESULT_FAIL, 0);
                msg.replyTo.send(response);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "handleConnectRequest() failed", ex);
        }
    }

    private void handleDisconnectRequest(Message msg) {
        try {
            if (mCardioSession != null) {
                Message endSessionMsg = Message.obtain(null, MSG_END_SESSION);
                endSessionMsg.replyTo = msg.replyTo;
                handleEndSessionRequest(msg);
                mCardioSession = null;
                // disconnect will be called when the worker thread is finished
                return;
            }
            disconnect();
            if (msg.replyTo != null) {
                Message response = Message.obtain(null, RESP_DISCONNECT_RESULT, RESULT_SUCCESS, 0);
                msg.replyTo.send(response);
            }
            init();
        } catch (RemoteException ex) {
            Log.d(TAG, "handleDisconnectRequest() failed", ex);
        }
    }

    private void handleResetRequest(Message msg) {
        if (mCardioSession != null) {
            Message endSessionMsg = Message.obtain(null, MSG_END_SESSION);
            endSessionMsg.replyTo = msg.replyTo;
            handleEndSessionRequest(msg);
            mCardioSession = null;
            // disconnect will be called when the worker thread is finished
            return;
        }
    }

    private void handleStartSessionRequest(Message msg) {
        if (mCardioSession != null) {
            // already started!
            return;
        }
        try {
            try {
                final String parseUserId = msg.getData().getString("parseUserId");
                TransactionManager.callInTransaction(DatabaseHelperFactory.getHelper().getConnectionSource(), new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        SessionDAO sessionDao = DatabaseHelperFactory.getHelper().getSessionDao();
                        SessionEntity cardioSession = new SessionEntity();
                        cardioSession.setStartTimestamp(System.currentTimeMillis());
                        cardioSession.setCreationDate(new Date());
                        cardioSession.setSyncDate(new Date());
                        cardioSession.setSyncUserId(parseUserId);
                        sessionDao.create(cardioSession);
                        cardioSession.setName(getNewSessionName(cardioSession));
                        cardioSession.setDescription(getNewSessionDescription(cardioSession));
                        sessionDao.update(cardioSession);
                        mCardioSession = cardioSession;
                        return null;
                    }
                });

                int limitType = msg.getData().getInt("limitType", 0);
                int durationLimit = msg.getData().getInt("durationLimit", 120);
                int countLimit = msg.getData().getInt("countLimit", 100);
                // start worker thread
                mWorkerThread = new CardioDataCollector(mCardioSession, limitType, durationLimit, countLimit);
                mWorkerThread.start();

                // SUCCESS
                if (msg.replyTo != null) {
                    Message response = Message.obtain(null, RESP_START_SESSION_RESULT, RESULT_SUCCESS, 0);
                    response.getData().putLong("sessionId", mCardioSession.getId());
                    response.getData().putLong("startTimestamp", mCardioSession.getStartTimestamp());
                    msg.replyTo.send(response);
                }
            } catch (SQLException ex) {
                if (msg.replyTo != null) {
                    Message response = Message.obtain(null, RESP_START_SESSION_RESULT, RESULT_FAIL, 0);
                    response.getData().putString("errorMessage", ex.getMessage());
                    msg.replyTo.send(response);
                }
            }

        } catch (RemoteException ex) {
            Log.d(TAG, "handleStartSessionRequest() failed", ex);
        }
    }

    private void handleEndSessionRequest(Message msg) {
        try {
            try {
                if (mCardioSession != null) {
                    synchronized (sessionLock) {
                        TransactionManager.callInTransaction(DatabaseHelperFactory.getHelper().getConnectionSource(), new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                SessionDAO sessionDao = DatabaseHelperFactory.getHelper().getSessionDao();
                                sessionDao.refresh(mCardioSession);
                                mCardioSession.setEndTimestamp(System.currentTimeMillis());
                                mCardioSession.setSyncDate(new Date(mCardioSession.getEndTimestamp()));
                                sessionDao.update(mCardioSession);
                                return null;
                            }
                        });
                    }
                    // stop worker thread
                    if (mWorkerThread != null) {
                        mWorkerThread.finishWork();
                    }
                }

                // SUCCESS
                if (msg.replyTo != null) {
                    Message response = Message.obtain(null, RESP_END_SESSION_RESULT, RESULT_SUCCESS, 0);
                    response.getData().putLong("sessionId", mCardioSession.getId());
                    response.getData().putLong("endTimestamp", mCardioSession.getEndTimestamp());
                    msg.replyTo.send(response);
                } else {
                    // TODO: make notification
                    for (Messenger m: mClients) {
                        Message response = Message.obtain(null, RESP_END_SESSION_RESULT, RESULT_SUCCESS, 0);
                        response.getData().putLong("sessionId", mCardioSession.getId());
                        response.getData().putLong("endTimestamp", mCardioSession.getEndTimestamp());
                        m.send(response);
                    }
                }

                synchronized (sessionLock) {
                    mCardioSession = null;
                }
            } catch (SQLException ex) {
                if (msg.replyTo != null) {
                    Message response = Message.obtain(null, RESP_END_SESSION_RESULT, RESULT_FAIL, 0);
                    response.getData().putString("errorMessage", ex.getMessage());
                    msg.replyTo.send(response);
                }
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "handleStartSessionRequest() failed", ex);
        }
    }

    private void handleUpdateInfoRequest(Message msg) {
        String name = msg.getData().getString("name");
        String description = msg.getData().getString("description");
        try {
            if (mCardioSession != null) {
                try {
                    synchronized (sessionLock) {
                        SessionDAO sessionDAO = DatabaseHelperFactory.getHelper().getSessionDao();
                        sessionDAO.refresh(mCardioSession);
                        mCardioSession.setSyncDate(new Date());
                        mCardioSession.setName(name);
                        mCardioSession.setDescription(description);
                        sessionDAO.update(mCardioSession);
                    }
                } catch (SQLException ex) {
                    if (msg.replyTo != null) {
                        Message response = Message.obtain(null, MSG_UPDATE_INFO, RESULT_FAIL, 0);
                        response.getData().putString("errorMessage", ex.getMessage());
                        msg.replyTo.send(response);
                    }
                }
            } else {
                prefHelper.putString(ConfigurationConstants.MEASUREMENT_NAME, name);
                prefHelper.putString(ConfigurationConstants.MEASUREMENT_DESCRIPTION, description);
            }
            if (msg.replyTo != null) {
                Message response = Message.obtain(null, MSG_UPDATE_INFO, RESULT_SUCCESS, 0);
                msg.replyTo.send(response);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "handleUpdateInfoRequest() failed", ex);
        }
    }

    public void showNotification() {
        if (mNotificationManager == null || mNotificationBuilder == null) {
            // onCreate() wasn't invoked????
            return;
        }

        if (mCardioSession != null) {
            startForeground(SERVICE_NOTIFICATION_ID, mNotificationBuilder.build());
        }
    }

    public void hideNotification() {
        stopForeground(true);
    }


    private String getNewSessionName(SessionEntity session) {
        String pattern = prefHelper.getString(ConfigurationConstants.MEASUREMENT_NAME, "Measurement $date$");
        return processMacros(pattern, session);
    }

    private String getNewSessionDescription(SessionEntity session) {
        String pattern = prefHelper.getString(ConfigurationConstants.MEASUREMENT_DESCRIPTION);
        prefHelper.remove(ConfigurationConstants.MEASUREMENT_DESCRIPTION);
        return processMacros(pattern, session);
    }

    private String processMacros(String pattern, SessionEntity session) {
        if (pattern == null)
            return null;
        return pattern.replaceAll("\\$date\\$", DATE_FORMAT.format(session.getCreationDate()))
                .replaceAll("\\$id\\$", String.valueOf(session.getId()))
                .replaceAll("\\$device_name\\$", getConnectedDeviceName())
                .replaceAll("\\$device_address\\$", getConnectedDeviceAddress());

    }

    private String getConnectedDeviceAddress() {
        if (hrMonitor != null && hrMonitor.isConnectingOrConnected()) {
            return hrMonitor.getDeviceAddress();
        } else return "";
    }

    private String getConnectedDeviceName() {
        if (hrMonitor != null && hrMonitor.isConnectingOrConnected()) {
            String name = hrMonitor.getDeviceName();
            if (name == null)
                return "";
            return name;
        } else return "";
    }


    private class PubnubWorkerThread extends WorkerThread<JSONObject> {

        private String channelName = null;
        private Queue<JSONObject> buffer = new LinkedList<JSONObject>();
        private boolean disableRealTime = true;
        private Pubnub pubnub;

        @Override
        public void onStart() {
            super.onStart();
            disableRealTime = prefHelper.getBoolean(ConfigurationConstants.SYNC_DISABLE_REAL_TIME, true, true);
            String pubKey = prefHelper.getString(ConfigurationConstants.CONFIG_PUBNUB_PUB_KEY);
            String subKey = prefHelper.getString(ConfigurationConstants.CONFIG_PUBNUB_SUB_KEY);
            pubnub = new Pubnub(pubKey, subKey);
            channelName = prefHelper.getString(ConfigurationConstants.CONFIG_PUBNUB_CHANNEL);
        }

        private Task publish(JSONObject item) {
            String msg = item.toString();
            Log.d(TAG, "Pubnub.publish() -> " + msg);
            final Task.TaskCompletionSource task = Task.create();
            if (disableRealTime) {
                task.setResult(null);
            } else {
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
            }
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

        private static final int LIMIT_TYPE_NONE = 0;
        private static final int LIMIT_TYPE_TIME = 1;
        private static final int LIMIT_TYPE_COUNT = 2;
        private static final int LIMIT_TYPE_CUSTOM = 3;

        private final SessionEntity cardioSession;
        private final SessionDAO sessionDAO;
        private final CardioItemDAO itemDao;

        private final DataWindow.Timed stressDataWindow = new DataWindow.Timed(120 * 1000, 5000);
        private final DataWindow.IntervalsCount sdnnDataWindow = new DataWindow.IntervalsCount(20, 5);
        private final ArtifactFilter filter = new PisarukArtifactFilter();

        private final int limitType;
        private final int durationLimit;
        private final int countLimit;

        private volatile long T0 = -1, T = -1;
        private volatile int duration = 0;
        private volatile int count = 0;
        private volatile Double currentSDNN = null;
        private volatile Integer currentStress = null;

        private volatile WorkerThread<JSONObject> pubnubWorkerThread = new PubnubWorkerThread();

        private CardioDataCollector(SessionEntity cardioSession, int limitType, int durationLimit, int countLimit) throws SQLException {
            this.cardioSession = cardioSession;
            this.limitType = limitType;
            this.durationLimit = durationLimit*1000;
            this.countLimit = countLimit;

            this.sessionDAO = DatabaseHelperFactory.getHelper().getSessionDao();
            this.itemDao = DatabaseHelperFactory.getHelper().getCardioItemDao();

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
            T0 = -1;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                    hideNotification();
                }
            });
            synchronized (sessionLock) {
                try {
                    sessionDAO.refresh(cardioSession);
                    if (cardioSession.getEndTimestamp() == null) {
                        cardioSession.setEndTimestamp(System.currentTimeMillis());
                        cardioSession.setSyncDate(new Date());
                        sessionDAO.update(cardioSession);
                    }
                } catch (SQLException ex) {
                    // suppress this
                }
            }
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
            if (cardioSession != null) {
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
                message.put("userId", cardioSession.getSyncUserId());
                message.put("s", cardioSession.getStartTimestamp());
                message.put("localId", cardioSession.getId());
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
            if (cardioSession == null) {
                return;
            }
            publishItem(item);
            if (item.getRr().length == 0) {
                // ignore package without interval
                return;
            }
            try {
                if (T0 < 0) {
                    // save start of session
                    T0 = item.getTimestamp();
                }

                // t = milliseconds from the beginning
                long t = item.getTimestamp() - T0;
                T = (t > T) ? t : T + 1;
                for (int rr : item.getRr()) {
                    stressDataWindow.add(rr);
                    sdnnDataWindow.add(rr);
                    CardioItemEntity entity = new CardioItemEntity();
                    entity.setSession(cardioSession);
                    entity.setT(T);
                    entity.setRr(rr);
                    entity.setBpm(item.getBpm());
                    T += rr;

                    itemDao.create(entity);

                    duration += rr;
                    count++;
                    publishProgress();
                    if (limitReached()) {
                        finishWork();
                        break;
                    }
                }

                synchronized (sessionLock) {
                    sessionDAO.refresh(cardioSession);
                    cardioSession.setSyncDate(new Date());
                    sessionDAO.update(cardioSession);
                }
            } catch (SQLException ex) {
                Log.w(TAG, "workerThread.processItem() failed", ex);
            }
        }

        private boolean limitReached() {
            switch (limitType) {
                case LIMIT_TYPE_COUNT:
                    return countLimit > 0 && count >= countLimit;
                case LIMIT_TYPE_TIME:
                    return durationLimit > 0 && duration >= durationLimit;
                case LIMIT_TYPE_CUSTOM:
                    if (countLimit > 0 && count >= countLimit)
                        return true;
                    if (durationLimit > 0 && duration >= durationLimit)
                        return true;
                    return false;
                case LIMIT_TYPE_NONE:
                default:
                    return false;
            }
        }

        private void publishProgress() {
            double pct = 0;
            switch (limitType) {
                case LIMIT_TYPE_COUNT:
                    pct = ((double) count) / countLimit * 100;
                    break;
                case LIMIT_TYPE_TIME:
                    pct = ((double) duration) / durationLimit * 100;
                    break;
                case LIMIT_TYPE_CUSTOM:
                    if (countLimit > 0)
                        pct = ((double) count) / countLimit * 100;
                    if (durationLimit > 0 && duration >= durationLimit)
                        pct = Math.max(pct, ((double) duration) / durationLimit * 100);
                    break;
                case LIMIT_TYPE_NONE:
                default:
                    break;
            }
            final double progressPct = Math.min(100.0d, pct);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (Messenger m: mClients) {
                        try {
                            Message msg = Message.obtain(null, MSG_PROGRESS, limitType, 0);
                            msg.getData().putDouble("progress", progressPct);
                            m.send(msg);
                        } catch (RemoteException ex) {
                            // suppress this
                        }
                    }
                }
            });
        }
    }

}
