package com.cardiomood.heartrate.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

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

    public LeHRMonitor(Context context) {
        this.context = context;
    }

    public static LeHRMonitor getMonitor(Context context) {
        LeHRMonitor monitor = null;
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

        try {
            return new FallbackMonitor(context);
        } catch (NoClassDefFoundError ex) {
            Log.wtf(TAG, "getMonitor(): shit just got real...", ex);
        }

        return null;
    }

    public abstract boolean isSupported();
    public abstract boolean initialize();
    public abstract boolean connect(String deviceAddress);
    public abstract void disconnect();
    public abstract void close();
    public abstract BluetoothAdapter getCurrentBluetoothAdapter();

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public boolean requestBatteryLevel() {
        return false;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        // Use BluetoothManager for API level >= 18
        if (Build.VERSION.SDK_INT >= 18) {
            BluetoothManager btMan = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            return btMan == null ? null : btMan.getAdapter();
        }
        // Use default BluetoothAdapter on API level < 18
        return BluetoothAdapter.getDefaultAdapter();
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        notifyBatteryLevel(batteryLevel);
        this.batteryLevel = batteryLevel;
    }

    public int getLastBPM() {
        return lastBPM;
    }

    public void setLastBPM(int bpm) {
        if (lastBPM != bpm) {
            notifyBPMChanged(lastBPM, bpm);
            lastBPM = bpm;
        }
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        if (this.connectionStatus != connectionStatus) {
            notifyConnectionStatusChanged(this.connectionStatus, connectionStatus);
            this.connectionStatus = connectionStatus;
        }
    }

    protected void notifyBatteryLevel(int batteryLevel) {
        Intent intent = new Intent(ACTION_BATTERY_LEVEL);
        intent.putExtra(EXTRA_BATTERY_LEVEL, batteryLevel);
        context.sendBroadcast(intent);

        if (callback != null) {
            callback.onBatteryLevelReceived(batteryLevel);
        }
    }

    protected void notifyConnectionStatusChanged(int oldStatus, int newStatus) {
        Intent intent = new Intent(ACTION_CONNECTION_STATUS_CHANGED);
        intent.putExtra(EXTRA_OLD_STATUS, oldStatus);
        intent.putExtra(EXTRA_NEW_STATUS, newStatus);
        context.sendBroadcast(intent);

        if (callback != null) {
            callback.onConnectionStatusChanged(oldStatus, newStatus);
        }
    }

    protected void notifyBPMChanged(int oldBPM, int newBPM) {
        Intent intent = new Intent(ACTION_BPM_CHANGED);
        intent.putExtra(EXTRA_OLD_BPM, oldBPM);
        intent.putExtra(EXTRA_NEW_BPM, newBPM);
        context.sendBroadcast(intent);

        if (callback != null) {
            callback.onBPMChanged(newBPM);
        }
    }

    protected void notifyHeartRateDataReceived(int bpm, short energyExpended, short[] rrIntervals) {
        Log.d(TAG, "notifyHeartRateDataReceived(): bpm=" + bpm);
        Intent intent = new Intent(ACTION_HEART_RATE_DATA_RECEIVED);
        intent.putExtra(EXTRA_BPM, bpm);
        intent.putExtra(EXTRA_ENERGY_EXPENDED, energyExpended);
        intent.putExtra(EXTRA_INTERVALS, rrIntervals);
        context.sendBroadcast(intent);

        if (callback != null) {
            callback.onDataReceived(bpm, rrIntervals);
        }
    }

    public static interface Callback {
        void onBPMChanged(int bpm);
        void onConnectionStatusChanged(int oldStatus, int newStatus);
        void onDataReceived(int bpm, short[] rr);
        void onBatteryLevelReceived(int level);
    }
}
