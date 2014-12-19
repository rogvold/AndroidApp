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
    protected boolean doInitialize() {
        return false;
    }

    @Override
    protected boolean doConnect(String deviceAddress) {
        return false;
    }

    @Override
    protected void doDisconnect() {
    }

    @Override
    protected void doClose() {
        setConnectionStatus(INITIAL_STATUS);
    }

    @Override
    public BluetoothAdapter getCurrentBluetoothAdapter() {
        return getBluetoothAdapter();
    }
}
