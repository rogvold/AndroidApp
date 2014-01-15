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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.R;
import com.cardiomood.android.SessionDetailsActivity;
import com.cardiomood.android.bluetooth.HeartRateLeService;
import com.cardiomood.android.bluetooth.LeHRMonitor;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;
import com.cardiomood.android.tools.IMonitors;
import com.flurry.android.FlurryAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Project: CardioMood
 * User: danon
 * Date: 23.05.13
 * Time: 23:50
 */
public class MonitorFragment extends Fragment {

   private static final String TAG = "CardioMood.MonitorFragment";
    // Bluetooth Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;

    private static final int HR_DATA_EVENT = 1;
    private static final int CONNECTION_STATUS_CHANGE_EVENT = 2;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private View container;
    private WebView webView;
    private View initialView;
    private Button connectDeviceButton;
    private ScrollView scrollView;

    private Handler mHandler;
    private boolean mScanning = false;

    private AlertDialog alertSelectDevice;

    private List<HeartRateDataItem> collectedData;

    private ProgressDialog sessionSavingDialog;
    public static boolean isMonitoring = false;
    private boolean isConnectedView = false;

    private HeartRateLeService mBluetoothLeService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((HeartRateLeService.LocalBinder) service).getService();
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
            if (LeHRMonitor.ACTION_BPM_CHANGED.equals(action)) {
                int bpm = intent.getIntExtra(LeHRMonitor.EXTRA_NEW_BPM, 0);
                execJS("setPulse(" + bpm + ")");
            }
            if (LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED.equals(action)) {
                final int newStatus = intent.getIntExtra(LeHRMonitor.EXTRA_NEW_STATUS, -100);
                final int oldStatus = intent.getIntExtra(LeHRMonitor.EXTRA_OLD_STATUS, -100);

                if (getActivity() != null) {
                    getActivity().invalidateOptionsMenu();
                }

                // update button ui state
                connectDeviceButton.setEnabled(newStatus == LeHRMonitor.CONNECTED_STATUS || newStatus == LeHRMonitor.INITIAL_STATUS);

                // update timer
                if (newStatus == LeHRMonitor.CONNECTED_STATUS) {
                    FlurryAgent.logEvent("device_connected");
                    monitorTime = 0;
                    if (timer != null) {
                        timer.cancel();
                        timer.purge();
                    }
                    timer = new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            monitorTime += 1;
                            if (monitorTime > 120) {
                                timer.cancel();
                                timer.purge();
                                timer = null;
                                performDisconnect();
                                saveAndOpenSessionView();
                            } else {
                                execJS("setProgress(" + (float)monitorTime/120*100 + ");");
                            }

                        }
                    }, 0, 1000);
                    if (collectedData!= null) {
                        collectedData.clear();
                    }

                    collectedData = Collections.synchronizedList(new ArrayList<HeartRateDataItem>());

                    setConnectedView();
                }
                if (newStatus == LeHRMonitor.DISCONNECTING_STATUS || newStatus == LeHRMonitor.READY_STATUS && oldStatus != LeHRMonitor.INITIAL_STATUS) {
                    if (timer != null) {
                        timer.cancel();
                        timer.purge();
                        timer = null;
                    }
                    if (monitorTime <=120) {
                        Toast.makeText(getActivity(), R.string.device_was_disconnected, Toast.LENGTH_SHORT).show();
                    }
                    FlurryAgent.logEvent("device_disconnected", new HashMap<String, String>(){{put("monitorTime", monitorTime+"");}});
                    setDisconnectedView();
                }
                if (newStatus == LeHRMonitor.INITIAL_STATUS) {
                    setDisconnectedView();
                }
            }
            if (LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED.equals(action)) {
                int bpm = intent.getIntExtra(LeHRMonitor.EXTRA_BPM, 0);
                short[] rr = intent.getShortArrayExtra(LeHRMonitor.EXTRA_INTERVALS);
                // this should be always invoked!!!
                saveHeartRateData(bpm, 0, rr);
                execJS("setPulse(" + bpm + ")");
            }
        }
    };

    private long monitorTime = 0;
    private Timer timer = new Timer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        Intent gattServiceIntent = new Intent(getActivity(), HeartRateLeService.class);
        getActivity().bindService(gattServiceIntent, mServiceConnection, Activity.BIND_AUTO_CREATE);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(dataReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void initWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setBuiltInZoomControls(false);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        webView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        this.container = inflater.inflate(R.layout.fragment_monitor, container, false);
        webView = (WebView) this.container.findViewById(R.id.webView1);
        initialView = this.container.findViewById(R.id.initial_view);
        scrollView = (ScrollView) this.container.findViewById(R.id.connected_view);
        connectDeviceButton = (Button) this.container.findViewById(R.id.btn_connect_device);

        connectDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("connect_device_click");
                performConnect();
            }
        });

        setDisconnectedView();
        return this.container;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                FlurryAgent.logEvent("stop_button_clicked", new HashMap<String, String>(){{put("monitorTime", monitorTime+"");}});
                performDisconnect();
                break;
        }
        return false;
    }

    private void setConnectedView() {
        isConnectedView = false;
        final Semaphore mutex = new Semaphore(1);
        isMonitoring = true;
        initWebView();
        scrollView.setVisibility(View.VISIBLE);
        try {
            mutex.acquire();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        webView.loadUrl(getString(R.string.asset_countdown_html));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (getString(R.string.asset_countdown_html).equals(url)) {
                    mutex.release();
                    isConnectedView = true;
                }
            }
        });
        connectDeviceButton.setEnabled(false);
//        execJS("setSliderText(1, \"" + getString(R.string.monitor_slider_text1) + "\")");
//        execJS("setSliderText(2, \"" + getString(R.string.monitor_slider_text2) + "\")");
//        execJS("setSliderText(3, \"" + getString(R.string.monitor_slider_text3) + "\")");
//        execJS("setSliderText(4, \"" + getString(R.string.monitor_slider_text4) + "\")");

        // Wait for the page to load
        try {
            while (true) {
                if (mutex.tryAcquire(100L, TimeUnit.MILLISECONDS));
                    mutex.release();
                    return;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void setDisconnectedView() {
        isConnectedView = false;
        scrollView.setVisibility(View.GONE);
        webView.stopLoading();
        connectDeviceButton.setEnabled(true);
        initialView.setVisibility(View.VISIBLE);
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
                if (!mBluetoothLeService.initialize(getActivity())) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    return;
                }
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

    private void execJS(final String js) {
        if (!isConnectedView) {
            return;
        }
        final Activity activity = getActivity();
        if (activity == null)
            return;
        activity.runOnUiThread(new Runnable() {
            @Override
            @SuppressWarnings("NewApi")
            public void run() {
                Log.d(TAG, "execJS(): js = " + js);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(js, null);
                } else {
                    webView.loadUrl("javascript:" + js);
                }
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
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

    private void saveAndOpenSessionView() {
        final List<HeartRateDataItem> data = new ArrayList<HeartRateDataItem>(collectedData);
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected void onPreExecute() {
                showSavingSessionDialog();
            }

            @Override
            protected Long doInBackground(Void... params) {
                if (data.isEmpty())
                    return null;
                HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO();
                HeartRateSession session = new HeartRateSession();
                session.setDateStarted(data.get(0).getTimeStamp());
                session.setDateEnded(new Date());
                session.setStatus(SessionStatus.COMPLETED);
                session = sessionDAO.insert(session, data);
                Long sessionId = session.getId();

                Map<String, String> args = new HashMap<String, String>();
                args.put("sessionId", sessionId+"");
                args.put("totalSessions", sessionDAO.getCount()+"");
                FlurryAgent.logEvent("session_saved", args);

                return sessionId;
            }

            @Override
            protected void onPostExecute(Long sessionId) {
                if (sessionId == null) {
                    return;
                }
                Intent intent = new Intent(getActivity(), SessionDetailsActivity.class);
                intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, sessionId);
                intent.putExtra(SessionDetailsActivity.POST_RENDER_ACTION_EXTRA, SessionDetailsActivity.RENAME_ACTION);
                startActivity(intent);

                removeSavingSessionDialog();
            }
        }.execute();
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        performDisconnect();
        getActivity().unbindService(mServiceConnection);
        mBluetoothLeService = null;
        getActivity().unregisterReceiver(dataReceiver);
    }

    private void saveHeartRateData(int heartBeatsPerMinute, int energyExpended, short[] rrIntervals) {
        try {
            for (short rr: rrIntervals) {
                collectedData.add(new HeartRateDataItem(heartBeatsPerMinute, (int) (rr * (1.0 / 1024 * 1000))));
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG, "onCreateOptionsMenu()");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_monitor, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_disconnect);
        LeHRMonitor monitor = mBluetoothLeService == null ? null : mBluetoothLeService.getMonitor();
        if (item != null && monitor != null) {
            int c = monitor.getConnectionStatus();
            if (c == LeHRMonitor.CONNECTED_STATUS) {
                item.setEnabled(true);
                //item.setVisible(true);
            } else {
                item.setEnabled(false);
                //item.setVisible(false);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED);
        intentFilter.addAction(LeHRMonitor.ACTION_BPM_CHANGED);
        intentFilter.addAction(LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED);
        return intentFilter;
    }

}
