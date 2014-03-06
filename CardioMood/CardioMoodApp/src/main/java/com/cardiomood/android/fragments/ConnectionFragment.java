package com.cardiomood.android.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.MonitoringActivity;
import com.cardiomood.android.R;
import com.cardiomood.android.heartrate.AbstractDataCollector;
import com.cardiomood.android.heartrate.CardioMoodHeartRateLeService;
import com.cardiomood.android.heartrate.IntervalLimitDataCollector;
import com.cardiomood.android.tools.IMonitors;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.flurry.android.FlurryAgent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Project: CardioMood
 * User: danon
 * Date: 23.05.13
 * Time: 23:50
 */
public class ConnectionFragment extends Fragment {

   private static final String TAG = ConnectionFragment.class.getSimpleName();
    // Bluetooth Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;

    private View container;
    private TextView hintText;
    private Button connectDeviceButton;

    private PreferenceHelper mPrefHelper;
    private Handler mHandler;
    private boolean mScanning = false;

    private AlertDialog alertSelectDevice;

    private ProgressDialog sessionSavingDialog;
    public static boolean isMonitoring = false;
    private boolean disableBluetoothOnClose = false;

    // Service registration
    private boolean receiverRegistered = false;
    private boolean serviceBound = false;
    private boolean deviceConnected = false;

    private CardioMoodHeartRateLeService mBluetoothLeService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = (CardioMoodHeartRateLeService) ((CardioMoodHeartRateLeService.LocalBinder) service).getService();
            LeHRMonitor monitor = mBluetoothLeService.getMonitor();
            deviceConnected = (monitor.getConnectionStatus() == LeHRMonitor.CONNECTED_STATUS);
            updateView();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED.equals(action)) {
                final int newStatus = intent.getIntExtra(LeHRMonitor.EXTRA_NEW_STATUS, -100);
                final int oldStatus = intent.getIntExtra(LeHRMonitor.EXTRA_OLD_STATUS, -100);

                if (getActivity() != null) {
                    getActivity().invalidateOptionsMenu();
                }

                // update button ui state
                connectDeviceButton.setEnabled(newStatus == LeHRMonitor.CONNECTED_STATUS || newStatus == LeHRMonitor.INITIAL_STATUS);
                deviceConnected = newStatus == LeHRMonitor.CONNECTED_STATUS;

                if (newStatus == LeHRMonitor.CONNECTED_STATUS) {
                    FlurryAgent.logEvent("device_connected");
                    openMonitor();
                }
                if (newStatus == LeHRMonitor.DISCONNECTING_STATUS || newStatus == LeHRMonitor.READY_STATUS && oldStatus != LeHRMonitor.INITIAL_STATUS) {
                    FlurryAgent.logEvent("device_disconnected");
                    setDisconnectedView();
                }
                if (newStatus == LeHRMonitor.INITIAL_STATUS) {
                    setDisconnectedView();
                }
            }

            updateView();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        if (! serviceBound) {
            Intent gattServiceIntent = new Intent(getActivity(), CardioMoodHeartRateLeService.class);
            getActivity().bindService(gattServiceIntent, mServiceConnection, Activity.BIND_AUTO_CREATE);
            serviceBound = true;
        }

        setHasOptionsMenu(true);

        mPrefHelper = new PreferenceHelper(getActivity().getApplicationContext());
        mPrefHelper.setPersistent(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!receiverRegistered) {
            getActivity().registerReceiver(dataReceiver, makeGattUpdateIntentFilter());
            receiverRegistered = true;
        }
        //unlimitedLength = mPrefHelper.getBoolean(ConfigurationConstants.MEASUREMENT_UNLIMITED_LENGTH);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (receiverRegistered) {
            receiverRegistered = false;
            getActivity().unregisterReceiver(dataReceiver);
        }
    }

    private void vibrate(long milliseconds) {
        Activity activity = getActivity();
        if (activity == null)
                return;
        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (v!= null && v.hasVibrator()) {
            v.vibrate(milliseconds);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        this.container = inflater.inflate(R.layout.fragment_monitor, container, false);
        connectDeviceButton = (Button) this.container.findViewById(R.id.btn_connect_device);

        connectDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("connect_device_click");
                performConnect();
            }
        });

        hintText = (TextView) this.container.findViewById(R.id.hintText);

        setDisconnectedView();
        return this.container;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                FlurryAgent.logEvent("stop_button_clicked");
                performDisconnect();
                break;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Log.e(TAG, "onActivityResult(): BT enabled");
                    disableBluetoothOnClose = mPrefHelper.getBoolean(ConfigurationConstants.CONNECTION_DISABLE_BT_ON_CLOSE);
                    performConnect();
                } else {
                    Log.e(TAG, "onActivityResult(): BT not enabled");
                }
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        performDisconnect();

        if (serviceBound) {
            serviceBound = false;
            getActivity().unbindService(mServiceConnection);
        }

        if (disableBluetoothOnClose) {
            LeHRMonitor monitor = mBluetoothLeService.getMonitor();
            if (monitor != null) {
                BluetoothAdapter bluetoothAdapter = monitor.getBluetoothAdapter();
                if (bluetoothAdapter!= null && bluetoothAdapter.isEnabled())
                    bluetoothAdapter.disable();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG, "onCreateOptionsMenu()");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_connection, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem disconnectItem = menu.findItem(R.id.menu_disconnect);
        MenuItem bpmItem = menu.findItem(R.id.menu_bpm);


        if (deviceConnected && serviceBound) {
            LeHRMonitor monitor = mBluetoothLeService.getMonitor();

            bpmItem.setVisible(true);
            bpmItem.setTitle(String.valueOf(monitor.getLastBPM()));

            disconnectItem.setVisible(true);
        } else {
            bpmItem.setVisible(false);
            disconnectItem.setVisible(false);
        }

        super.onPrepareOptionsMenu(menu);
    }

    private void openMonitor() {
        startActivity(new Intent(getActivity(), MonitoringActivity.class));
    }

    private void setDisconnectedView() {
        connectDeviceButton.setEnabled(true);

        isMonitoring = false;
    }

    public boolean requestEnableBluetooth() {
        LeHRMonitor monitor = mBluetoothLeService.getMonitor();
        if (monitor == null)
            return false;
        BluetoothAdapter bluetoothAdapter = monitor.getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getActivity(), "Bluetooth adapter is not available.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            return false;
        } return true;
    }

    public void performConnect() {
        try {
            try {
                if (!requestEnableBluetooth())
                    return;

                // Check if already initialized
                LeHRMonitor monitor = mBluetoothLeService.getMonitor();

                if (monitor != null && monitor.getConnectionStatus() == LeHRMonitor.CONNECTED_STATUS) {
                    openMonitor();
                    return;
                }

                if (monitor != null && monitor.getConnectionStatus() != LeHRMonitor.INITIAL_STATUS) {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                }

                if (!mBluetoothLeService.initialize(getActivity())) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    return;
                }

                mBluetoothLeService.setDataCollector(createDataCollector());
            } catch (Exception ex) {
                Log.e(TAG, "Failed to initialize service.", ex);
                Toast.makeText(getActivity(), "Cannot start Bluetooth.", Toast.LENGTH_SHORT);
                return;
            }
            showConnectionDialog();
        } catch (Exception ex) {
            Log.d(TAG, "performConnect(): "+ex);
            ex.printStackTrace();
        }
    }

    public void performDisconnect() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        }
    }

    private AbstractDataCollector createDataCollector() {
        return new IntervalLimitDataCollector(mBluetoothLeService, 100);
    }

    private void updateView() {
        if (serviceBound && deviceConnected) {
            connectDeviceButton.setText(getString(R.string.open_monitor));
            hintText.setText(R.string.device_connected_open_monitor);
        } else {
            connectDeviceButton.setText(getString(R.string.connect_device));
            hintText.setText(R.string.pair_and_tap_connect);
        }
        getActivity().invalidateOptionsMenu();
    }

    @SuppressWarnings("NewApi")
    private void showConnectionDialog() {
        Log.d(TAG, "showConnectionDialog()");
        final BluetoothAdapter bluetoothAdapter = mBluetoothLeService.getMonitor().getCurrentBluetoothAdapter();

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout_listview = inflater.inflate(R.layout.device_list, (ViewGroup) getActivity().findViewById(R.id.root_device_list));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(layout_listview);

        final ArrayList<String> discoveredDeviceNames = new ArrayList<String>();
        final ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.device_name, discoveredDeviceNames);
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
                    for (String s : IMonitors.MonitorNamesPatternLE) {
                        Pattern p_le = Pattern.compile(s);
                        if (device.getName().matches(p_le.pattern())) {
                            mPairedDevicesArrayAdapter.add(device.getName()  + "\n" + device.getAddress());
                            foundDevices = true;
                        }
                    }
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

        dialogBuilder.setTitle(getText(R.string.select_device_to_connect));
        alertSelectDevice = dialogBuilder.create();
        final Object leScanCallback = (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
                ? null : new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                Log.i(TAG, "onLeScan(): bluetoothDevice.address="+bluetoothDevice.getAddress());
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String deviceString = bluetoothDevice.getName() + "\n" + bluetoothDevice.getAddress();
                        for (BluetoothDevice device: discoveredDevices) {
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
                if (mBluetoothLeService == null)
                    return;
                LeHRMonitor monitor = mBluetoothLeService.getMonitor();
                if (monitor == null)
                    return;
                int status = monitor.getConnectionStatus();
                if (status == LeHRMonitor.READY_STATUS || status == LeHRMonitor.INITIAL_STATUS) {
                    mBluetoothLeService.close();
                    setDisconnectedView();
                }
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
                    mBluetoothLeService.connect(deviceAddress);
                } catch (Exception ex) {
                    Log.e(TAG, "mBluetoothLeService.connect() failed to connect to the device '" + deviceAddress+"'", ex);
                }
                alertSelectDevice.dismiss();
                alertSelectDevice = null;
            }
        };
        pairedListView.setOnItemClickListener(mPairedListClickListener);
        alertSelectDevice.show();
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
            bluetoothAdapter.startLeScan(new UUID[]{UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")}, leScanCallback);
            Log.d(TAG, "LeScan started.");
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
            Log.d(TAG, "LeScan stopped.");
        }
    }


    private void showSavingSessionDialog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sessionSavingDialog = ProgressDialog.show(getActivity(), getString(R.string.please_wait), getString(R.string.saving_data), true, false);
            }
        });
    }

    private void removeSavingSessionDialog() {
        if (sessionSavingDialog != null) {
            sessionSavingDialog.dismiss();
            sessionSavingDialog = null;
        }
    }

//    private void saveAndOpenSessionView() {
//        final List<HeartRateDataItem> data = new ArrayList<HeartRateDataItem>(collectedData);
//        new AsyncTask<Void, Void, Long>() {
//            @Override
//            protected void onPreExecute() {
//                showSavingSessionDialog();
//            }
//
//            @Override
//            protected Long doInBackground(Void... params) {
//                if (data.isEmpty())
//                    return null;
//                HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO();
//                HeartRateSession session = new HeartRateSession();
//                session.setDateStarted(data.get(0).getTimeStamp());
//                session.setDateEnded(new Date());
//                session.setStatus(SessionStatus.COMPLETED);
//                session = sessionDAO.insert(session, data);
//                Long sessionId = session.getId();
//
//                Map<String, String> args = new HashMap<String, String>();
//                args.put("sessionId", sessionId+"");
//                args.put("totalSessions", sessionDAO.getCount()+"");
//                FlurryAgent.logEvent("session_saved", args);
//
//                return sessionId;
//            }
//
//            @Override
//            protected void onPostExecute(Long sessionId) {
//                if (sessionId == null) {
//                    return;
//                }
//                Intent intent = new Intent(getActivity(), SessionDetailsActivity.class);
//                intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, sessionId);
//                intent.putExtra(SessionDetailsActivity.POST_RENDER_ACTION_EXTRA, SessionDetailsActivity.RENAME_ACTION);
//                startActivity(intent);
//
//                removeSavingSessionDialog();
//            }
//        }.execute();
//    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED);
        intentFilter.addAction(LeHRMonitor.ACTION_BPM_CHANGED);
        intentFilter.addAction(LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED);
        return intentFilter;
    }

}
