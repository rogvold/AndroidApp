package com.cardiomood.android.kolomna.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cardiomood.android.kolomna.MainActivity;
import com.cardiomood.android.kolomna.R;
import com.cardiomood.android.kolomna.db.CardioItemDAO;
import com.cardiomood.android.kolomna.db.CardioSessionDAO;
import com.cardiomood.android.kolomna.db.HelperFactory;
import com.cardiomood.android.kolomna.db.entity.CardioItemEntity;
import com.cardiomood.android.kolomna.db.entity.CardioSessionEntity;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;

import bolts.Task;

public class CardioMonitoringService extends Service {

    private static final String TAG = CardioMonitoringService.class.getSimpleName();

    private static final int SERVICE_NOTIFICATION_ID = 4343;

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

    public static final int MSG_HR_DATA = 10;
    public static final int MSG_CONNECTION_STATUS_CHANGED = 11;
    public static final int MSG_BATTERY_LEVEL = 12;


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

    volatile CardioSessionEntity mCardioSession = null;
    WorkerThread<CardioDataPackage> mWorkerThread;

    Pubnub pubnub = new Pubnub("pub-c-a86ef89b-7858-4b4c-8f89-c4348bfc4b79",
            "sub-c-e5ae235a-4c3e-11e4-9e3d-02ee2ddab7fe");

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
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (newStatus != LeHRMonitor.CONNECTED_STATUS) {
                        if (mCardioSession != null && mWorkerThread != null) {
                            handleEndSessionRequest(Message.obtain(null, MSG_END_SESSION));
                        }
                    }
                    for (Messenger m: mClients) {
                        try {
                            Message msg = Message.obtain(null, MSG_CONNECTION_STATUS_CHANGED, oldStatus, newStatus);
                            msg.getData().putString("deviceAddress", mDeviceAddress);
                            m.send(msg);
                        } catch (RemoteException ex) {
                            Log.w(TAG, "onConnectionStatusChanged() failed", ex);
                        }
                    }
                }
            });

        }

        @Override
        public void onDataReceived(int bpm, short[] rr) {
            long timestamp = System.currentTimeMillis();
            CardioDataPackage data = new CardioDataPackage(timestamp, bpm, toIntArray(rr));
            if (mCardioSession != null && mWorkerThread != null) {
                mWorkerThread.put(data);
            }
            for (Messenger m: mClients) {
                try {
                    Message msg = Message.obtain(null, MSG_HR_DATA, bpm, 0);
                    msg.getData().setClassLoader(CardioDataPackage.class.getClassLoader());
                    msg.getData().putParcelable("data", data);
                    msg.getData().putString("deviceAddress", mDeviceAddress);
                    m.send(msg);
                } catch (RemoteException ex) {
                    Log.w(TAG, "onDataReceived() failed", ex);
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
        mClients = new ArrayList<Messenger>();

        // create NotificationManager
        mNotificationBuilder = setupNotificationBuilder();
        // create notification
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        HelperFactory.setHelper(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        try {
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
                        .setContentTitle("MIPT Health is running")
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
                if (hrMonitor.isConnectingOrConnected()) {
                    hrMonitor.disconnect();
                }
                hrMonitor.close();
                hrMonitor.setCallback(null);
            }
            hrMonitor = null;
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
                TransactionManager.callInTransaction(HelperFactory.getHelper().getConnectionSource(), new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        CardioSessionDAO sessionDao = HelperFactory.getHelper().getCardioSessionDao();
                        CardioSessionEntity cardioSession = new CardioSessionEntity();
                        cardioSession.setStartTimestamp(System.currentTimeMillis());
                        cardioSession.setCreationDate(new Date());
                        cardioSession.setSyncDate(new Date());
                        cardioSession.setSyncUserId(parseUserId);
                        sessionDao.create(cardioSession);
                        mCardioSession = cardioSession;
                        return null;
                    }
                });

                // start worker thread
                mWorkerThread = new CardioDataCollector(mCardioSession);
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
                    TransactionManager.callInTransaction(HelperFactory.getHelper().getConnectionSource(), new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            CardioSessionDAO sessionDao = HelperFactory.getHelper().getCardioSessionDao();
                            sessionDao.refresh(mCardioSession);
                            mCardioSession.setEndTimestamp(System.currentTimeMillis());
                            mCardioSession.setSyncDate(new Date(mCardioSession.getEndTimestamp()));
                            sessionDao.update(mCardioSession);
                            return null;
                        }
                    });

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


    private class PubnubWorkerThread extends WorkerThread<JSONObject> {

        private final String CHANNEL_NAME = "KolomnaRealTime";
        private Queue<JSONObject> buffer = new LinkedList<JSONObject>();

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
            pubnub.publish(CHANNEL_NAME, item, false, new Callback() {
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

        private CardioSessionEntity cardioSession;
        private CardioSessionDAO sessionDAO;
        private CardioItemDAO itemDao;
        private long T = -1;

        private DataWindow.Timed stressDataWindow = new DataWindow.Timed(120 * 1000, 5000);
        private DataWindow.IntervalsCount sdnnDataWindow = new DataWindow.IntervalsCount(20, 5);
        private final ArtifactFilter filter = new PisarukArtifactFilter();

        private Double currentSDNN = null;
        private Integer currentStress = null;

        private WorkerThread<JSONObject> pubnubWorkerThread = new PubnubWorkerThread();

        private CardioDataCollector(CardioSessionEntity cardioSession) throws SQLException {
            this.cardioSession = cardioSession;
            this.sessionDAO = HelperFactory.getHelper().getCardioSessionDao();
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
                    mCardioSession = null;
                    disconnect();
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
                if (T < 0) {
                    // save start of session
                    T = item.getTimestamp();
                }

                // t = milliseconds from the beginning
                long t = item.getTimestamp() - T;
                for (int rr : item.getRr()) {
                    stressDataWindow.add(rr);
                    sdnnDataWindow.add(rr);
                    CardioItemEntity entity = new CardioItemEntity();
                    entity.setSession(cardioSession);
                    entity.setT(t);
                    entity.setRr(rr);
                    entity.setBpm(item.getBpm());
                    t += rr;

                    itemDao.create(entity);
                }

                sessionDAO.refresh(cardioSession);
                cardioSession.setSyncDate(new Date());
                sessionDAO.update(cardioSession);
            } catch (SQLException ex) {
                Log.w(TAG, "workerThread.processItem() failed", ex);
            }
        }
    }

}
