package com.cardiomood.heartrate.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.motorola.bluetoothle.BluetoothGatt;
import com.motorola.bluetoothle.hrm.IBluetoothHrm;
import com.motorola.bluetoothle.hrm.IBluetoothHrmCallback;

import java.util.Arrays;

/**
 * Created by danon on 18.11.13.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MotorolaLeHRMonitor extends LeHRMonitor {

    private static final String TAG = MotorolaLeHRMonitor.class.getSimpleName();

    public static final ParcelUuid HRM = ParcelUuid.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    public static final String hrmUUID = HRM.toString();

    private Context mContext = null;

    //private final CountDownLatch latch = new CountDownLatch(1);

    private BluetoothDevice device;
    private BluetoothAdapter bluetoothAdapter;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            hrmService = IBluetoothHrm.Stub.asInterface(service);
            bleBound = true;
            //latch.countDown();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            hrmService = null;
            bleBound = false;
        }
    };
    private IBluetoothHrm hrmService = null;
    private boolean bleBound = false;
    private boolean leDisconnected = true;
    private BLECallback callback1;

    private boolean receiverRegistered = false;
    private IntentFilter filterScan = null;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, action);

            if (BluetoothGatt.CONNECT_COMPLETE.equals(action)) {
                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);

                String service = intent.getStringExtra("uuid");
                if (status == BluetoothGatt.SUCCESS) {
                    leDisconnected = false;
                    setConnectionStatus(CONNECTED_STATUS);
                    //if (getSensorLocation() == -1) {
                       // requestLocation();
                    //}

                    Log.e(TAG, "Connected!");

                } else {
                    setConnectionStatus(DISCONNECTING_STATUS);
                    leDisconnected = true;
                    try {
                        hrmService.disconnectLe(device, hrmUUID);
                    } catch (RemoteException e) {
                        Log.e(TAG, "CONNECT_COMPLETE -> not SUCCESS", e);
                    }
                    setConnectionStatus(READY_STATUS);
                }

            } else if (action.equals(BluetoothGatt.DISCONNECT_COMPLETE)) {
                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                //String service = intent.getStringExtra("uuid");
                if (status == BluetoothGatt.SUCCESS) {
                    setConnectionStatus(READY_STATUS);
                    device = null;
                } else if (status != BluetoothGatt.SUCCESS) {
                    setConnectionStatus(READY_STATUS);
                    device = null;
                }

            } else if ((BluetoothGatt.GET_COMPLETE).equals(action)) {
                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                String service = intent.getStringExtra("uuid");// Todo
                int length = intent.getIntExtra("length", 0);
                if (status == BluetoothGatt.SUCCESS) {
                    if ((length >= 0) && service.equalsIgnoreCase(hrmUUID)) {
                        //Log.d(TAG, "broadcastReceiver.onReceive(): received "+length+" bytes of data");
                        byte[] data = new byte[length];
                        data = intent.getByteArrayExtra("data");
                        //setSensorLocation(data[0]);
                    }
                } else {
                    Toast.makeText(context, "Sensor query failed! ",
                            Toast.LENGTH_LONG).show();
                }
            } else if (action.equals(BluetoothGatt.SET_COMPLETE)) {
                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                String service = intent.getStringExtra("uuid"); // Todo
                if (status == BluetoothGatt.SUCCESS
                        && service.equalsIgnoreCase(hrmUUID)) {
                } else if (status != BluetoothGatt.SUCCESS
                        && service.equalsIgnoreCase(hrmUUID)) {
                    Toast.makeText(context, "Notification enabling failed! ",
                            Toast.LENGTH_LONG).show();
                }
            }
        }

    };

    private byte[] previousData = null;


    public MotorolaLeHRMonitor(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean isSupported() {
        try {
            Class.forName("android.bluetooth.BluetoothGattService");
            Class.forName("com.motorola.bluetoothle.BluetoothGatt");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "isSupported() - exception:", e);
            return false;
        }
    }

    @Override
    protected boolean doInitialize() {
        if (getConnectionStatus() == READY_STATUS)
            return true; // already initialized

        if (getConnectionStatus() != INITIAL_STATUS) {
            throw new IllegalStateException("Incorrect connection status. Close service first.");
        }

        // get BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.w(TAG, "initialize() - Bluetooth adapter is not available.");
            return false;
        }

        // setup Intent Filter
        filterScan = new IntentFilter(BluetoothGatt.CONNECT_COMPLETE);
        filterScan.addAction(BluetoothGatt.DISCONNECT_COMPLETE);
        filterScan.addAction(BluetoothGatt.GET_COMPLETE);
        filterScan.addAction(BluetoothGatt.SET_COMPLETE);
        filterScan.addAction(BluetoothGatt.ACTION_GATT_CHARACTERISTICS_GET);
        filterScan.addAction(BluetoothGatt.ACTION_GATT_CHARACTERISTICS_READ);
        filterScan.addAction(BluetoothGatt.ACTION_GATT_CHARACTERISTICS_WRITE);

        // Register BroadcastReceiver
        mContext.registerReceiver(broadcastReceiver, filterScan);
        receiverRegistered = true;

        // Bind Motorola BLE Service
        Intent intent1 = new Intent(IBluetoothHrm.class.getName());
        getContext().bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);

        // start LE
        Intent intent2 = new Intent(BluetoothGatt.ACTION_START_LE);
        intent2.putExtra(BluetoothGatt.EXTRA_PRIMARY_UUID, hrmUUID);
        mContext.sendBroadcast(intent2);

        // create callback to receive data from the service
        callback1 = new BLECallback(hrmUUID);

        previousData = new byte[0];

        // todo: this method is asynchronous!!!
//        try {
//            latch.await();
//        } catch (InterruptedException ex) {
//            Thread.currentThread().interrupt();
//        }
        setConnectionStatus(READY_STATUS);
        // initialized!
        return true;
    }

    @Override
    protected boolean doConnect(String deviceAddress) {
        if (getConnectionStatus() == READY_STATUS && deviceAddress != null) {
            device = bluetoothAdapter.getRemoteDevice(deviceAddress);

            if (device != null && callback1 != null && hrmUUID != null) {
                try {
                    int status = hrmService.connectLe(device, hrmUUID, callback1);
                    if (status == BluetoothGatt.SUCCESS) {
                        setConnectionStatus(CONNECTING_STATUS);
                        return true;
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "connect(): connectLe() failed", ex);
                    setConnectionStatus(READY_STATUS);
                    leDisconnected = true;
                }
            }
        } else {
            throw new IllegalStateException("Incorrect connection status for this operation.");
        }

        return false;
    }

    @Override
    protected void doDisconnect() {
        if (!leDisconnected && (getConnectionStatus() == CONNECTED_STATUS || getConnectionStatus() == CONNECTING_STATUS)) {
            try {
                int status = hrmService.disconnectLe(device, hrmUUID);
                if (status == BluetoothGatt.SUCCESS) {
                    setConnectionStatus(DISCONNECTING_STATUS);
                } else {
                    setConnectionStatus(READY_STATUS);
                }
            } catch (Exception ex) {
                Log.e(TAG, "disconnect(): disconnectLe() failed", ex);
                setConnectionStatus(READY_STATUS);
                leDisconnected = true;
            }
        } else {
            throw new IllegalStateException("Incorrect connection status for this operation.");
        }
    }

    @Override
    protected void doClose() {
        if (getConnectionStatus() == CONNECTED_STATUS) {
            disconnect();
        }
        if (receiverRegistered) {
            receiverRegistered = false;
            mContext.unregisterReceiver(broadcastReceiver);
        }

        if (hrmService != null) {
            mContext.unbindService(mConnection);
            mConnection = null;
            hrmService = null;
        }

        callback1 = null;

        setConnectionStatus(INITIAL_STATUS);
    }

    @Override
    public BluetoothAdapter getCurrentBluetoothAdapter() {
        return bluetoothAdapter;
    }

    private void parseData(int length, byte[] data) {
        if (length > 4 && Arrays.equals(data, previousData)) {
            Log.w(TAG, "RR-Intervals ignore???");
            return;
        }
        previousData = data;

        int bpm = 0;
        if (data[1] == 0) {
            bpm = (data[0] + 256) % 256;
        } else if (data[0] < 0) {
            bpm = ((data[0] & 0xFF) << 8) + (data[1] & 0xFF);
        }
        setLastBPM(bpm);

        short energyExpended = 0;
        energyExpended |= data[2] & 0xFF;
        energyExpended |= ((data[3] & 0xFF) << 8);

        short[] rrIntervals = getRRIntervals(data, length-4);

        notifyHeartRateDataReceived(bpm, energyExpended, rrIntervals);
    }

    private short[] getRRIntervals(byte[] data, int length) {
        final double A = 1.0; // 1000.0/1024.0 ???
        if(length <= 0) {
            return new short[]{};
        }
        short[] result = new short[length/2];
        for(int i=4; i<length+4; i+=2) {
            result[(i-4)/2] = (short)((((data[i+1] & 0xFF) << 8) + (data[i] & 0xFF)) * A);
        }
        return result;
    }


    public class BLECallback extends IBluetoothHrmCallback.Stub {
        private String service;

        public BLECallback(String serv) {
            service = serv;
        }

        public void indicationLeCb(BluetoothDevice device, String service, int length, byte[] data) {
            Log.e(TAG, "indicationLeCb()");
            parseData(length, data);
        }

        public void notificationLeCb(BluetoothDevice device, String service, int length, byte[] data) {
            Log.e(TAG, "notificationLeCb()");
            parseData(length, data);
        }
    }
}
