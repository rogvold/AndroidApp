package com.cardiomood.heartrate.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by danon on 18.11.13.
 */
public abstract class LeHRMonitor {

    private static final String TAG = LeHRMonitor.class.getSimpleName();

    public static final String ACTION_CONNECTION_STATUS_CHANGED = "com.cardiomood.android.bluetooth.ACTION_CONNECTION_STATUS_CHANGED";
    public static final String ACTION_BPM_CHANGED = "com.cardiomood.android.bluetooth.ACTION_BPM_CHANGED";
    public static final String ACTION_HEART_RATE_DATA_RECEIVED = "com.cardiomood.android.bluetooth.ACTION_HEART_RATE_DATA_RECEIVED";
    public static final String ACTION_BATTERY_LEVEL = "com.cardiomood.android.bluetooth.ACTION_BATTERY_LEVEL";

    public static final String EXTRA_OLD_STATUS = "com.cardiomood.android.bluetooth.EXTRA_OLD_STATUS";
    public static final String EXTRA_NEW_STATUS = "com.cardiomood.android.bluetooth.EXTRA_NEW_STATUS";
    public static final String EXTRA_OLD_BPM = "com.cardiomood.android.bluetooth.EXTRA_OLD_BPM";
    public static final String EXTRA_NEW_BPM = "com.cardiomood.android.bluetooth.EXTRA_NEW_BPM";
    public static final String EXTRA_BPM = "com.cardiomood.android.bluetooth.EXTRA_BPM";
    public static final String EXTRA_INTERVALS = "com.cardiomood.android.bluetooth.EXTRA_INTERVALS";
    public static final String EXTRA_ENERGY_EXPENDED = "com.cardiomood.android.bluetooth.EXTRA_ENERGY_EXPENDED";
    public static final String EXTRA_BATTERY_LEVEL = "com.cardiomood.android.bluetooth.EXTRA_BATTERY_LEVEL";

    public static final int INITIAL_STATUS = 0;
    public static final int READY_STATUS = 1;
    public static final int CONNECTING_STATUS = 2;
    public static final int CONNECTED_STATUS = 3;
    public static final int DISCONNECTING_STATUS = 4;

    private int lastBPM = 0;
    private int batteryLevel = -1;
    private int connectionStatus = INITIAL_STATUS;
    private Context context;
    private Callback callback;
    private boolean enableBroadcasts = true;
    private String deviceAddress;

    private boolean autoReconnect = false;
    private int maxReconnectionAttempts = 0;
    private int reconnectionAttemptsMade = 0;
    private boolean reconnectFlag = false;

    private Handler mHandler;

    private Timer timer = new Timer("connection_freeze_timer");
    private TimerTask connectingTimeoutTask;


    public LeHRMonitor(Context context) {
        this.context = context;
        mHandler = new Handler();
    }

    @SuppressWarnings("NewApi")
    public static LeHRMonitor getMonitor(Context context) {
        LeHRMonitor monitor = null;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                return new FallbackMonitor(context);
            }

            try {
                monitor = new AndroidLeHRMonitor(context);
                if (monitor.isSupported())
                    return monitor;
            } catch (NoClassDefFoundError ex) {
                Log.w(TAG, "getMonitor(): this is not Android 4.3+ device.", ex);
            }

            try {
                monitor = new MotorolaLeHRMonitor(context);
                if (monitor.isSupported())
                    return monitor;
            } catch (NoClassDefFoundError ex) {
                Log.w(TAG, "getMonitor(): this device not Motorola.", ex);
            }
        } catch (Throwable th) {
            Log.w(TAG, "getMonitor(): this device is not supported");
        }

        // returning a mock monitor instance
        return new FallbackMonitor(context);
    }

    protected abstract boolean doInitialize();
    protected abstract boolean doConnect(String deviceAddress);
    protected abstract void doDisconnect();
    protected abstract void doClose();
    public abstract boolean isSupported();
    public abstract BluetoothAdapter getCurrentBluetoothAdapter();

    public synchronized boolean initialize() {
        return doInitialize();
    }
    public synchronized boolean connect(String deviceAddress) {
        this.deviceAddress = deviceAddress;
        return doConnect(deviceAddress);
    }

    public synchronized void disconnect() {
        doDisconnect();
    }
    public synchronized void close() {
        doClose();
        deviceAddress = null;
    }


    public synchronized void enableBroadcasts(boolean enable) {
        enableBroadcasts = enable;
    }

    public synchronized void setCallback(Callback callback) {
        this.callback = callback;
    }

    public synchronized boolean requestBatteryLevel() {
        return false;
    }

    public synchronized BluetoothAdapter getBluetoothAdapter() {
        // Use BluetoothManager for API level >= 18
        if (Build.VERSION.SDK_INT >= 18) {
            BluetoothManager btMan = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            return btMan == null ? null : btMan.getAdapter();
        }
        // Use default BluetoothAdapter on API level < 18
        return BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized Context getContext() {
        return context;
    }

    public synchronized void setContext(Context context) {
        this.context = context;
    }

    public synchronized int getBatteryLevel() {
        return batteryLevel;
    }

    public synchronized void setBatteryLevel(final int batteryLevel) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyBatteryLevel(batteryLevel);
            }
        });
        this.batteryLevel = batteryLevel;
    }

    public synchronized int getLastBPM() {
        return lastBPM;
    }

    public synchronized void setLastBPM(final int bpm) {
        if (lastBPM != bpm) {
            final int oldBpm = lastBPM;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyBPMChanged(oldBpm, bpm);
                }
            });
            lastBPM = bpm;
        }
    }

    public synchronized boolean isAutoReconnect() {
        return autoReconnect;
    }

    public synchronized void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public synchronized int getMaxReconnectionAttempts() {
        return maxReconnectionAttempts;
    }

    public synchronized void setMaxReconnectionAttempts(int maxReconnectionAttempts) {
        this.maxReconnectionAttempts = maxReconnectionAttempts;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public synchronized boolean isConnected() {
        return CONNECTED_STATUS == getConnectionStatus();
    }

    public synchronized boolean isConnectingOrConnected() {
        return (getConnectionStatus() == CONNECTING_STATUS ||
                getConnectionStatus() == CONNECTED_STATUS);
    }

    public synchronized int getConnectionStatus() {
        return connectionStatus;
    }

    public synchronized void setConnectionStatus(final int connectionStatus) {
        Log.w(TAG, "setConnectionStatus(): " + this.connectionStatus + " -> " + connectionStatus);
        if (this.connectionStatus != connectionStatus) {
            final int oldStatus = this.connectionStatus;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyConnectionStatusChanged(oldStatus, connectionStatus);
                }
            });
            this.connectionStatus = connectionStatus;
        }
    }

    protected synchronized void notifyBatteryLevel(int batteryLevel) {
        if (enableBroadcasts) {
            Intent intent = new Intent(ACTION_BATTERY_LEVEL);
            intent.putExtra(EXTRA_BATTERY_LEVEL, batteryLevel);
            context.sendBroadcast(intent);
        }

        if (callback != null) {
            callback.onBatteryLevelReceived(batteryLevel);
        }
    }

    protected synchronized void notifyConnectionStatusChanged(int oldStatus, int newStatus) {
        if (connectingTimeoutTask != null) {
            connectingTimeoutTask.cancel();
            connectingTimeoutTask = null;
            timer.purge();
        }

        if (newStatus == CONNECTING_STATUS) {
            // make sure connecting lasts not more than 10 seconds
            connectingTimeoutTask = new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (getConnectionStatus() == CONNECTING_STATUS) {
                                disconnect();
                                if (!isAutoReconnect()) {
                                    setConnectionStatus(READY_STATUS);
                                }
                            }
                        }
                    });

                }
            };
            timer.schedule(connectingTimeoutTask, 10000);
        }

        if (newStatus == CONNECTING_STATUS) {
            reconnectionAttemptsMade = 0;
        }

        if (isAutoReconnect()) {
            // auto-reconnect enabled
            boolean tryAgain = false;
            if (reconnectFlag && newStatus != CONNECTING_STATUS) {
                reconnectFlag = false;
                // tried to reconnect and the attempt finished
                tryAgain = newStatus != CONNECTED_STATUS;
                if (getMaxReconnectionAttempts() > 0) {
                    if (reconnectionAttemptsMade >= getMaxReconnectionAttempts()) {
                        tryAgain = false;
                    }
                }
            }

            if (!reconnectFlag && (tryAgain || oldStatus == CONNECTED_STATUS)) {
                // we are not in reconnecting phase,
                // and all previous attempts (if any) failed

                reconnectionAttemptsMade++;
                final String deviceAddress = getDeviceAddress();
                reconnectFlag = true;
                connect(deviceAddress);
                notifyReconnecting(reconnectionAttemptsMade);
                return;
            }
        }

        if (isAutoReconnect() && reconnectFlag) {
            // prevent notifications when in reconnecting cycle
            return;
        }

        if (enableBroadcasts) {
            Intent intent = new Intent(ACTION_CONNECTION_STATUS_CHANGED);
            intent.putExtra(EXTRA_OLD_STATUS, oldStatus);
            intent.putExtra(EXTRA_NEW_STATUS, newStatus);
            context.sendBroadcast(intent);
        }

        if (callback != null) {
            callback.onConnectionStatusChanged(oldStatus, newStatus);
        }
    }

    protected synchronized void notifyReconnecting(int attemptNumber) {
        if (callback != null) {
            callback.onReconnecting(attemptNumber, getMaxReconnectionAttempts());
        }
    }

    protected synchronized void notifyBPMChanged(int oldBPM, int newBPM) {
        if (enableBroadcasts) {
            Intent intent = new Intent(ACTION_BPM_CHANGED);
            intent.putExtra(EXTRA_OLD_BPM, oldBPM);
            intent.putExtra(EXTRA_NEW_BPM, newBPM);
            context.sendBroadcast(intent);
        }

        if (callback != null) {
            callback.onBPMChanged(newBPM);
        }
    }

    protected synchronized void notifyHeartRateDataReceived(final int bpm, final short energyExpended, final short[] rrIntervals) {
        Log.d(TAG, "notifyHeartRateDataReceived(): bpm=" + bpm);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (enableBroadcasts) {
                    Intent intent = new Intent(ACTION_HEART_RATE_DATA_RECEIVED);
                    intent.putExtra(EXTRA_BPM, bpm);
                    intent.putExtra(EXTRA_ENERGY_EXPENDED, energyExpended);
                    intent.putExtra(EXTRA_INTERVALS, rrIntervals);
                    context.sendBroadcast(intent);
                }

                if (callback != null) {
                    callback.onDataReceived(bpm, rrIntervals);
                }
            }
        });
    }

    public static interface Callback {
        void onBPMChanged(int bpm);
        void onConnectionStatusChanged(int oldStatus, int newStatus);
        void onDataReceived(int bpm, short[] rr);
        void onBatteryLevelReceived(int level);
        void onReconnecting(int attemptNumber, int maxAttempts);
    }
}
