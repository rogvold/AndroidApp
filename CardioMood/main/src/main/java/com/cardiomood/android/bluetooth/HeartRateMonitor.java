package com.cardiomood.android.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.R;
import com.cardiomood.android.tools.IMonitors;
import com.cardiomood.android.tools.Tools;
import com.motorola.bluetoothle.BluetoothGatt;
import com.motorola.bluetoothle.hrm.IBluetoothHrm;
import com.motorola.bluetoothle.hrm.IBluetoothHrmCallback;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

class HeartRateMonitor {

    public static final ParcelUuid HRM = ParcelUuid.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    //public static final ParcelUuid BATTERY = ParcelUuid.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    public static final int CONNECTED = 2;
    public static final int CONNECTING = 1;
    public static final int DISCONNECTED = 0;
    public static final int DISCONNECTING = 3;
    public static final int INIT = -1;

    private static final String TAG = "HeartRateMonitor";

    // Bluetooth Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;

    private Activity mActivity;
    private Context mContext;
    private Callback mCallback;

    AlertDialog alertDialog;
    AlertDialog.Builder alert_paired;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    ProgressDialog aDialog;

    // sensor parameters
    private int sensorLocation = -1;
    private int energyExpended = 0;
    private int heartBeatsPerMinute = 0;

    private long lastIntervalDate = 0;
    private long lastIntervalsLength = 0;

    private BluetoothDevice device;
    private BluetoothAdapter bluetoothAdapter;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            hrmService = IBluetoothHrm.Stub.asInterface(service);
            bleBound = true;
        }

        public void onServiceDisconnected(ComponentName arg0) {
            hrmService = null;
            bleBound = false;
        }
    };
    private boolean bleSupported = false;
    private boolean bleBound = false;
    private boolean leDisconnected = true;
    boolean flag_leRcvrReg = false;
    private int leState = INIT;
    private IBluetoothHrm hrmService;
    private String hrmUUID;
    private IntentFilter filter_scan;
    private BLECallback callback1;

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
                    setConnectionStatus(CONNECTED);
                    if (getSensorLocation() == -1) {
                        requestLocation();
                    }

                    Log.e(TAG, "Connected!");

                } else {
                    setConnectionStatus(DISCONNECTED);
                    leDisconnected = true;
                    try {
                        hrmService.disconnectLe(device, hrmUUID);
                    } catch (RemoteException e) {
                        //Log.e(TAG, "", e);
                    }
                }

            } else if (action.equals(BluetoothGatt.DISCONNECT_COMPLETE)) {
                int status = intent.getIntExtra("status", BluetoothGatt.FAILURE);
                //String service = intent.getStringExtra("uuid");
                if (status == BluetoothGatt.SUCCESS) {
                    setConnectionStatus(DISCONNECTED);
                    device = null;
                } else if (status != BluetoothGatt.SUCCESS) {
                    setConnectionStatus(DISCONNECTED);
                    device = null;
                }

            }

            else if ((BluetoothGatt.GET_COMPLETE).equals(action)) {
                int status = intent
                        .getIntExtra("status", BluetoothGatt.FAILURE);
                String service = intent.getStringExtra("uuid");// Todo
                int length = intent.getIntExtra("length", 0);
                if (status == BluetoothGatt.SUCCESS) {
                    if ((length >= 0) && service.equalsIgnoreCase(hrmUUID)) {
                        //Log.d(TAG, "broadcastReceiver.onReceive(): received "+length+" bytes of data");
                        byte[] data = new byte[length];
                        data = intent.getByteArrayExtra("data");
                        setSensorLocation(data[0]);
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

    /** Constructor */
    public HeartRateMonitor(Activity activity) {
        mActivity = activity;
        mContext = activity.getApplicationContext();
    }

    public void initBLEService() {
        bleSupported = Tools.isBLESupported();
        if (!bleSupported) {
            throw new UnsupportedOperationException("This device doesn't support GATT service.");
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                throw new UnsupportedOperationException("Bluetooth adapter is not available.");
            }

            hrmUUID = HRM.toString();

            filter_scan = new IntentFilter(BluetoothGatt.CONNECT_COMPLETE);
            filter_scan.addAction(BluetoothGatt.DISCONNECT_COMPLETE);
            filter_scan.addAction(BluetoothGatt.GET_COMPLETE);
            filter_scan.addAction(BluetoothGatt.SET_COMPLETE);
            filter_scan.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);

            mContext.registerReceiver(broadcastReceiver, filter_scan);

            flag_leRcvrReg = true;
            Intent intent1 = new Intent(IBluetoothHrm.class.getName());
            mContext.bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);

            Intent intent2 = new Intent(BluetoothGatt.ACTION_START_LE);
            intent2.putExtra(BluetoothGatt.EXTRA_PRIMARY_UUID, hrmUUID);
            mContext.sendBroadcast(intent2);

            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mActivity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }

            callback1 = new BLECallback(hrmUUID);
        }
    }

    public void attemptConnection() {
        attemptConnection(null);
    }

    public void attemptConnection(String deviceAddress) {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            showPairedDeviceSelectDialog(deviceAddress);
        } else {
            throw new IllegalStateException("Bluetooth is not available.");
        }
    }

    public boolean isConnected() {
        return leState == CONNECTED;
    }

    public void requestLocation() {
        try {
            if(hrmService != null)
                hrmService.getLeData(device, hrmUUID, BluetoothGatt.OPERATION_READ_SENSOR_LOCATION);
        } catch (Exception e) {
            //Log.e(TAG, "pull sensor location ", e);
        }
    }

    public int getSensorLocation() {
        return sensorLocation;
    }

    public void setSensorLocation(int sensorLocation) {
        int oldLocation = sensorLocation;
        if (oldLocation != sensorLocation) {
            this.sensorLocation = sensorLocation;
            if (mCallback != null) {
                mCallback.onSensorLocationChange(oldLocation, sensorLocation);
            }
        }
    }

    public int getConnectionStatus() {
        return leState;
    }

    public void setConnectionStatus(int leState) {
        int oldState = this.leState;
        if (leState != oldState) {
            this.leState = leState;
            leStateChanged(oldState, leState);
        }
    }

    public int getHeartBeatsPerMinute() {
        return heartBeatsPerMinute;
    }

    public int getEnergyExpended() {
        return energyExpended;
    }

    public void disconnect() {
        if (!leDisconnected) {
            if (leState == CONNECTED) {
                if (device != null) {
                    setConnectionStatus(DISCONNECTING);
                    try {
                        hrmService.disconnectLe(device, hrmUUID);
                    } catch (RemoteException e) {

                    } finally {
                        setConnectionStatus(DISCONNECTED);
                    }
                }
            }
            leDisconnected = true;
        }
    }

    public void cleanup() {
        if (flag_leRcvrReg) {
            flag_leRcvrReg = false;
            mContext.unregisterReceiver(broadcastReceiver);
        }

        if (hrmService != null) {
            mContext.unbindService(mConnection);
        }

        if (mConnection != null)
            mConnection = null;
        if (callback1 != null)
            callback1 = null;
        if (hrmService != null)
            hrmService = null;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public void finalize() {
        try {
            disconnect();
            cleanup();
        } catch (Throwable ex) {
            // nothing to do here
        }
    }

    private byte[] previousData = new byte[]{};

    private void parseData(int length, byte[] data) {
        if (length > 4 && Arrays.equals(data, previousData)) {
            Log.w(TAG, "RR-Intervals ignore???");
            return;
        }
        previousData = data;

        int _bpm = 0;
        _bpm = 0;
        if (data[1] == 0) {
            _bpm = (data[0] + 256) % 256;
        } else if (data[0] < 0) {
            _bpm = ((data[0] & 0xFF) << 8) + (data[1] & 0xFF);
        }
        heartBeatsPerMinute = _bpm;

        energyExpended |= data[2] & 0xFF;
        energyExpended |= ((data[3] & 0xFF) << 8);

        short[] rrIntervals = getRRIntervals(data, length-4);

        for (short rr: rrIntervals) {
            lastIntervalDate += rr;
        }

        if (mCallback != null) {
            mCallback.onHRDataRecieved(heartBeatsPerMinute, energyExpended, rrIntervals);
        }

    }

    private short[] getRRIntervals(byte[] data, int length) {
        final double A = 1.0; // 1000.0/1024.0
        if(length <= 0) {
            return new short[]{};
        }
        short[] result = new short[length/2];
        for(int i=4; i<length+4; i+=2) {
            result[(i-4)/2] = (short)((((data[i+1] & 0xFF) << 8) + (data[i] & 0xFF)) * A);
        }
        return result;
    }

    private void showPairedDeviceSelectDialog(String deviceAddress) {

        if (deviceAddress != null) {
            device = bluetoothAdapter.getRemoteDevice(deviceAddress);

            if (device != null && callback1 != null && hrmUUID != null) {
                if (alertDialog != null)
                    ((DialogInterface) alertDialog).cancel();
                try {
                    int status = hrmService.connectLe(device, hrmUUID, callback1);
                    if (status == BluetoothGatt.SUCCESS) {
                        //Log.d(TAG, "mPairedListClickListener.onItemClick(): connectLe sent out succesfully.");
                        setConnectionStatus(CONNECTING);
                    } else {
                        //Log.d(TAG, "mPairedListClickListener.onItemClick(): connectLe sent out but failed.");
                        setConnectionStatus(INIT);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    setConnectionStatus(DISCONNECTED);
                    leDisconnected = true;
                }
            }
            return;
        }

        OnItemClickListener mPairedListClickListener = new OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                bluetoothAdapter.cancelDiscovery();

                String info = ((TextView) v).getText().toString();
                //Log.d(TAG, "mPairedListClickListener.onItemClick(): the total length is " + info.length());
                String deviceAddress = info.substring(info.lastIndexOf("\n")+1);

                device = bluetoothAdapter.getRemoteDevice(deviceAddress);

                if (device != null && callback1 != null && hrmUUID != null) {
                    ((DialogInterface) alertDialog).cancel();
                    try {
                        int status = hrmService.connectLe(device, hrmUUID,
                                callback1);
                        if (status == BluetoothGatt.SUCCESS) {
                            //Log.d(TAG, "mPairedListClickListener.onItemClick(): connectLe sent out succesfully.");
                            setConnectionStatus(CONNECTING);
                        } else {
                            //Log.d(TAG, "mPairedListClickListener.onItemClick(): connectLe sent out but failed.");
                            setConnectionStatus(INIT);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        setConnectionStatus(DISCONNECTED);
                        leDisconnected = true;
                    }
                }

            }
        };

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout_listview = inflater.inflate(R.layout.device_list, (ViewGroup) mActivity.findViewById(R.id.root_device_list));

        alert_paired = new AlertDialog.Builder(mActivity);
        alert_paired.setView(layout_listview);

        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(mContext, R.layout.device_name);
        ListView pairedListView = (ListView) layout_listview.findViewById(R.id.paired_devices);
        mPairedDevicesArrayAdapter.clear();
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mPairedListClickListener);

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            boolean foundDevices = false;

            if (pairedDevices.size() > 0) {

                // foundDevices = false;
                for (BluetoothDevice device : pairedDevices) {
                    for (String s : IMonitors.MonitorNamesPatternLE) {
                        Pattern p_le = Pattern.compile(s);
                        if (device.getName().matches(p_le.pattern())) {
                            mPairedDevicesArrayAdapter.add(device.getName()
                                    + "\n" + device.getAddress());
                            foundDevices = true;
                        }
                    }
                }
            }

            if (foundDevices) {
                layout_listview.findViewById(R.id.title_paired_devices)
                        .setVisibility(View.VISIBLE);
            } else {
                layout_listview.findViewById(R.id.title_paired_devices)
                        .setVisibility(View.GONE);
                String noDevices = "None devices have been paired";
                mPairedDevicesArrayAdapter.add(noDevices);
            }
        } else {
            layout_listview.findViewById(R.id.title_paired_devices)
                    .setVisibility(View.GONE);
            String noDevices ="None devices have been paired";
            mPairedDevicesArrayAdapter.add(noDevices);
        }
        alert_paired.setTitle(mContext.getText(R.string.select_device_to_connect));
        alertDialog = alert_paired.show();
    }

    private void leStateChanged(int oldState, int newState) {
        if (mCallback != null) {
            mCallback.onConnectionStateChange(oldState, newState);
        }
    }

    public static String getStringSensorLocation(int intLocation) {
        String strLocation = null;
        switch (intLocation) {
            case 0:
                strLocation = "Other";
                break;
            case 1:
                strLocation = "Chest";
                break;
            case 2:
                strLocation = "Wrist";
                break;
            case 3:
                strLocation = "Finger";
                break;
            case 4:
                strLocation = "Hand";
                break;
            case 5:
                strLocation = "Ear Lobe";
                break;
            case 6:
                strLocation = "Foot";
                break;
            case 100:
                strLocation = "No Skin Contact";
            default:
                strLocation = "Unknown";
                break;
        }
        return strLocation;
    }

    public static String getStatusAsText(int leState) {
        String strStatus = "N/A";
        switch (leState) {
            case INIT:
                strStatus = "No device";
                break;
            case CONNECTED:
                strStatus = "Connected";
                break;
            case CONNECTING:
                strStatus = "Connecting...";
                break;
            case DISCONNECTING:
                strStatus = "Disconnecting...";
                break;
            case DISCONNECTED:
                strStatus = "Disconnected";
                break;
            default:
                strStatus = "N/A";
                break;
        }
        return strStatus;
    }

    public static interface Callback {

        void onConnectionStateChange(int oldState, int newState);

        void onHRDataRecieved(int heartBeatsPerMinute, int energyExpended, short[] rrIntervals);

        void onSensorLocationChange(int oldLocation, int newLocation);

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

