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
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.MonitoringActivity;
import com.cardiomood.android.R;
import com.cardiomood.android.SessionDetailsActivity;
import com.cardiomood.android.controls.progress.CircularProgressBar;
import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.entity.ContinuousSessionEntity;
import com.cardiomood.android.gps.CardioMoodGPSService;
import com.cardiomood.android.gps.GPSDataCollector;
import com.cardiomood.android.heartrate.AbstractDataCollector;
import com.cardiomood.android.heartrate.CardioMoodHeartRateLeService;
import com.cardiomood.android.heartrate.IntervalLimitDataCollector;
import com.cardiomood.android.heartrate.TimeAndIntervalLimitDataCollector;
import com.cardiomood.android.heartrate.TimeLimitDataCollector;
import com.cardiomood.android.heartrate.UnlimitedDataCollector;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.IMonitors;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
   private static final  int REQUEST_ENABLE_BT = 2;

    // Stops scanning after 100 seconds.
    private static final long SCAN_PERIOD = 100000;

    private View container;
    private TextView hintText;
    private Button connectDeviceButton;

    private LinearLayout measurementOptionsLayout;
    private LinearLayout measurementStatusLayout;
    private LinearLayout timeLimitLayout;
    private LinearLayout countLimitLayout;
    private LinearLayout customLimitLayout;
    private Spinner limitTypeSpinner;
    private Spinner timeLimitSpinner;
    private Spinner countLimitSpinner;
    private EditText customCountLimitTxt;
    private EditText customTimeLimitTxt;
    private CheckBox startImmediately;
    private CircularProgressBar measurementProgress;
    private TextView intervalsCollected;
    private TextView timeElapsed;

    private PreferenceHelper mPrefHelper;
    private Handler mHandler;
    private boolean mScanning = false;

    private AlertDialog alertSelectDevice;

    private ProgressDialog sessionSavingDialog;
    private boolean disableBluetoothOnClose = false;

    // Service registration
    private boolean receiverRegistered = false;
    private boolean hrServiceBound = false;
    private boolean gpsServiceBound = false;

    private CardioMoodHeartRateLeService mBluetoothLeService;
    private CardioMoodGPSService mGPSService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mHRServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = (CardioMoodHeartRateLeService) ((CardioMoodHeartRateLeService.LocalBinder) service).getService();
            updateView();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final ServiceConnection mGPSServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mGPSService = ((CardioMoodGPSService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mGPSService = null;
        }
    };


    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED.equals(action)) {
                final int newStatus = intent.getIntExtra(LeHRMonitor.EXTRA_NEW_STATUS, -100);
                final int oldStatus = intent.getIntExtra(LeHRMonitor.EXTRA_OLD_STATUS, -100);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            getActivity().invalidateOptionsMenu();
                        }
                    }
                });

                // update button ui state and options layout
                boolean enableControls = (newStatus != LeHRMonitor.CONNECTING_STATUS);
                connectDeviceButton.setEnabled(enableControls);
                disableEnableControls(enableControls, measurementOptionsLayout);

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
    private DatabaseHelper databaseHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        if (!gpsServiceBound) {
            Intent gpsServiceIntent = new Intent(getActivity(), CardioMoodGPSService.class);
            getActivity().bindService(gpsServiceIntent, mGPSServiceConnection, Activity.BIND_AUTO_CREATE);
            gpsServiceBound = true;
        }

        if (!hrServiceBound) {
            Intent gattServiceIntent = new Intent(getActivity(), CardioMoodHeartRateLeService.class);
            getActivity().bindService(gattServiceIntent, mHRServiceConnection, Activity.BIND_AUTO_CREATE);
            hrServiceBound = true;
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
        updateView();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (receiverRegistered) {
            receiverRegistered = false;
            getActivity().unregisterReceiver(dataReceiver);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        this.container = inflater.inflate(R.layout.fragment_connection, container, false);
        connectDeviceButton = (Button) this.container.findViewById(R.id.btn_connect_device);
        hintText = (TextView) this.container.findViewById(R.id.hintText);
        measurementOptionsLayout = (LinearLayout) this.container.findViewById(R.id.measurement_options_layout);
        limitTypeSpinner = (Spinner) this.container.findViewById(R.id.limit_by);
        timeLimitLayout = (LinearLayout) this.container.findViewById(R.id.time_limit_layout);
        countLimitLayout = (LinearLayout) this.container.findViewById(R.id.count_limit_layout);
        customLimitLayout = (LinearLayout) this.container.findViewById(R.id.custom_limit_layout);
        timeLimitSpinner = (Spinner) this.container.findViewById(R.id.time_limit);
        countLimitSpinner = (Spinner) this.container.findViewById(R.id.count_limit);
        customCountLimitTxt = (EditText) this.container.findViewById(R.id.custom_count_limit);
        customTimeLimitTxt = (EditText) this.container.findViewById(R.id.custom_time_limit);
        startImmediately = (CheckBox) this.container.findViewById(R.id.auto_start_measurement);
        measurementStatusLayout = (LinearLayout) this.container.findViewById(R.id.measurement_status_layout);
        measurementProgress = (CircularProgressBar) this.container.findViewById(R.id.measurement_progress);
        intervalsCollected = (TextView) this.container.findViewById(R.id.intervalsCollected);
        timeElapsed = (TextView) this.container.findViewById(R.id.timeElapsed);

        connectDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("connect_device_click");
                performConnect();
            }
        });

        measurementProgress.setLineWidth(40 * getResources().getDisplayMetrics().density);
        measurementProgress.setTextSize(27 * getResources().getDisplayMetrics().density);
        measurementProgress.setLabelConverter(new CircularProgressBar.LabelConverter() {
            @Override
            public String getLabelFor(float progress, float max, Paint paint) {
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                return mBluetoothLeService.getMonitor().getLastBPM() + " bpm";
            }
        });
        measurementProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMonitor();
            }
        });

        limitTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (position) {
                            case 0:
                                countLimitLayout.setVisibility(View.GONE);
                                timeLimitLayout.setVisibility(View.GONE);
                                customLimitLayout.setVisibility(View.GONE);
                                break;
                            case 1:
                                countLimitLayout.setVisibility(View.GONE);
                                timeLimitLayout.setVisibility(View.VISIBLE);
                                customLimitLayout.setVisibility(View.GONE);
                                break;
                            case 2:
                                countLimitLayout.setVisibility(View.VISIBLE);
                                timeLimitLayout.setVisibility(View.GONE);
                                customLimitLayout.setVisibility(View.GONE);
                                break;
                            case 3:
                                countLimitLayout.setVisibility(View.GONE);
                                timeLimitLayout.setVisibility(View.GONE);
                                customLimitLayout.setVisibility(View.VISIBLE);
                                break;
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                countLimitLayout.setVisibility(View.GONE);
                timeLimitLayout.setVisibility(View.GONE);
                customLimitLayout.setVisibility(View.GONE);
            }
        });

        setDisconnectedView();

        limitTypeSpinner.setSelection(mPrefHelper.getInt(ConfigurationConstants.MEASUREMENT_LIMIT_TYPE, 0));
        timeLimitSpinner.setSelection(mPrefHelper.getInt(ConfigurationConstants.MEASUREMENT_TIME_LIMIT, 0));
        countLimitSpinner.setSelection(mPrefHelper.getInt(ConfigurationConstants.MEASUREMENT_COUNT_LIMIT, 0));
        customCountLimitTxt.setText(mPrefHelper.getString(ConfigurationConstants.MEASUREMENT_CUSTOM_COUNT_LIMIT, ""));
        customTimeLimitTxt.setText(mPrefHelper.getString(ConfigurationConstants.MEASUREMENT_CUSTOM_TIME_LIMIT, ""));
        startImmediately.setChecked(mPrefHelper.getBoolean(ConfigurationConstants.MEASUREMENT_AUTO_START));

        this.container.postDelayed(new Runnable() {
            @Override
            public void run() {
                CommonTools.hideSoftInputKeyboard(getActivity());
            }
        }, 100);
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

        if (hrServiceBound) {
            hrServiceBound = false;
            getActivity().unbindService(mHRServiceConnection);
        }

        if (gpsServiceBound) {
            gpsServiceBound = false;
            getActivity().unbindService(mGPSServiceConnection);
        }

        if (disableBluetoothOnClose) {
            LeHRMonitor monitor = mBluetoothLeService.getMonitor();
            if (monitor != null) {
                BluetoothAdapter bluetoothAdapter = monitor.getBluetoothAdapter();
                if (bluetoothAdapter!= null && bluetoothAdapter.isEnabled())
                    bluetoothAdapter.disable();
            }
        }

        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }

        mPrefHelper.putInt(ConfigurationConstants.MEASUREMENT_LIMIT_TYPE, limitTypeSpinner.getSelectedItemPosition());
        mPrefHelper.putInt(ConfigurationConstants.MEASUREMENT_TIME_LIMIT, timeLimitSpinner.getSelectedItemPosition());
        mPrefHelper.putInt(ConfigurationConstants.MEASUREMENT_COUNT_LIMIT, countLimitSpinner.getSelectedItemPosition());
        mPrefHelper.putString(ConfigurationConstants.MEASUREMENT_CUSTOM_COUNT_LIMIT, customCountLimitTxt.getText().toString());
        mPrefHelper.putString(ConfigurationConstants.MEASUREMENT_CUSTOM_TIME_LIMIT, customTimeLimitTxt.getText().toString());
        mPrefHelper.putBoolean(ConfigurationConstants.MEASUREMENT_AUTO_START, startImmediately.isChecked());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_connection, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem disconnectItem = menu.findItem(R.id.menu_disconnect);
        MenuItem bpmItem = menu.findItem(R.id.menu_bpm);


        if (deviceConnected()) {
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
        //Toast.makeText(getActivity(), R.string.this_feature_will_be_available_soon, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getActivity(), MonitoringActivity.class));
    }

    private void setDisconnectedView() {
        measurementStatusLayout.setVisibility(View.GONE);
        measurementOptionsLayout.setVisibility(View.VISIBLE);
    }

    private void setConnectedView() {
        connectDeviceButton.setEnabled(true);
        measurementOptionsLayout.setVisibility(View.GONE);
        measurementStatusLayout.setVisibility(View.VISIBLE);
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
                    Toast.makeText(getActivity(), "Failed to initialize service. Make sure your device is supported.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    return;
                }

                if (mGPSService != null && isGPSCollectingEnabled()) {
                    mGPSService.setDataCollector(new GPSDataCollector(mGPSService, getHelper()));
                }

                AbstractDataCollector collector = createDataCollector();
                if (collector != null) {
                    collector.setListener(new AbstractDataCollector.SimpleListener() {

                        @Override
                        public void onStart() {
                            if (mGPSService != null && isGPSCollectingEnabled())
                                mGPSService.start();
                        }

                        @Override
                        public void onDataSaved(ContinuousSessionEntity session) {
                            if (mGPSService != null)
                                mGPSService.close();
                            Activity activity = getActivity();
                            if (activity != null) {
                                Intent intent = new Intent(activity, SessionDetailsActivity.class);
                                intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, session.getId());
                                intent.putExtra(SessionDetailsActivity.POST_RENDER_ACTION_EXTRA, SessionDetailsActivity.RENAME_ACTION);
                                startActivity(intent);
                            }
                        }
                    });
                    if (startImmediately.isChecked()) {
                        collector.startCollecting();
                    }
                }
                mBluetoothLeService.setDataCollector(collector);
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

    private boolean isGPSCollectingEnabled() {
        return mPrefHelper.getBoolean(ConfigurationConstants.GPS_COLLECT_LOCATION);
    }

    public void performDisconnect() {
        if (mBluetoothLeService != null) {
            AbstractDataCollector collector = (AbstractDataCollector) mBluetoothLeService.getDataCollector();
            if (collector == null) {
                mBluetoothLeService.disconnect();
                mBluetoothLeService.close();
            } else {
                collector.stopCollecting();
            }
        }
        if (mGPSService != null) {
            mGPSService.close();
        }
    }

    private AbstractDataCollector createDataCollector() {
        switch (limitTypeSpinner.getSelectedItemPosition()) {
            case 0: return new UnlimitedDataCollector(mBluetoothLeService, getHelper());
            case 1:
                switch (timeLimitSpinner.getSelectedItemPosition()) {
                    case 0: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 60*1000);
                    case 1: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 2*60*1000);
                    case 2: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 3*60*1000);
                    case 3: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 5*60*1000);
                    case 4: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 10*60*1000);
                    case 5: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 30*60*1000);
                    case 6: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 60*60*1000);
                    case 7: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 2*60*60*1000);
                    case 8: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 5*60*60*1000);
                    case 9: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 12*60*60*1000);
                    case 10: return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), 24*60*60*1000);
                }
                break;
            case 2:
                switch (countLimitSpinner.getSelectedItemPosition()) {
                    case 0: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 100);
                    case 1: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 150);
                    case 2: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 200);
                    case 3: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 250);
                    case 4: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 300);
                    case 5: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 400);
                    case 6: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 500);
                    case 7: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 1000);
                    case 8: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 2000);
                    case 9: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 5000);
                    case 10: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 10000);
                    case 11: return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), 20000);
                }
                break;
            case 3:
                int countLimit = 0;
                double timeLimit = 0;
                try {
                    if (customTimeLimitTxt.getText().length() > 0)
                        timeLimit = Double.parseDouble(customTimeLimitTxt.getText().toString());
                } catch (NumberFormatException ex) {
                    Log.d(TAG, "Incorrect double format: " + customTimeLimitTxt.getText());
                }
                try {
                    if (customCountLimitTxt.getText().length() > 0 && TextUtils.isDigitsOnly(customCountLimitTxt.getText().toString()))
                        countLimit = Integer.parseInt(customCountLimitTxt.getText().toString());
                } catch (NumberFormatException ex) {
                    Log.d(TAG, "Incorrect integer format: " + customCountLimitTxt.getText());
                }
                if (timeLimit > 0) {
                    if (countLimit > 0) {
                        return new TimeAndIntervalLimitDataCollector(mBluetoothLeService, getHelper(), timeLimit, countLimit);
                    } else return new TimeLimitDataCollector(mBluetoothLeService, getHelper(), timeLimit*1000);
                } else {
                    if (countLimit > 0) {
                        return new IntervalLimitDataCollector(mBluetoothLeService, getHelper(), countLimit);
                    } else {
                        new UnlimitedDataCollector(mBluetoothLeService, getHelper());
                    }
                }
            default: return new UnlimitedDataCollector(mBluetoothLeService, getHelper());
        }
        return null;
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
        }
        return databaseHelper;
    }

    private boolean deviceConnected() {
        if (mBluetoothLeService == null)
            return false;
        LeHRMonitor monitor = mBluetoothLeService.getMonitor();
        if (monitor != null) {
            return (monitor.getConnectionStatus() == LeHRMonitor.CONNECTED_STATUS);
        } else return false;
    }

    private void updateView() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                getActivity().invalidateOptionsMenu();
                if (hrServiceBound && deviceConnected()) {
                    AbstractDataCollector collector = (AbstractDataCollector) mBluetoothLeService.getDataCollector();
                    if (collector == null) {
                        measurementProgress.setProgress(0);
                        timeElapsed.setText("00:00");
                        intervalsCollected.setText("0");
                    } else {
                        measurementProgress.setProgress((float) collector.getProgress(), 300);
                        intervalsCollected.setText(String.valueOf(collector.getIntervalsCount()));
                        timeElapsed.setText(CommonTools.timeToHumanString(Math.round(collector.getDuration())));
                    }
                    setConnectedView();
                    connectDeviceButton.setText(getString(R.string.open_monitor));
                    hintText.setText(R.string.device_connected_open_monitor);
                } else {
                    setDisconnectedView();
                    connectDeviceButton.setText(getString(R.string.connect_device));
                    hintText.setText(R.string.pair_and_tap_connect);
                }
            }
        });
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
            //bluetoothAdapter.startLeScan(new UUID[]{UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")}, leScanCallback);
            bluetoothAdapter.startLeScan(leScanCallback);
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

    private void disableEnableControls(boolean enable, ViewGroup vg){
        for (int i = 0; i < vg.getChildCount(); i++){
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup){
                disableEnableControls(enable, (ViewGroup)child);
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED);
        intentFilter.addAction(LeHRMonitor.ACTION_BPM_CHANGED);
        intentFilter.addAction(LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED);
        return intentFilter;
    }

}
