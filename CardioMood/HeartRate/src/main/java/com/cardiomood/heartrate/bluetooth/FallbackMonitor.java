package com.cardiomood.heartrate.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

/**
 * Created by danon on 11.03.14.
 */
public class FallbackMonitor extends LeHRMonitor {

    public FallbackMonitor(Context context) {
        super(context);
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public boolean initialize() {
        return false;
    }

    @Override
    public boolean connect(String deviceAddress) {
        return false;
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void close() {
        setConnectionStatus(INITIAL_STATUS);
    }

    @Override
    public BluetoothAdapter getCurrentBluetoothAdapter() {
        return getBluetoothAdapter();
    }
}
