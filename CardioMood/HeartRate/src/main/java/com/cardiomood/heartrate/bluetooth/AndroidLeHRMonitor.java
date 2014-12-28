package com.cardiomood.heartrate.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.UUID;

/**
 * Created by danon on 07.01.14.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AndroidLeHRMonitor extends LeHRMonitor {

    private static final String TAG = AndroidLeHRMonitor.class.getSimpleName();

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public final static UUID UUID_HEART_RATE_SERVICE =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HEART_RATE_MEASUREMENT_CHARACTERISTIC =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_BATTERY_SERVICE =
            UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_BATTERY_LEVEL_CHARACTERISTIC =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");


    private Context mContext = null;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                Log.i(TAG, "Connected to GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (getConnectionStatus() == DISCONNECTING_STATUS || getConnectionStatus() == CONNECTED_STATUS)
                    setConnectionStatus(READY_STATUS);
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Enable Heart Rate Measurement notification
                BluetoothGattService heartRateService = gatt.getService(UUID_HEART_RATE_SERVICE);
                if (heartRateService != null) {
                    BluetoothGattCharacteristic characteristic = heartRateService
                            .getCharacteristic(UUID_HEART_RATE_MEASUREMENT_CHARACTERISTIC);
                    if (characteristic == null) {
                        Log.wtf(TAG, "onServicesDiscovered(): Heart Rate Service doesn't have Heart Rate Measurement Characteristic!!! O_o");
                        return;
                    }
                    gatt.setCharacteristicNotification(characteristic, true);
                    // This is specific to Heart Rate Measurement.
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                } else {
                    Log.e(TAG, "onServicesDiscovered(): Heart Rate Service was not discovered.");
                    disconnect();
                    return;
                }
                setConnectionStatus(CONNECTED_STATUS);
            } else {
                setConnectionStatus(READY_STATUS);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processCharacteristic(gatt, characteristic);
            } else {
                Log.w(TAG, "onCharactaristicRead(): failed to read characteristic " + characteristic.getUuid() +": status = " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            processCharacteristic(gatt, characteristic);
        }

        private void processCharacteristic(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "processCharacteristic(): characteristic.uuid = " + characteristic.getUuid());

            // This is special handling for the Heart Rate Measurement profile.  Data parsing is
            // carried out as per profile specifications:
            // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
            if (UUID_HEART_RATE_MEASUREMENT_CHARACTERISTIC.equals(characteristic.getUuid())) {
                int flag = characteristic.getProperties();
                int format = -1;
                int offset = 1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(TAG, "Heart rate format UINT16.");
                    offset += 2;
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(TAG, "Heart rate format UINT8.");
                    offset += 1;
                }
                final short heartRate = characteristic.getIntValue(format, 1).shortValue();
                Log.d(TAG, String.format("Received heart rate: %d", heartRate));
                setLastBPM(heartRate);

                if ((flag & (1 << 3)) != 0) {
                    Log.d(TAG, "Energy expended bit is  1");
                    Log.d(TAG, "energy expended = " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset));
                    offset += 2;
                }

                if ((flag & (1 << 4)) != 0) {
                    Log.d(TAG, "RR intervals bit is  1");

                    byte[] value = characteristic.getValue();
                    short[] intervals = new short[(value.length-offset)/2];
                    int i = 0;
                    while(offset < value.length) {
                        intervals[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset).shortValue();
                        intervals[i] = (short) Math.round(1000.0f/1024.0f*intervals[i]);
                        offset += 2;
                        i++;
                    }
                    notifyHeartRateDataReceived(heartRate, (short) 0, intervals);
                }

            } else if (UUID_BATTERY_LEVEL_CHARACTERISTIC.equals(characteristic.getUuid())) {
                final int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                setBatteryLevel(batteryLevel);
                Log.d(TAG, String.format("Received battery level: %d", batteryLevel));
            } else {
                // unsupported UUID
                Log.w(TAG, "Unsupported UUID " + characteristic.getUuid());
            }
        }
    };

    public AndroidLeHRMonitor(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean isSupported() {
        // This implementation requires Android 4.3+
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2);
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null)
            return null;
        return bluetoothManager.getAdapter();
    }

    @Override
    protected boolean doInitialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        setConnectionStatus(READY_STATUS);
        return true;
    }

    @Override
    protected boolean doConnect(String deviceAddress) {
        if (mBluetoothAdapter == null || deviceAddress == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && deviceAddress.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGaаtt for connection.");
            setConnectionStatus(CONNECTING_STATUS);
            if (mBluetoothGatt.connect()) {
                mBluetoothGatt.discoverServices();
                return true;
            } else {
                setConnectionStatus(READY_STATUS);
                return false;
            }
        }

        setConnectionStatus(CONNECTING_STATUS);
        try {
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
            Log.d(TAG, "Trying to create a new connection.");
            mBluetoothDeviceAddress = deviceAddress;
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "connect(): unable to connect due to Exception", ex);
            setConnectionStatus(READY_STATUS);
            return false;
        }
    }

    @Override
    protected void doDisconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            if (getConnectionStatus() != INITIAL_STATUS) {
                setConnectionStatus(INITIAL_STATUS);
            }
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        setConnectionStatus(DISCONNECTING_STATUS);
    }

    @Override
    protected void doClose() {
        if (getConnectionStatus() != INITIAL_STATUS && getConnectionStatus() != READY_STATUS)
            disconnect();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mBluetoothDeviceAddress = null;
        setConnectionStatus(INITIAL_STATUS);
    }

    @Override
    public BluetoothAdapter getCurrentBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @Override
    public boolean requestBatteryLevel() {
        if (mBluetoothGatt == null)
            return  false;
        // Request battery level if supported
        BluetoothGattService batteryService = mBluetoothGatt.getService(UUID_BATTERY_SERVICE);
        if (batteryService != null) {
            Log.i(TAG, "onServicesDiscovered(): Battery service has been dicovered.");
            BluetoothGattCharacteristic characteristic = batteryService.getCharacteristic(UUID_BATTERY_LEVEL_CHARACTERISTIC);
            if (characteristic != null) {
                // This is specific to Battery Level.
                boolean s = mBluetoothGatt.readCharacteristic(characteristic);
                Log.v(TAG, "gatt.readCharacteristic(characteristic) => " + s);
                return s;
            }
        } else {
            Log.i(TAG, "onServicesDiscovered(): Battery Service is not supported. :(");
        }
        return super.requestBatteryLevel();
    }
}
