package com.cardiomood.heartrate.android;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.controls.progress.CircularProgressBar;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.heartrate.android.ads.AdMobController;
import com.cardiomood.heartrate.android.ads.AdsControllerBase;
import com.cardiomood.heartrate.android.dialogs.AboutDialog;
import com.cardiomood.heartrate.android.service.BluetoothHRMService;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.cardiomood.heartrate.android.tools.IMonitors;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.flurry.android.FlurryAgent;
import com.parse.ParseAnalytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    // Bluetooth Intent request codes
    private static final  int REQUEST_ENABLE_BT = 2;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;

    private TextView hintText;
    private Button connectDeviceButton;

    private LinearLayout measurementOptionsLayout;
    private LinearLayout measurementStatusLayout;
    private CircularProgressBar measurementProgress;
    private TextView intervalsCollected;
    private TextView timeElapsed;

    private boolean disableBluetoothOnClose = false;
    private boolean aboutDialogShown = false;

    private PreferenceHelper mPrefHelper;
    private Handler mHandler;
    private boolean mScanning = false;
    private AlertDialog alertSelectDevice;

    // Service registration
    private boolean receiverRegistered = false;
    private boolean hrServiceBound = false;
    private long startTimestamp = 0;
    private List<Short> rrList = Collections.synchronizedList(new ArrayList<Short>());

    private BluetoothHRMService mBluetoothLeService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = (BluetoothHRMService) ((BluetoothHRMService.LocalBinder) service).getService();
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

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            invalidateOptionsMenu();
                        }
                    }
                });

                // update button ui state and options layout
                boolean enableControls = (newStatus != LeHRMonitor.CONNECTING_STATUS);
                connectDeviceButton.setEnabled(enableControls);
                //disableEnableControls(enableControls, measurementOptionsLayout);

                if (newStatus == LeHRMonitor.CONNECTED_STATUS) {
                    FlurryAgent.logEvent("device_connected");
                    //openMonitor();
                    rrList.clear();
                    startTimestamp = System.currentTimeMillis();
                }
                if (newStatus == LeHRMonitor.DISCONNECTING_STATUS || newStatus == LeHRMonitor.READY_STATUS && oldStatus != LeHRMonitor.INITIAL_STATUS) {
                    FlurryAgent.logEvent("device_disconnected");
                    setDisconnectedView();
                }
                if (newStatus == LeHRMonitor.INITIAL_STATUS) {
                    setDisconnectedView();
                }

            }
            if (LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED.equals(action)) {
                short[] rr = intent.getShortArrayExtra(LeHRMonitor.EXTRA_INTERVALS);
                if (rr != null) {
                    for (short r: rr) {
                        rrList.add(r);
                    }
                }
            }

            updateView();
        }
    };

    // Ads
    // Home Screen Interstitial
    private static final String HOME_SCREEN_INTERSTITIAL_UNIT_ID = "ca-app-pub-1994590597793352/2533537627";
    // Home Screen Bottom Banner
    private static final String HOME_SCREEN_BOTTOM_BANNER_UNIT_ID = "ca-app-pub-1994590597793352/1056804428";

    private AdsControllerBase adsController;


    @Override @SuppressLint("WrongViewCast")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectDeviceButton = (Button) findViewById(R.id.btn_connect_device);
        hintText = (TextView) findViewById(R.id.hintText);
        measurementOptionsLayout = (LinearLayout) findViewById(R.id.measurement_options_layout);
        measurementStatusLayout = (LinearLayout) findViewById(R.id.measurement_status_layout);
        measurementProgress = (CircularProgressBar) findViewById(R.id.measurement_progress);
        intervalsCollected = (TextView) findViewById(R.id.intervalsCollected);
        timeElapsed = (TextView) findViewById(R.id.timeElapsed);

        connectDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("connect_device_click");
                if (!deviceConnected())
                    performConnect();
                else tryDisconnect();
            }
        });

        measurementProgress.setLabelConverter(new CircularProgressBar.LabelConverter() {
            @Override
            public String getLabelFor(float progress, float max, Paint paint) {
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                return mBluetoothLeService.getMonitor().getLastBPM() + " bpm";
            }
        });

        setDisconnectedView();

        ParseAnalytics.trackAppOpened(getIntent());

        // initialize utilities
        mHandler = new Handler();
        mPrefHelper = new PreferenceHelper(getApplicationContext(), true);

        // establish connection to BluetoothHRMService
        if (!hrServiceBound) {
            Intent gattServiceIntent = new Intent(this, BluetoothHRMService.class);
            bindService(gattServiceIntent, mServiceConnection, Activity.BIND_AUTO_CREATE);
            hrServiceBound = true;
        }

        // register broadcast receiver to collect data from sensor
        if (!receiverRegistered) {
            registerReceiver(dataReceiver, makeGattUpdateIntentFilter());
            receiverRegistered = true;
        }

        adsController = new AdMobController(
                this,
                (RelativeLayout) findViewById(R.id.container),
                HOME_SCREEN_BOTTOM_BANNER_UNIT_ID,
                HOME_SCREEN_INTERSTITIAL_UNIT_ID
        );
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                FlurryAgent.logEvent("menu_settings_clicked");
                openSettingsActivity();
                return true;
            case R.id.action_about:
                FlurryAgent.logEvent("menu_about_clicked");
                showAboutDialog();
                return true;
            case R.id.action_bt_settings:
                FlurryAgent.logEvent("menu_bt_settings_clicked");
                openBluetoothSettings();
                return true;
            case R.id.action_feedback:
                FlurryAgent.logEvent("menu_feedback_clicked");
                openFeedbackWindow();
                return true;
            case R.id.action_personal_data:
                FlurryAgent.logEvent("menu_personal_data_clicked");
                openPersonalDataWindow();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPersonalDataWindow() {
        startActivity(new Intent(this, PersonalDataActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, ConfigurationConstants.FLURRY_API_KEY);
        adsController.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsController.onResume();

    }

    @Override
    protected void onPause() {
        adsController.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
        adsController.onStop();
    }

    @Override
    protected void onDestroy() {
        adsController.onDestroy();
        adsController = null;

        // try disconnect from HRM Service
        tryDisconnect();

        // unregister broadcast receiver
        if (receiverRegistered) {
            unregisterReceiver(dataReceiver);
            receiverRegistered = false;
        }

        // unbind BluetoothHRMService
        if (hrServiceBound) {
            unbindService(mServiceConnection);
            hrServiceBound = false;
        }

        super.onDestroy();
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

    private void openBluetoothSettings() {
        Intent intent =  new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    private void openFeedbackWindow() {
        startActivity(new Intent(this, FeedbackActivity.class));
    }

    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public void displayInterstitial() {
        ((AdMobController) adsController).showInterstitial();
    }

    private void showAboutDialog() {
        if (!aboutDialogShown) {
            AboutDialog dlg = new AboutDialog(this);
            dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    aboutDialogShown = false;
                }
            });
            dlg.setTitle(R.string.title_about_dialog);
            dlg.show();
        }
    }

    private void openSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
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
            Toast.makeText(this, "Bluetooth adapter is not available.", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Device is already connected.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (monitor != null && monitor.getConnectionStatus() != LeHRMonitor.INITIAL_STATUS) {
                    mBluetoothLeService.disconnect();
                    mBluetoothLeService.close();
                }

                if (!mBluetoothLeService.initialize(this)) {
                    Toast.makeText(this, "Failed to initialize service. Make sure your device is supported.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    return;
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed to initialize service.", ex);
                Toast.makeText(this, "Cannot start Bluetooth.", Toast.LENGTH_SHORT);
                return;
            }
            showConnectionDialog();
        } catch (Exception ex) {
            Log.d(TAG, "performConnect(): "+ex);
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("NewApi")
    private void showConnectionDialog() {
        Log.d(TAG, "showConnectionDialog()");
        final BluetoothAdapter bluetoothAdapter = mBluetoothLeService.getMonitor().getCurrentBluetoothAdapter();

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout_listview = inflater.inflate(R.layout.device_list, (ViewGroup) findViewById(R.id.root_device_list));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(layout_listview);

        final ArrayList<String> discoveredDeviceNames = new ArrayList<String>();
        final ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name, discoveredDeviceNames);
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
                runOnUiThread(new Runnable() {
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

    private void tryDisconnect() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        }
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
                invalidateOptionsMenu();
                if (hrServiceBound && deviceConnected()) {
                    measurementProgress.setProgress(0);
                    intervalsCollected.setText(String.valueOf(rrList.size()));
                    timeElapsed.setText(CommonTools.timeToHumanString(System.currentTimeMillis() - startTimestamp));
                    setConnectedView();
                    connectDeviceButton.setText(getString(R.string.disconnect));
                    hintText.setText(R.string.device_connected);
                } else {
                    setDisconnectedView();
                    connectDeviceButton.setText(getString(R.string.connect_device));
                    hintText.setText(R.string.pair_and_tap_connect);
                }
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED);
        intentFilter.addAction(LeHRMonitor.ACTION_BPM_CHANGED);
        intentFilter.addAction(LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED);
        return intentFilter;
    }

}
