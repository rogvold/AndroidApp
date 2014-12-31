package com.cardiomood.android.fragments;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.SessionDetailsActivity;
import com.cardiomood.android.controls.gauge.BatteryIndicatorGauge;
import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.db.entity.CardioItemDAO;
import com.cardiomood.android.db.entity.SessionDAO;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.dialogs.MeasurementDurationDialog;
import com.cardiomood.android.dialogs.MeasurementInfoDialog;
import com.cardiomood.android.expert.R;
import com.cardiomood.android.service.CardioDataPackage;
import com.cardiomood.android.service.CardioMonitoringService;
import com.cardiomood.android.sync.parse.ParseTools;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.tools.ui.TouchEffect;
import com.cardiomood.android.ui.HeartRateGraphView;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.j256.ormlite.dao.GenericRawResults;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.parse.ParseUser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class NewMeasurementFragment extends Fragment {

    private static final String TAG = NewMeasurementFragment.class.getSimpleName();

    private static final int MAX_GRAPH_VIEW_POINTS = 500;

    public static final int REQUEST_ENABLE_BT = 2;

    public static boolean inProgress = true;

    // UI
    @InjectView(android.R.id.text1)
    protected TextView userNameView;
    @InjectView(android.R.id.text2)
    protected TextView userEmailView;
    @InjectView(R.id.hrm_device_name)
    protected TextView hrmDeviceNameView;
    @InjectView(R.id.hr_monitor_status)
    protected TextView hrmStatusView;
    @InjectView(R.id.heart_rate)
    protected TextView heartRateView;
    @InjectView(R.id.connect_hr_monitor_button)
    protected Button connectButton;
    @InjectView(R.id.graph_container)
    protected LinearLayout chartContainer;
    @InjectView(R.id.empty_message)
    protected TextView emptyMessageView;
    @InjectView(R.id.start_session_button)
    protected Button startSessionButton;
    @InjectView(R.id.stop_session_button)
    protected Button stopSessionButton;
    @InjectView(R.id.time_elapsed)
    protected TextView timeElapsedView;
    @InjectView(R.id.hrm_battery)
    protected BatteryIndicatorGauge hrmBattery;
    @InjectView(R.id.duration_settings_button)
    protected ImageButton durationSettingsButton;
    @InjectView(R.id.measurement_params_button)
    protected ImageButton measurementParamsButton;
    @InjectView(R.id.measurement_progress)
    protected ProgressBar measurementProgressBar;

    //chart is added manually
    protected GraphView mGraphView;

    // State
    protected boolean hrmConnected = false;
    protected int hrmState = LeHRMonitor.INITIAL_STATUS;
    protected boolean mScanning = false;
    protected long mCurrentSessionId = -1L;
    protected SessionEntity mCurrentSession = null;
    protected GraphViewSeries mHeartRateSeries = null;
    protected long graphT = 0;
    protected boolean sessionDataLoaded = false;
    protected boolean disableBluetoothOnClose = false;

    // Tools
    protected LeHRMonitor hrMonitor;
    protected Handler mHandler;
    protected PreferenceHelper prefHelper;

    // timer
    protected Timer sessionTimer = new Timer("session_timer");
    protected TimerTask updateUiTask = null;

    // Service
    /** Messenger for communicating with the service. */
    Messenger mCardioService = null;

    /** Flag indicating whether we have called bind on the service. */
    protected boolean mCardioServiceBound;

    protected class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //this switch reads the information in the message (usually just
                //an integer) and will do something depending on which integer is sent
                case CardioMonitoringService.RESP_STATUS:
                    long sessionId = msg.getData().getLong("sessionId", -1L);
                    onConnectionStatusChanged(-1, msg.arg1, msg.getData().getString("deviceAddress"));
                    if (sessionId > 0L) {
                        mCurrentSessionId = sessionId;
                        inProgress = true;
                        msg.what = CardioMonitoringService.RESP_START_SESSION_RESULT;
                        msg.arg1 = CardioMonitoringService.RESULT_SUCCESS;
                        onSessionStarted(msg);
                    } else {
                        if (mCurrentSessionId > 0) {
                            msg.what = CardioMonitoringService.RESP_END_SESSION_RESULT;
                            msg.arg1 = CardioMonitoringService.RESULT_SUCCESS;
                            onSessionFinished(msg);
                        }
                        inProgress = false;
                    }
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
                    inProgress = true;
                    onSessionStarted(msg);
                    break;
                case CardioMonitoringService.RESP_END_SESSION_RESULT:
                    onSessionFinished(msg);
                    inProgress = false;
                    break;
                case CardioMonitoringService.MSG_RECONNECTING:
                    onReconnecting(msg);
                    break;
                case CardioMonitoringService.MSG_BATTERY_LEVEL:
                    int level = msg.arg1;
                    onBatteryLevelReceived(level);
                    break;
                case CardioMonitoringService.MSG_PROGRESS:
                    if (msg.arg1 != 0) {
                        measurementProgressBar.setVisibility(View.VISIBLE);
                        double progress = msg.getData().getDouble("progress", 0.0d);
                        measurementProgressBar.setProgress((int) Math.round(progress));
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    protected Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    protected ServiceConnection mConnection = new ServiceConnection() {
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

    protected View.OnClickListener connectButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!hrmConnected) {
                performConnect();
            } else {
                if (mCurrentSession != null) {
                    long duration = System.currentTimeMillis() - mCurrentSession.getStartTimestamp();
                    if (duration <= 60*1000) {
                        Toast.makeText(
                                getActivity(),
                                "You should record for at least 1 minute.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }
                }
                performDisconnect();
            }
            connectButton.setEnabled(false);

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
        prefHelper = new PreferenceHelper(getActivity(), true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.fragment_new_measurement, container, false);
        ButterKnife.inject(this, root);

        hrmBattery.setOrientation(BatteryIndicatorGauge.VERTICAL);
        hrmBattery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Sensor battery level: " + hrmBattery.getValue() + "%",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // user info block
        ParseUser user = ParseUser.getCurrentUser();
        userEmailView.setText(user.getEmail());
        userNameView.setText(ParseTools.getUserFullName(user));

        connectButton.setOnClickListener(connectButtonListener);
        startSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSession();
            }
        });

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

        durationSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MeasurementDurationDialog()
                        .show(getChildFragmentManager(), "session_duration");
            }
        });
        durationSettingsButton.setOnTouchListener(TouchEffect.FADE_ON_TOUCH);

        measurementParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MeasurementInfoDialog dlg = null;

                if (mCurrentSession != null) {
                    try {
                        DatabaseHelperFactory
                                .getHelper().getSessionDao().refresh(mCurrentSession);
                    } catch (SQLException ex) {
                        // suppress this
                    }
                    dlg = MeasurementInfoDialog.newInstance(
                            mCurrentSession.getName(),
                            mCurrentSession.getDescription()
                    );
                } else {
                    dlg = MeasurementInfoDialog.newInstance(
                            prefHelper.getString(ConfigurationConstants.MEASUREMENT_NAME),
                            prefHelper.getString(ConfigurationConstants.MEASUREMENT_DESCRIPTION)
                    );
                }
                dlg.setCallback(new MeasurementInfoDialog.Callback() {
                    @Override
                    public void onInfoUpdated(String name, String description) {
                        // send rename command to the service
                        if (mCardioServiceBound) {
                            requestUpdateInfo(name, description);
                        }
                    }
                });
                dlg.show(getChildFragmentManager(), "session_params");
            }
        });
        measurementParamsButton.setOnTouchListener(TouchEffect.FADE_ON_TOUCH);

        // Init Graph View
        mGraphView = new HeartRateGraphView(getActivity());
        mGraphView.setVisibility(View.GONE);

        mHeartRateSeries = new GraphViewSeries(new GraphView.GraphViewData[0]);
        mHeartRateSeries.getStyle().color = getResources().getColor(R.color.colorAccent);
        mGraphView.addSeries(mHeartRateSeries);

        chartContainer.addView(mGraphView);

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
                            Task.callInBackground(new Callable<SessionEntity>() {
                                @Override
                                public SessionEntity call() throws Exception {
                                    return DatabaseHelperFactory.getHelper()
                                            .getSessionDao()
                                            .queryForId(mCurrentSessionId);
                                }
                            }).onSuccess(new Continuation<SessionEntity, Object>() {
                                @Override
                                public Object then(Task<SessionEntity> task) throws Exception {
                                    SessionEntity session = task.getResult();
                                    if (session != null && mCurrentSession != null) {
                                        timeElapsedView.setText(
                                                CommonTools.timeToHumanString(
                                                        System.currentTimeMillis()
                                                                - session.getStartTimestamp()
                                                )
                                        );
                                        mCurrentSession = session;
                                    }
                                    return null;
                                }
                            }, Task.UI_THREAD_EXECUTOR);
                        } else {
                            timeElapsedView.setText(CommonTools.timeToHumanString(0));
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

        getView().postDelayed(new Runnable() {
            @Override
            public void run() {
                CommonTools.hideSoftInputKeyboard(getActivity());
            }
        }, 200);
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
    public void onDestroy() {
        super.onDestroy();
        if (disableBluetoothOnClose) {
            if (hrMonitor != null) {
                BluetoothAdapter bluetoothAdapter = hrMonitor.getBluetoothAdapter();
                if (bluetoothAdapter!= null && bluetoothAdapter.isEnabled())
                    bluetoothAdapter.disable();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Log.e(TAG, "onActivityResult(): BT enabled");
                    disableBluetoothOnClose = prefHelper.getBoolean(ConfigurationConstants.CONNECTION_DISABLE_BT_ON_CLOSE);
                    performConnect();
                } else {
                    Log.e(TAG, "onActivityResult(): BT not enabled");
                    connectButton.setEnabled(true);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();

        // user info block
        ParseUser user = ParseUser.getCurrentUser();
        userEmailView.setText(user.getEmail());
        userNameView.setText(ParseTools.getUserFullName(user));

        connectButton.requestFocus();
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

    private void requestUpdateInfo(String name, String description) {
        try {
            Message msg = Message.obtain(null, CardioMonitoringService.MSG_UPDATE_INFO);
            msg.getData().putString("name", name);
            msg.getData().putString("description", description);
            mCardioService.send(msg);
        } catch (RemoteException ex) {
            Log.w(TAG, "requestUpdateInfo() failed", ex);
        }
    }

    private void resetHRM() {
        try {
            Message msg = Message.obtain(null, CardioMonitoringService.MSG_RESET);
            msg.replyTo = mMessenger;
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
        msg.replyTo = mMessenger;
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
                // nothing selected
                if (LeHRMonitor.CONNECTING_STATUS != hrmState)
                    connectButton.setEnabled(true);
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
                sessionDataLoaded = true;
                connectDeviceAddress(deviceAddress);
                alertSelectDevice.cancel();
                alertSelectDevice = null;
            }
        };
        pairedListView.setOnItemClickListener(mPairedListClickListener);
        // preventing double click
        connectButton.setEnabled(false);
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

    private void startSession() {
        try {
            String userId = ParseUser.getCurrentUser().getObjectId();
            Message msg = Message.obtain(null, CardioMonitoringService.MSG_START_SESSION);
            msg.getData().putString("parseUserId", userId);
            int limitType = prefHelper.getInt(ConfigurationConstants.MEASUREMENT_LIMIT_TYPE + "_" + userId, 1);
            msg.getData().putInt("limitType", limitType);
            if (limitType == 1) {
                int limitIndex = prefHelper.getInt(ConfigurationConstants.MEASUREMENT_TIME_LIMIT + "_" + userId, 0);
                int limit = getResources().getIntArray(R.array.time_limit_values)[limitIndex];
                msg.getData().putInt("durationLimit", limit);
            } else if (limitType == 2) {
                int limitIndex = prefHelper.getInt(ConfigurationConstants.MEASUREMENT_COUNT_LIMIT + "_" + userId, 0);
                int limit = getResources().getIntArray(R.array.count_limit_values)[limitIndex];
                msg.getData().putInt("countLimit", limit);
            } else {
                // custom limits
                int countLimit = Integer.parseInt(
                        prefHelper.getString(ConfigurationConstants.MEASUREMENT_CUSTOM_COUNT_LIMIT + "_" + userId, "0")
                );
                int durationLimit = Integer.parseInt(
                        prefHelper.getString(ConfigurationConstants.MEASUREMENT_CUSTOM_TIME_LIMIT + "_" + userId, "0")
                );
                msg.getData().putInt("durationLimit", durationLimit);
                msg.getData().putInt("countLimit", countLimit);
            }
            msg.replyTo = mMessenger;
            mCardioService.send(msg);
        } catch (Exception ex) {
            Log.w(TAG, "failed to start session", ex);
            Toast.makeText(getActivity(), "Failed to start measurement. " +
                    "Try to restart the app.", Toast.LENGTH_SHORT).show();
        }
    }

    protected void onDataReceived(Message msg) {
        msg.getData().setClassLoader(CardioDataPackage.class.getClassLoader());
        CardioDataPackage data = msg.getData().getParcelable("data");
        if (data == null)
            return;

        heartRateView.setText(String.valueOf(data.getBpm()));

        if (mCurrentSession != null && sessionDataLoaded && graphT >= 0) {
            long t = msg.getData().getLong("t", 0L);
            int[] rr = data.getRr();
            if (rr != null) {
                for (int r: rr) {
                    t -= r;
                }
                if (t > graphT) {
                    graphT = t;
                }
                for (int r: rr) {
                    mHeartRateSeries.appendData(
                            new GraphView.GraphViewData(graphT, Math.round(60000.f/r)),
                            true, MAX_GRAPH_VIEW_POINTS);
                    graphT += r;
                }
            }
        }
    }

    private void onSessionStarted(Message msg) {
        if (msg.arg1 == CardioMonitoringService.RESULT_SUCCESS) {
            msg.getData().setClassLoader(long.class.getClassLoader());
            mCurrentSessionId = msg.getData().getLong("sessionId", -1L);
            startSessionButton.setEnabled(false);
            //startSessionButton.setVisibility(View.GONE);
//            stopSessionButton.setEnabled(true);
//            stopSessionButton.setVisibility(View.VISIBLE);
            loadSessionData(mCurrentSessionId);
        }
    }

    private void onSessionFinished(Message msg) {
        if (mCurrentSessionId >0) {
            if (!isDetached()) {
                Intent intent = new Intent(getActivity(), SessionDetailsActivity.class);
                intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, mCurrentSessionId);
                intent.putExtra(SessionDetailsActivity.POST_RENDER_ACTION_EXTRA, SessionDetailsActivity.RENAME_ACTION);
                startActivity(intent);

                Toast.makeText(getActivity(), R.string.please_wait, Toast.LENGTH_SHORT).show();

                CommonTools.vibrate(getActivity(), new long[]{0, 500, 300, 500}, -1);
            }
            mCurrentSessionId = -1L;
            mCurrentSession = null;
        }
        startSessionButton.setEnabled(false);
        startSessionButton.setVisibility(View.VISIBLE);
        stopSessionButton.setEnabled(false);
        stopSessionButton.setVisibility(View.GONE);
        mGraphView.setVisibility(View.GONE);
        emptyMessageView.setVisibility(View.VISIBLE);
        measurementProgressBar.setVisibility(View.GONE);
        resetHRM();
        onConnectionStatusChanged(LeHRMonitor.CONNECTED_STATUS, LeHRMonitor.INITIAL_STATUS, null);
        graphT = -1;
    }

    private void onReconnecting(Message msg) {
        if (getActivity() == null)
            return;
        Toast.makeText(getActivity(), "Connection lost! Reconnecting...", Toast.LENGTH_SHORT)
                .show();
        connectButton.setEnabled(true);
        hrmDeviceNameView.setText("Reconnecting...");
        CommonTools.vibrate(getActivity(), 1000);
    }

    private void onConnectionStatusChanged(int oldStatus, int newStatus, String deviceAddress) {
        hrmState = newStatus;
        if (getActivity() == null)
            return;

        if (newStatus != LeHRMonitor.CONNECTED_STATUS) {
            // disconnected
            if (mCurrentSessionId > 0L)
                onSessionFinished(null);
            hrmBattery.setVisibility(View.GONE);
            measurementProgressBar.setVisibility(View.GONE);
        }

        if (oldStatus >= 0) {
            if (oldStatus == LeHRMonitor.CONNECTING_STATUS &&
                    (newStatus == LeHRMonitor.READY_STATUS || newStatus == LeHRMonitor.DISCONNECTING_STATUS)) {
                Toast.makeText(getActivity(), "Connection failed.", Toast.LENGTH_SHORT).show();
            } else if (newStatus == LeHRMonitor.CONNECTED_STATUS) {
                Toast.makeText(getActivity(), "Connected to " + deviceAddress, Toast.LENGTH_SHORT).show();
            } else{
                //Toast.makeText(getActivity(), "Status changed from " + oldStatus + " to " + newStatus + "!", Toast.LENGTH_SHORT).show();
            }
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
                //startSessionButton.setVisibility(View.GONE);
                startSessionButton.setEnabled(false);
                //stopSessionButton.setVisibility(View.VISIBLE);
                //stopSessionButton.setEnabled(true);
            }
            if (prefHelper.getBoolean(ConfigurationConstants.MEASUREMENT_AUTO_START +
                    "_" + ParseUser.getCurrentUser().getObjectId()))
                startSession();
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
            if (alertSelectDevice == null || !alertSelectDevice.isShowing())
                connectButton.setEnabled(true);
            connectButton.setVisibility(View.VISIBLE);
            hrmConnected = false;
            heartRateView.setVisibility(View.GONE);
            startSessionButton.setVisibility(View.VISIBLE);
            startSessionButton.setEnabled(false);
            stopSessionButton.setVisibility(View.GONE);
            return;
        }

        // default state
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
        stopSessionButton.setEnabled(false);
    }

    private void onBatteryLevelReceived(int batteryLevel) {
        hrmBattery.setValue(batteryLevel);
        hrmBattery.setVisibility(View.VISIBLE);
        Toast.makeText(getActivity(), "Heart rate sensor battery level: "
                + hrmBattery.getValue() + "%", Toast.LENGTH_SHORT).show();
        prefHelper.putString(ConfigurationConstants.LAST_DEVICE_BATTERY_LEVEL, batteryLevel + "%");
    }

    protected void loadSessionData(final long sessionId) {
        sessionDataLoaded = false;
        graphT = -1;
        Task.callInBackground(new Callable<SessionEntity>() {
            @Override
            public SessionEntity call() throws Exception {
                SessionDAO dao = DatabaseHelperFactory.getHelper().getSessionDao();
                return dao.queryForId(sessionId);
            }
        }).onSuccess(new Continuation<SessionEntity, SessionEntity>() {
            @Override
            public SessionEntity then(Task<SessionEntity> task) throws Exception {
                mCurrentSession = task.getResult();
                return mCurrentSession;
            }
        }, Task.UI_THREAD_EXECUTOR).onSuccess(new Continuation<SessionEntity, List<GraphView.GraphViewData>>() {
            @Override
            public List<GraphView.GraphViewData> then(Task<SessionEntity> task) throws Exception {
                long sessionId = task.getResult().getId();
                CardioItemDAO dao = DatabaseHelperFactory.getHelper().getCardioItemDao();
                GenericRawResults<String[]> items = dao.queryBuilder()
                        .selectColumns("_id", "rr", "t")
                        .orderBy("_id", false)
                        .limit((long) MAX_GRAPH_VIEW_POINTS)
                        .where().eq("session_id", sessionId)
                        .queryRaw();

                List<String[]> resultList = new ArrayList<String[]>();
                for (String[] item : items) {
                    resultList.add(item);
                }
                Collections.reverse(resultList);
                List<GraphView.GraphViewData> result = new ArrayList<GraphView.GraphViewData>(resultList.size());
                long tt = 0;
                for (String[] item : resultList) {
                    float rr = Float.parseFloat(item[1]);
                    long t = Long.parseLong(item[2]);
                    if (t > tt)
                        tt = t;
                    else tt += 1;
                    int bpm = Math.round(60000.0f/rr);
                    result.add(
                            new GraphView.GraphViewData(
                                    tt,
                                    bpm
                            )
                    );
                }
                return result;
            }
        }, Task.BACKGROUND_EXECUTOR).continueWith(new Continuation<List<GraphView.GraphViewData>, Void>() {
            @Override
            public Void then(Task<List<GraphView.GraphViewData>> task) throws Exception {
                if (task.isFaulted()) {
                    Context ctx = getActivity();
                    if (ctx != null) {
                        Toast.makeText(ctx, "Failed to load session data", Toast.LENGTH_SHORT).show();
                    }
                } else if (task.isCompleted()) {
                    emptyMessageView.setVisibility(View.GONE);

                    // init example series data
                    List<GraphView.GraphViewData> items = task.getResult();
                    GraphView.GraphViewData[] data =
                            new GraphView.GraphViewData[Math.min(items.size(), MAX_GRAPH_VIEW_POINTS)];
                    graphT = 0;
                    long t = 0;
                    for (int j = 0, i = Math.max(items.size() - data.length, 0); i < items.size(); i++, j++) {
                        GraphView.GraphViewData item = items.get(i);
                        if (Math.round(item.getX()) > t) {
                            t = Math.round(item.getX());
                        }
                        data[j] = item;
                    }
                    graphT = t;
                    mHeartRateSeries.resetData(data);
                    mGraphView.setVisibility(View.VISIBLE);
                }
                sessionDataLoaded = true;
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

}
