package com.cardiomood.android.mipt.fragments;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.mipt.R;
import com.cardiomood.android.mipt.db.HelperFactory;
import com.cardiomood.android.mipt.db.entity.CardioSessionEntity;
import com.cardiomood.android.mipt.parse.ParseTools;
import com.cardiomood.android.mipt.service.CardioDataPackage;
import com.cardiomood.android.mipt.service.CardioMonitoringService;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.parse.ParseUser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewMeasurementFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewMeasurementFragment extends Fragment {

    private static final String TAG = NewMeasurementFragment.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 2;

    // UI
    private TextView userNameView;
    private TextView userEmailView;
    private TextView hrmDeviceNameView;
    private TextView hrmStatusView;
    private TextView heartRateView;
    private Button connectButton;
    private LinearLayout chartContainer;
    private TextView emptyMessageView;
    private Button startSessionButton;
    private Button stopSessionButton;
    private TextView timeElapsedView;

    // State
    private boolean hrmConnected = false;
    private int hrmState = LeHRMonitor.INITIAL_STATUS;
    private boolean mScanning = false;
    private long mCurrentSessionId = -1L;

    // Tools
    LeHRMonitor hrMonitor;
    private Handler mHandler;

    // timer
    private Timer sessionTimer = new Timer("session_timer");
    private TimerTask updateUiTask = null;

    // Service
    /** Messenger for communicating with the service. */
    Messenger mCardioService = null;

    /** Flag indicating whether we have called bind on the service. */
    boolean mCardioServiceBound;

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //this switch reads the information in the message (usually just
                //an integer) and will do something depending on which integer is sent
                case CardioMonitoringService.RESP_STATUS:
                    mCurrentSessionId = msg.getData().getLong("sessionId", -1L);
                    onConnectionStatusChanged(-1, msg.arg1, msg.getData().getString("deviceAddress"));
                    break;
                case CardioMonitoringService.MSG_CONNECTION_STATUS_CHANGED:
                    onConnectionStatusChanged(msg.arg1, msg.arg2, msg.getData().getString("deviceAddress"));
                    break;
                case CardioMonitoringService.RESP_INIT_RESULT:
                    if (msg.arg1 == CardioMonitoringService.RESULT_FAIL) {
                        Toast.makeText(getActivity(), "Failed to initialize Bluetooth SMART.", Toast.LENGTH_SHORT).show();
                        resetHRM();
                    } else {
                        // assuming RESULT_SUCCESS
                        performConnect();
                    }
                    break;
                case CardioMonitoringService.RESP_CONNECT_RESULT:
                    if (msg.arg1 == CardioMonitoringService.RESULT_FAIL) {
                        resetHRM();
                        Toast.makeText(getActivity(), "Failed to connect.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case CardioMonitoringService.MSG_HR_DATA:
                    onDataReceived(msg);
                    break;
                case CardioMonitoringService.RESP_START_SESSION_RESULT:
                    onSessionStarted(msg);
                    break;
                case CardioMonitoringService.RESP_END_SESSION_RESULT:
                    onSessionFinished(msg);
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mCardioService = new Messenger(service);
            mCardioServiceBound = true;

            registerClient();
            requestConnectionStatus();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mCardioService = null;
            mCardioServiceBound = false;
        }
    };

    private View.OnClickListener connectButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!hrmConnected) {
                performConnect();
            } else {
                performDisconnect();
            }
        }
    };


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NewMeasurementFragment.
     */
    public static NewMeasurementFragment newInstance() {
        NewMeasurementFragment fragment = new NewMeasurementFragment();
        return fragment;
    }

    public NewMeasurementFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hrMonitor = LeHRMonitor.getMonitor(getActivity());
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_new_measurement, container, false);

        userNameView = (TextView) root.findViewById(android.R.id.text1);
        userEmailView = (TextView) root.findViewById(android.R.id.text2);

        // user info block
        ParseUser user = ParseUser.getCurrentUser();
        userEmailView.setText(user.getEmail());
        userNameView.setText(ParseTools.getUserFullName(user));

        // HRM info block
        hrmDeviceNameView = (TextView) root.findViewById(R.id.hrm_device_name);
        hrmStatusView = (TextView) root.findViewById(R.id.hr_monitor_status);
        heartRateView = (TextView) root.findViewById(R.id.heart_rate);

        connectButton = (Button) root.findViewById(R.id.connect_hr_monitor_button);
        connectButton.setOnClickListener(connectButtonListener);

        startSessionButton = (Button) root.findViewById(R.id.start_session_button);
        startSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Message msg = Message.obtain(null, CardioMonitoringService.MSG_START_SESSION);
                    msg.getData().putString("parseUserId", ParseUser.getCurrentUser().getObjectId());
                    msg.replyTo = mMessenger;
                    mCardioService.send(msg);
                } catch (RemoteException ex) {

                }
            }
        });

        stopSessionButton = (Button) root.findViewById(R.id.stop_session_button);
        stopSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentSessionId > 0L) {
                    try {
                        Message msg = Message.obtain(null, CardioMonitoringService.MSG_END_SESSION);
                        msg.replyTo = mMessenger;
                        mCardioService.send(msg);
                    } catch (RemoteException ex) {

                    }
                }
            }
        });

        timeElapsedView = (TextView) root.findViewById(R.id.time_elapsed);

        // GraphView block
        emptyMessageView = (TextView) root.findViewById(R.id.empty_message);

        chartContainer = (LinearLayout) root.findViewById(R.id.graph_container);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        // init timer
        updateUiTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentSessionId > 0L) {
                            try {
                                CardioSessionEntity session = HelperFactory.getHelper().getCardioSessionDao().queryForId(mCurrentSessionId);
                                timeElapsedView.setText(CommonTools.timeToHumanString(System.currentTimeMillis() - session.getStartTimestamp()));
                            } catch (SQLException ex) {
                                // suppress this
                            }
                        }
                    }
                });
            }
        };
        sessionTimer.scheduleAtFixedRate(updateUiTask, 0, 1000);

        // Bind to the service
        Intent cardioServiceIntent = new Intent(getActivity(), CardioMonitoringService.class);
        getActivity().startService(cardioServiceIntent);
        getActivity().bindService(cardioServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();

        // purge timer
        updateUiTask.cancel();
        updateUiTask = null;
        sessionTimer.purge();

        // Unbind from the service
        if (mCardioServiceBound) {
            unregisterClient();
            getActivity().unbindService(mConnection);
            mCardioServiceBound = false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Log.e(TAG, "onActivityResult(): BT enabled");
                    performConnect();
                } else {
                    Log.e(TAG, "onActivityResult(): BT not enabled");
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void registerClient() {
        try {
            Message msg = Message.obtain(null, CardioMonitoringService.MSG_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mCardioService.send(msg);
        } catch (RemoteException ex) {
            Log.w(TAG, "registerClient() failed", ex);
        }
    }

    private void unregisterClient() {
        try {
            Message msg = Message.obtain(null, CardioMonitoringService.MSG_UNREGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mCardioService.send(msg);
        } catch (RemoteException ex) {
            Log.w(TAG, "unregisterClient() failed", ex);
        }
    }

    private void requestConnectionStatus() {
        try {
            Message msg = Message.obtain(null, CardioMonitoringService.MSG_GET_STATUS);
            msg.replyTo = mMessenger;
            mCardioService.send(msg);
        } catch (RemoteException ex) {
            Log.w(TAG, "requestConnectionStatus() failed", ex);
        }
    }

    private void resetHRM() {
        try {
            Message msg = Message.obtain(null, CardioMonitoringService.MSG_RESET);
            mCardioService.send(msg);
        } catch (RemoteException ex) {
            Log.w(TAG, "resetHRM() failed", ex);
        }
    }

    private void performConnect() {
        if (!requestEnableBluetooth()) {
            // this will show dialog to enable bluetooth
            return;
        }

        if (hrmState == LeHRMonitor.INITIAL_STATUS || hrmState == LeHRMonitor.DISCONNECTING_STATUS) {
            // call init first!
            Message msg = Message.obtain(null, CardioMonitoringService.MSG_INIT);
            msg.replyTo = mMessenger;
            try {
                mCardioService.send(msg);
            } catch (RemoteException ex) {
                Log.w(TAG, "performConnect() failed to send MSG_INIT", ex);
            }
            return;
        }

        if (hrmState == LeHRMonitor.READY_STATUS) {
            showConnectionDialog();
        } else {
            throw new IllegalStateException("Incorrect monitor state!");
        }
    }

    private void performDisconnect() {
        Message msg = Message.obtain(null, CardioMonitoringService.MSG_DISCONNECT);
        try {
            mCardioService.send(msg);
        } catch (RemoteException ex) {

        }
    }

    AlertDialog alertSelectDevice = null;

    @SuppressWarnings("NewApi")
    private void showConnectionDialog() {
        Log.d(TAG, "showConnectionDialog()");
        final BluetoothAdapter bluetoothAdapter = hrMonitor.getBluetoothAdapter();

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

                connectDeviceAddress(deviceAddress);
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
            }, 10000);

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

    private void connectDeviceAddress(String deviceAddress) {
        if (hrmState == LeHRMonitor.READY_STATUS) {
            try {
                Message msg = Message.obtain(null, CardioMonitoringService.MSG_CONNECT, 0, 0);
                msg.replyTo = mMessenger;
                msg.getData().putString("deviceAddress", deviceAddress);
                mCardioService.send(msg);
            } catch (RemoteException ex) {
                Log.w(TAG, "connectDeviceAddress() failed to send MSG_CONNECT", ex);
            }
        } else {
            throw new IllegalStateException("Incorrect monitor state!");
        }
    }

    private boolean requestEnableBluetooth() {
        if (hrMonitor == null)
            return false;
        BluetoothAdapter bluetoothAdapter = hrMonitor.getBluetoothAdapter();
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

    private void onDataReceived(Message msg) {
        msg.getData().setClassLoader(CardioDataPackage.class.getClassLoader());
        CardioDataPackage data = msg.getData().getParcelable("data");
        heartRateView.setText(String.valueOf(data.getBpm()));

        if (data.getBpm() == 0) {
            CommonTools.vibrate(getActivity(), 1000);
        }
    }

    private void onSessionStarted(Message msg) {
        if (msg.arg1 == CardioMonitoringService.RESULT_SUCCESS) {
            msg.getData().setClassLoader(long.class.getClassLoader());
            mCurrentSessionId = msg.getData().getLong("sessionId", -1L);
            startSessionButton.setEnabled(false);
            startSessionButton.setVisibility(View.GONE);
            stopSessionButton.setEnabled(true);
            stopSessionButton.setVisibility(View.VISIBLE);
        }
    }

    private void onSessionFinished(Message msg) {
        mCurrentSessionId = -1L;
        startSessionButton.setEnabled(false);
        startSessionButton.setVisibility(View.VISIBLE);
        stopSessionButton.setEnabled(false);
        stopSessionButton.setVisibility(View.GONE);

    }

    private void onConnectionStatusChanged(int oldStatus, int newStatus, String deviceAddress) {
        hrmState = newStatus;
        if (getActivity() == null)
            return;

        if (newStatus != LeHRMonitor.CONNECTED_STATUS) {
            // disconnected
            if (mCurrentSessionId > 0L)
                onSessionFinished(null);
        }

        if (oldStatus == LeHRMonitor.CONNECTING_STATUS &&
                (newStatus == LeHRMonitor.READY_STATUS || newStatus == LeHRMonitor.DISCONNECTING_STATUS)) {
            Toast.makeText(getActivity(), "Connection failed.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Status changed from " + oldStatus + " to " + newStatus +"!", Toast.LENGTH_SHORT).show();
        }

        if (newStatus == LeHRMonitor.CONNECTED_STATUS) {
            hrmStatusView.setText(deviceAddress);
            hrmDeviceNameView.setText("Device connected");
            connectButton.setEnabled(true);
            connectButton.setText(R.string.button_disconnect);
            connectButton.setVisibility(View.VISIBLE);
            hrmConnected = true;
            heartRateView.setVisibility(View.VISIBLE);
            if (mCurrentSessionId <= 0) {
                startSessionButton.setVisibility(View.VISIBLE);
                startSessionButton.setEnabled(true);
                stopSessionButton.setVisibility(View.GONE);
            } else {
                startSessionButton.setVisibility(View.GONE);
                startSessionButton.setEnabled(false);
                stopSessionButton.setVisibility(View.VISIBLE);
                stopSessionButton.setEnabled(true);
            }
            return;
        }
        if (newStatus == LeHRMonitor.CONNECTING_STATUS) {
            hrmStatusView.setText(R.string.hrm_connecting);
            hrmDeviceNameView.setText("Please, wait.");
            connectButton.setEnabled(false);
            connectButton.setText(R.string.connect_button);
            connectButton.setVisibility(View.VISIBLE);
            hrmConnected = false;
            heartRateView.setVisibility(View.GONE);
            startSessionButton.setVisibility(View.VISIBLE);
            startSessionButton.setEnabled(false);
            stopSessionButton.setVisibility(View.GONE);
            return;
        }
        if (newStatus == LeHRMonitor.DISCONNECTING_STATUS) {
            hrmStatusView.setText(R.string.hrm_not_connected);
            hrmDeviceNameView.setText(R.string.select_hrm_device);
            connectButton.setText(R.string.connect_button);
            connectButton.setEnabled(true);
            connectButton.setVisibility(View.VISIBLE);
            hrmConnected = false;
            heartRateView.setVisibility(View.GONE);
            startSessionButton.setVisibility(View.VISIBLE);
            startSessionButton.setEnabled(false);
            stopSessionButton.setVisibility(View.GONE);
            return;
        }
        if (newStatus == LeHRMonitor.READY_STATUS) {
            hrmStatusView.setText(R.string.hrm_ready);
            hrmDeviceNameView.setText(R.string.select_hrm_device);
            connectButton.setText(R.string.connect_button);
            connectButton.setEnabled(true);
            connectButton.setVisibility(View.VISIBLE);
            hrmConnected = false;
            heartRateView.setVisibility(View.GONE);
            startSessionButton.setVisibility(View.VISIBLE);
            startSessionButton.setEnabled(false);
            stopSessionButton.setVisibility(View.GONE);
            return;
        }

        hrmStatusView.setText(R.string.hrm_not_connected);
        hrmDeviceNameView.setText(R.string.select_hrm_device);
        connectButton.setText(R.string.connect_button);
        connectButton.setEnabled(true);
        connectButton.setVisibility(View.VISIBLE);
        hrmConnected = false;
        heartRateView.setVisibility(View.GONE);
        startSessionButton.setVisibility(View.VISIBLE);
        startSessionButton.setEnabled(false);
        stopSessionButton.setVisibility(View.GONE);
    }
}
