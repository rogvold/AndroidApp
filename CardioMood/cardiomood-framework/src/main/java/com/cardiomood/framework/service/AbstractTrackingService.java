package com.cardiomood.framework.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cardiomood.android.tools.thread.WorkerThread;
import com.cardiomood.framework.db.entity.AbstractSessionEntity;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
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
import java.util.concurrent.CountDownLatch;


/**
 * Abstract class that holds most of the logic for collecting data from BLE heart rate monitor.
 * Communication with this service is performed via Messenger.
 *
 * @param <S> the type representing session to be recorded.
 */
public abstract class AbstractTrackingService<S extends AbstractSessionEntity> extends Service {

    private static final String TAG = AbstractTrackingService.class.getSimpleName();

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

    Object sessionLock = new Object();

    volatile S mCardioSession = null;
    volatile WorkerThread<CardioDataPackage> mWorkerThread;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private Messenger mMessenger = null;

    /**
     * Handler of incoming messages from clients.
     */
    protected class IncomingHandler extends Handler {
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

    public class HrmCallback implements LeHRMonitor.Callback {
        @Override
        public void onBPMChanged(int bpm) {

        }

        @Override
        public void onConnectionStatusChanged(final int oldStatus, final int newStatus) {
            if (hrMonitor != null && newStatus == LeHRMonitor.CONNECTED_STATUS) {
                hrMonitor.setAutoReconnect(isAutoReconnect());
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
                if (getCurrentSession() != null /* && mWorkerThread != null*/) {
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
            if (getCurrentSession() != null && mWorkerThread != null) {
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
                    synchronized (sessionLock) {
                        S session = getCurrentSession();
                        if (session != null) {
                            msg.getData().putLong("t", data.getTimestamp() - session.getStartTimestamp());
                        }
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
    }

    protected abstract NotificationCompat.Builder setupNotificationBuilder();
    protected abstract IncomingHandler createIncomingHandler();
    protected abstract boolean isAutoReconnect();
    protected abstract S createSession(Bundle params) throws SQLException;
    protected abstract void updateSession(S session) throws SQLException;
    protected abstract void finalizeSession(S session) throws SQLException;
    protected abstract void updateSessionInfo(S session, Bundle params) throws SQLException;

    protected abstract CardioDataCollector createDataCollector(S cardioSession, int limitType,
                                                               int durationLimit, int countLimit);


    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        hrMonitor = null;
        mClients = new ArrayList<>();

        // create NotificationManager
        mNotificationBuilder = setupNotificationBuilder();
        // create notification
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mMessenger = new Messenger(createIncomingHandler());
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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
    }

    protected synchronized final S getCurrentSession() {
        synchronized (sessionLock) {
            return mCardioSession;
        }
    }

    protected synchronized final void setCurrentSession(S session) {
        synchronized (sessionLock) {
            mCardioSession = session;
        }
    }



    protected final void reloadMonitor() {
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
        hrMonitor.setCallback(getHrmCallback());
    }

    protected LeHRMonitor.Callback getHrmCallback() {
        return new HrmCallback();
    }

    protected final boolean init() {
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

    protected final boolean connect(String address) {
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

    protected final void disconnect() {
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

    protected void handleConnectionStatusRequest(Message msg) {
        try {
            if (msg.replyTo != null) {
                Message response = Message.obtain(null, RESP_STATUS,
                        hrMonitor == null ? LeHRMonitor.INITIAL_STATUS : hrMonitor.getConnectionStatus(), 0);
                response.getData().putString("deviceAddress", mDeviceAddress);
                synchronized (sessionLock) {
                    S session = getCurrentSession();
                    if (session != null) {
                        response.getData().putLong("sessionId", session.getId());
                    }
                }
                msg.replyTo.send(response);
            }
        } catch (RemoteException ex) {
            Log.d(TAG, "handleConnectionStatusRequest() failed", ex);
        }
    }

    protected void handleInitRequest(Message msg) {
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

    protected void handleConnectRequest(Message msg) {
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

    protected void handleDisconnectRequest(Message msg) {
        try {
            synchronized (sessionLock) {
                S session = getCurrentSession();
                if (session != null) {
                    Message endSessionMsg = Message.obtain(null, MSG_END_SESSION);
                    endSessionMsg.replyTo = msg.replyTo;
                    handleEndSessionRequest(msg);
                    setCurrentSession(null);
                    // disconnect will be called when the worker thread is finished
                    return;
                }
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

    protected void handleResetRequest(Message msg) {
        synchronized (sessionLock) {
            S session = getCurrentSession();
            if (session != null) {
                Message endSessionMsg = Message.obtain(null, MSG_END_SESSION);
                endSessionMsg.replyTo = msg.replyTo;
                handleEndSessionRequest(msg);
                setCurrentSession(null);
                // disconnect will be called when the worker thread is finished
                return;
            }
        }
    }

    protected void handleStartSessionRequest(Message msg) {
        if (mCardioSession != null) {
            // already started!
            return;
        }
        try {
            try {
                synchronized (sessionLock) {
                    S cardioSession = createSession(msg.getData());
                    cardioSession.setName(getNewSessionName(cardioSession));
                    cardioSession.setDescription(getNewSessionDescription(cardioSession));
                    updateSession(cardioSession);
                    setCurrentSession(cardioSession);

                    int limitType = msg.getData().getInt("limitType", 0);
                    int durationLimit = msg.getData().getInt("durationLimit", 120);
                    int countLimit = msg.getData().getInt("countLimit", 100);
                    // start worker thread

                    mWorkerThread = createDataCollector(cardioSession, limitType, durationLimit, countLimit);
                    mWorkerThread.start();

                    // SUCCESS
                    if (msg.replyTo != null) {
                        Message response = Message.obtain(null, RESP_START_SESSION_RESULT, RESULT_SUCCESS, 0);
                        response.getData().putLong("sessionId", cardioSession.getId());
                        response.getData().putLong("startTimestamp", cardioSession.getStartTimestamp());
                        msg.replyTo.send(response);
                    }
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

    protected void showSessionFinishedNotification(S session) {
        // this depends on implementation!
    }

    protected void handleEndSessionRequest(Message msg) {
        try {
            try {
                synchronized (sessionLock) {
                    S session = getCurrentSession();
                    if (session != null) {
                        finalizeSession(session);

                        // stop worker thread
                        if (mWorkerThread != null) {
                            mWorkerThread.finishWork();
                        }

                        showSessionFinishedNotification(session);

                        setCurrentSession(null);
                    }

                    // SUCCESS
                    if (msg.replyTo != null) {
                        Message response = Message.obtain(null, RESP_END_SESSION_RESULT, RESULT_SUCCESS, 0);
                        response.getData().putLong("sessionId", session.getId());
                        response.getData().putLong("endTimestamp", session.getEndTimestamp());
                        msg.replyTo.send(response);
                    } else {
                        for (Messenger m: mClients) {
                            Message response = Message.obtain(null, RESP_END_SESSION_RESULT, RESULT_SUCCESS, 0);
                            response.getData().putLong("sessionId", session.getId());
                            response.getData().putLong("endTimestamp", session.getEndTimestamp());
                            m.send(response);
                        }
                    }
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

    protected void handleUpdateInfoRequest(Message msg) {
        try {
            synchronized (sessionLock) {
                S session = getCurrentSession();
                try {
                    updateSessionInfo(session, msg.getData());
                } catch (SQLException ex) {
                    if (msg.replyTo != null) {
                        Message response = Message.obtain(null, MSG_UPDATE_INFO, RESULT_FAIL, 0);
                        response.getData().putString("errorMessage", ex.getMessage());
                        msg.replyTo.send(response);
                    }
                }
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


    protected String getNewSessionName(S session) {
        return processMacros("Measurement $date$", session);
    }

    protected String getNewSessionDescription(S session) {
        return null;
    }

    protected String processMacros(String pattern, S session) {
        if (pattern == null)
            return null;
        return pattern.replaceAll("\\$date\\$", DATE_FORMAT.format(new Date(session.getStartTimestamp())))
                .replaceAll("\\$id\\$", String.valueOf(session.getId()))
                .replaceAll("\\$device_name\\$", getConnectedDeviceName())
                .replaceAll("\\$device_address\\$", getConnectedDeviceAddress());

    }

    protected String getConnectedDeviceAddress() {
        if (hrMonitor != null && hrMonitor.isConnectingOrConnected()) {
            return hrMonitor.getDeviceAddress();
        } else return "";
    }

    protected String getConnectedDeviceName() {
        if (hrMonitor != null && hrMonitor.isConnectingOrConnected()) {
            String name = hrMonitor.getDeviceName();
            if (name == null)
                return "";
            return name;
        } else return "";
    }

    protected boolean isRealTimeEnabled() {
        return true;
    }

    protected String getPubnubPubKey() {
        return "";
    }

    protected String getPubnubSubKey() {
        return "";
    }

    protected String getPubnubChannelName() {
        return "";
    }

    private class PubnubWorkerThread extends WorkerThread<JSONObject> {

        private String channelName = null;
        private Queue<JSONObject> buffer = new LinkedList<JSONObject>();
        private boolean realTimeEnabled = false;
        private Pubnub pubnub;

        @Override
        public void onStart() {
            super.onStart();
            realTimeEnabled = isRealTimeEnabled();
            pubnub = new Pubnub(getPubnubPubKey(), getPubnubSubKey());
            channelName = getPubnubChannelName();
        }

        private boolean publish(JSONObject item) {
            String msg = item.toString();
            Log.d(TAG, "Pubnub.publish() -> " + msg);
            if (!realTimeEnabled) {
                Log.d(TAG, "real-time is disabled, message wasn't published");
                return true;
            } else {
                final CountDownLatch latch = new CountDownLatch(1);
                final Throwable error[] = new Throwable[1];
                pubnub.publish(channelName, item, false, new Callback() {
                    @Override
                    public void successCallback(String s, Object o) {
                        latch.countDown();
                        Log.d(TAG, "Pubnub.publish() -> published");
                    }

                    @Override
                    public void errorCallback(String s, PubnubError pubnubError) {
                        if (pubnubError != null) {
                            error[0] = new RuntimeException(pubnubError.getErrorString());
                        } else {
                            error[0] = new RuntimeException("Unknown error");
                        }
                        latch.countDown();
                        Log.w(TAG, "Pubnub.publish() -> failed", error[0]);
                    }
                });
                try {
                    latch.await();
                    return true;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }

            return false;
        }

        @Override
        public void processItem(JSONObject item) {
            buffer.add(item);
            while (!buffer.isEmpty()) {
                if (publish(buffer.peek())) {
                    buffer.remove();
                } else {
                    break;
                }
            }
        }
    }

    abstract class CardioDataCollector extends WorkerThread<CardioDataPackage> {

        public static final int LIMIT_TYPE_NONE = 0;
        public static final int LIMIT_TYPE_TIME = 1;
        public static final int LIMIT_TYPE_COUNT = 2;
        public static final int LIMIT_TYPE_CUSTOM = 3;

        protected final S cardioSession;

        protected final int limitType;
        protected final int durationLimit;
        protected final int countLimit;

        protected volatile long T0 = -1, T = -1;
        protected volatile int duration = 0;
        protected volatile int count = 0;

        protected volatile WorkerThread<JSONObject> pubnubWorkerThread = new PubnubWorkerThread();

        private CardioDataCollector(S cardioSession, int limitType, int durationLimit, int countLimit) {
            this.cardioSession = cardioSession;
            this.limitType = limitType;
            this.durationLimit = durationLimit*1000;
            this.countLimit = countLimit;
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
                    finalizeSession(cardioSession);
                } catch (SQLException ex) {
                    // suppress this
                }
            }
        }

        @Override
        public void onStart() {
            super.onStart();
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

        protected abstract JSONObject createPubnubMessage(S session, CardioDataPackage item) throws JSONException;

        private void publishItem(CardioDataPackage item) {
            try {
                pubnubWorkerThread.put(createPubnubMessage(cardioSession, item));
            } catch (JSONException ex) {
                Log.e(TAG, "publishItem() failed with exception", ex);
            }
        }

        protected abstract void saveDataItem(S session, long timestamp, int rr, int bpm) throws SQLException;

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
                    saveDataItem(cardioSession, T, rr, item.getBpm());

                    duration += rr;
                    count++;
                    publishProgress();
                    if (limitReached()) {
                        finishWork();
                        break;
                    }
                }
            } catch (SQLException ex) {
                Log.w(TAG, "workerThread.processItem() failed", ex);
            }
        }

        protected boolean limitReached() {
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

        protected double getCurrentProgress() {
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
            return Math.min(100.0d, pct);
        }

        protected void publishProgress() {
            final double progress = getCurrentProgress();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (Messenger m: mClients) {
                        try {
                            Message msg = Message.obtain(null, MSG_PROGRESS, limitType, 0);
                            msg.getData().putDouble("progress", progress);
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
