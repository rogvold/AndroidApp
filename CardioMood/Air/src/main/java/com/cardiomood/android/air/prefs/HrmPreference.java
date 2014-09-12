package com.cardiomood.android.air.prefs;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Handler;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.R;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by antondanhsin on 10/09/14.
 */
public class HrmPreference extends DialogPreference {

    private static final String TAG = HrmPreference.class.getSimpleName();

    private static final long SCAN_PERIOD = 10000;

    private Context context;
    private LeHRMonitor hrMonitor;
    private Handler mHandler;
    private AlertDialog alertSelectDevice;
    private boolean mScanning = false;

    public HrmPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);

        this.context = context;
        this.hrMonitor = LeHRMonitor.getMonitor(context);
        mHandler = new Handler();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        hrMonitor.initialize();

        final BluetoothAdapter bluetoothAdapter = hrMonitor.getBluetoothAdapter();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout_listview = inflater.inflate(R.layout.device_list, null);
        builder.setView(layout_listview);

        final ArrayList<String> discoveredDeviceNames = new ArrayList<String>();
        final ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<String>(context, R.layout.device_name, discoveredDeviceNames);
        final ListView pairedListView = (ListView) layout_listview.findViewById(R.id.paired_devices);
        mPairedDevicesArrayAdapter.clear();
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        final Set<BluetoothDevice> discoveredDevices = new HashSet<BluetoothDevice>();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        discoveredDevices.addAll(pairedDevices);
        boolean foundDevices = false;
        if (pairedDevices.size() > 0) {

            if (pairedDevices.size() > 0) {

                // foundDevices = false;
                for (BluetoothDevice device : pairedDevices) {
//                    for (String s : IMonitors.MonitorNamesPatternLE) {
//                        Pattern p_le = Pattern.compile(s);
//                        if (device.getName().matches(p_le.pattern())) {
                    mPairedDevicesArrayAdapter.add(device.getName()  + "\n" + device.getAddress());
                    foundDevices = true;
//                        }
//                    }
                }
            }

            if (foundDevices) {
                layout_listview.findViewById(R.id.title_paired_devices)
                        .setVisibility(View.VISIBLE);
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    layout_listview.findViewById(R.id.title_paired_devices)
                            .setVisibility(View.GONE);
                    String noDevices = "None devices have been paired";
                    mPairedDevicesArrayAdapter.add(noDevices);
                }
            }
        }

        builder.setTitle(R.string.title_select_hrm);
        final Object leScanCallback = (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
                ? null : new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                Log.i(TAG, "onLeScan(): bluetoothDevice.address="+bluetoothDevice.getAddress());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        String deviceString = bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress();
                        for (BluetoothDevice device : discoveredDevices) {
                            if (device.getAddress().equals(bluetoothDevice.getAddress()))
                                return;
                        }
                        discoveredDevices.add(bluetoothDevice);
                        mPairedDevicesArrayAdapter.add(deviceString);
                        mPairedDevicesArrayAdapter.notifyDataSetChanged();
                    }
                });

            }
        };
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            alertSelectDevice.setOnShowListener(new DialogInterface.OnShowListener() {
//                @Override
//                public void onShow(DialogInterface dialogInterface) {
//                    scanLeDevice(bluetoothAdapter, true, (BluetoothAdapter.LeScanCallback) leScanCallback);
//                }
//            });
//        }
//        alertSelectDevice.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialogInterface) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                    scanLeDevice(bluetoothAdapter, false, (BluetoothAdapter.LeScanCallback) leScanCallback);
//                }
//                hrMonitor.close();
//                hrMonitor = LeHRMonitor.getMonitor(context);
//            }
//        });
//        AdapterView.OnItemClickListener mPairedListClickListener = new AdapterView.OnItemClickListener() {
//            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
//                bluetoothAdapter.cancelDiscovery();
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                    scanLeDevice(bluetoothAdapter, false, (BluetoothAdapter.LeScanCallback) leScanCallback);
//                }
//
//                String info = ((TextView) v).getText().toString();
//                //Log.d(TAG, "mPairedListClickListener.onItemClick(): the total length is " + info.length());
//                String deviceAddress = info.substring(info.lastIndexOf("\n")+1);
//
//                try {
//
//                } catch (Exception ex) {
//                    Log.e(TAG, "mBluetoothLeService.connect() failed to connect to the device '" + deviceAddress+"'", ex);
//                }
//                alertSelectDevice.dismiss();
//                alertSelectDevice = null;
//            }
//        };
//        pairedListView.setOnItemClickListener(mPairedListClickListener);
        //alertSelectDevice.show();
            }

    public View performConnect() {
        try {
            try {
                // check bluetooth enabled
                BluetoothAdapter adapter = hrMonitor.getBluetoothAdapter();
                if (adapter == null || !adapter.isEnabled()) {
                    Toast.makeText(context, R.string.bluetooth_is_not_available, Toast.LENGTH_SHORT).show();
                }

                // call to initialize service
                if (!hrMonitor.initialize()) {
                    Toast.makeText(context, "Failed to initialize service. Make sure your device is supported.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    return null;
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed to initialize service.", ex);
                Toast.makeText(context, "Cannot start Bluetooth.", Toast.LENGTH_SHORT);
                return null;
            }
            return createConnectionDialog();
        } catch (Exception ex) {
            Log.d(TAG, "performConnect(): "+ex);
            ex.printStackTrace();
            return null;
        }
    }


    @SuppressWarnings("NewApi")
    private View createConnectionDialog() {
        Log.d(TAG, "showConnectionDialog()");
        final BluetoothAdapter bluetoothAdapter = hrMonitor.getBluetoothAdapter();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout_listview = inflater.inflate(R.layout.device_list, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setView(layout_listview);

        final ArrayList<String> discoveredDeviceNames = new ArrayList<String>();
        final ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<String>(context, R.layout.device_name, discoveredDeviceNames);
        final ListView pairedListView = (ListView) layout_listview.findViewById(R.id.paired_devices);
        mPairedDevicesArrayAdapter.clear();
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        final Set<BluetoothDevice> discoveredDevices = new HashSet<BluetoothDevice>();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        discoveredDevices.addAll(pairedDevices);
        boolean foundDevices = false;
        if (pairedDevices.size() > 0) {

            if (pairedDevices.size() > 0) {

                // foundDevices = false;
                for (BluetoothDevice device : pairedDevices) {
//                    for (String s : IMonitors.MonitorNamesPatternLE) {
//                        Pattern p_le = Pattern.compile(s);
//                        if (device.getName().matches(p_le.pattern())) {
                    mPairedDevicesArrayAdapter.add(device.getName()  + "\n" + device.getAddress());
                    foundDevices = true;
//                        }
//                    }
                }
            }

            if (foundDevices) {
                layout_listview.findViewById(R.id.title_paired_devices)
                        .setVisibility(View.VISIBLE);
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    layout_listview.findViewById(R.id.title_paired_devices)
                            .setVisibility(View.GONE);
                    String noDevices = "None devices have been paired";
                    mPairedDevicesArrayAdapter.add(noDevices);
                }
            }
        }

        dialogBuilder.setTitle(R.string.title_select_hrm);
        alertSelectDevice = dialogBuilder.create();
        final Object leScanCallback = (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
                ? null : new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                Log.i(TAG, "onLeScan(): bluetoothDevice.address="+bluetoothDevice.getAddress());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        String deviceString = bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress();
                        for (BluetoothDevice device : discoveredDevices) {
                            if (device.getAddress().equals(bluetoothDevice.getAddress()))
                                return;
                        }
                        discoveredDevices.add(bluetoothDevice);
                        mPairedDevicesArrayAdapter.add(deviceString);
                        mPairedDevicesArrayAdapter.notifyDataSetChanged();
                    }
                });

            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            alertSelectDevice.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    scanLeDevice(bluetoothAdapter, true, (BluetoothAdapter.LeScanCallback) leScanCallback);
                }
            });
        }
        alertSelectDevice.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    scanLeDevice(bluetoothAdapter, false, (BluetoothAdapter.LeScanCallback) leScanCallback);
                }
                hrMonitor.close();
                hrMonitor = LeHRMonitor.getMonitor(context);
            }
        });
        AdapterView.OnItemClickListener mPairedListClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                bluetoothAdapter.cancelDiscovery();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    scanLeDevice(bluetoothAdapter, false, (BluetoothAdapter.LeScanCallback) leScanCallback);
                }

                String info = ((TextView) v).getText().toString();
                //Log.d(TAG, "mPairedListClickListener.onItemClick(): the total length is " + info.length());
                String deviceAddress = info.substring(info.lastIndexOf("\n")+1);

                try {

                } catch (Exception ex) {
                    Log.e(TAG, "mBluetoothLeService.connect() failed to connect to the device '" + deviceAddress+"'", ex);
                }
                alertSelectDevice.dismiss();
                alertSelectDevice = null;
            }
        };
        pairedListView.setOnItemClickListener(mPairedListClickListener);
        //alertSelectDevice.show();
        return layout_listview;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void scanLeDevice(final BluetoothAdapter bluetoothAdapter, final boolean enable, final BluetoothAdapter.LeScanCallback leScanCallback) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanning) {
                        mScanning = false;
                        bluetoothAdapter.stopLeScan(leScanCallback);
                        Log.d(TAG, "LeScan stopped.");
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            //bluetoothAdapter.startLeScan(new UUID[]{UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")}, leScanCallback);
            bluetoothAdapter.startLeScan(leScanCallback);
            Log.d(TAG, "LeScan started.");
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
            Log.d(TAG, "LeScan stopped.");
        }
    }

}
