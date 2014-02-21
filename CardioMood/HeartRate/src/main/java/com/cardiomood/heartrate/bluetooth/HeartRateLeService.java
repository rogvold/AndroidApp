package com.cardiomood.heartrate.bluetooth;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by danon on 18.11.13.
 */
public abstract class HeartRateLeService extends Service {

    private final static String TAG = HeartRateLeService.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 2;

    private final IBinder mBinder = new LocalBinder();
    private LeHRMonitor monitor;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            HeartRateLeService.this.sendBroadcast(intent);
        }
    };

    public class LocalBinder extends Binder {
        public HeartRateLeService getService() {
            return HeartRateLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        monitor = LeHRMonitor.getMonitor(this);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        monitor = null;
        return super.onUnbind(intent);
    }

    // -------------- API methods ----------------------------------------------

    public boolean initialize(Activity owner) {
        if (monitor == null) {
            monitor = LeHRMonitor.getMonitor(this);
            if (monitor == null)
                return false;
        }
        BluetoothAdapter bluetoothAdapter = monitor.getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            Log.w(TAG, "initialize(): bluetooth adapter is not available");
            return false;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "initialize(): bluetooth adapter is not enabled");
        }
        return monitor.initialize();
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (monitor == null || monitor.getConnectionStatus() != LeHRMonitor.READY_STATUS) {
            Log.w(TAG, "connect() - monitor is not initialized or is already connected!");
            return false;
        }
        return monitor.connect(address);
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (monitor != null) {
            if (monitor.getConnectionStatus() != LeHRMonitor.CONNECTED_STATUS) {
                Log.w(TAG, "Monitor is not connected");
                return;
            }
            monitor.disconnect();
        }
    }

    public LeHRMonitor getMonitor() {
        return monitor;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (monitor == null) {
            return;
        }

        monitor.close();
        monitor = LeHRMonitor.getMonitor(this);
    }
}
