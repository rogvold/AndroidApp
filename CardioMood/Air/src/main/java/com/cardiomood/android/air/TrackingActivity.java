package com.cardiomood.android.air;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.data.AirSession;
import com.cardiomood.android.air.data.Aircraft;
import com.cardiomood.android.air.gps.GPSMonitor;
import com.cardiomood.android.air.gps.GPSService;
import com.cardiomood.android.air.gps.GPSServiceApi;
import com.cardiomood.android.air.gps.GPSServiceListener;
import com.cardiomood.android.air.tools.ParseTools;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.parse.DeleteCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class TrackingActivity extends Activity {

    public static final String SELECTED_PLANE_PARSE_ID = "com.cardiomood.android.air.extra.SELECTED_PLANE_PARSE_ID";
    public static final String GET_PLANE_FROM_SERVICE = "com.cardiomood.android.air.extra.GET_PLANE_FROM_SERVICE";

    private static final int REQUEST_ENABLE_BT = 2;

    private static final String TAG = TrackingActivity.class.getSimpleName();

    private GPSMonitor gpsMonitor;
    private LeHRMonitor hrMonitor;
    private Handler mHandler;

    private boolean mScanning = false;
    private AlertDialog alertSelectDevice;


    // current state variables
    private Aircraft mPlane = null;
    private volatile AirSession mAirSession = null;

    private GPSServiceApi gpsService;
    private boolean gpsBound;
    private ServiceConnection gpsConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            gpsService = GPSServiceApi.Stub.asInterface(service);
            gpsBound = true;

            try {
                gpsService.hideNotification();
            } catch (RemoteException ex) {
                Log.d(TAG, "onServiceConnected(): api.hideNotification() failed", ex);
            }

            try {
                gpsService.addListener(gpsServiceListener);
            } catch (Exception ex) {
                Log.d(TAG, "onServiceConnected(): api.addListener() failed", ex);
            }

            loadPlaneData();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            gpsBound = false;
            mStopButton.setEnabled(false);
        }
    };


    // view
    private MapFragment mMapFragment;
    private GoogleMap mMap;
    private View mCurrentUserView;
    private TextView mAltitudeView;
    private TextView mSpeedView;
    private Button mStopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate() savedInstanceState=" + savedInstanceState);

        // check if signed in
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null || !currentUser.isAuthenticated()) {
            performLogout(false);
            return;
        }

        // initialize view
        setContentView(R.layout.activity_tracking);

        mCurrentUserView = findViewById(R.id.current_user_box);
        final TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        text1.setText(ParseTools.getUserFullName(currentUser));
        final TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text2.setText(currentUser.getEmail());

        mAltitudeView = (TextView) findViewById(R.id.altitude);
        mSpeedView = (TextView) findViewById(R.id.speed);

        // initialize map
        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMap = mMapFragment.getMap();
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMyLocationEnabled(true);

        gpsMonitor = new GPSMonitor(this);
        Location lastLocation = gpsMonitor.getLastKnownLocation();
        if (lastLocation != null) {
            // setup initial map coordinated
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 14));
        }

        mStopButton = (Button) findViewById(R.id.stop_button);
        mStopButton.setEnabled(false);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(TrackingActivity.this)
                        .setCancelable(true)
                        .setTitle("Confirm finish")
                        .setMessage("Are you sure you want finish your flight?")
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                stopGPSService();
                            }
                        }).show();
            }
        });

        hrMonitor = LeHRMonitor.getMonitor(this);
        mHandler = new Handler();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // start service
        Intent intent = new Intent(this, GPSService.class);
        startService(intent);

        // Bind GPSService
        bindService(intent, gpsConnection, Context.BIND_AUTO_CREATE);
    }



    @Override
    protected void onStop() {
        super.onStop();

        if (gpsBound) {
            // show notification if the service is running
            try {
                if (gpsService.isRunning())
                    gpsService.showNotification();
            } catch (RemoteException ex) {
                Log.d(TAG, "onStop() -> show notification failed", ex);
            }
            // remove listener
            try {
                gpsService.removeListener(gpsServiceListener);
            } catch (RemoteException ex) {
                Log.d(TAG, "onStop() -> remove listener failed", ex);
            }

        }

        if (gpsBound) {
            unbindService(gpsConnection);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(TAG, "onSaveInstanceState()");
        if (mAirSession != null && mAirSession.getObjectId() != null) {
            outState.putString("sessionId", mAirSession.getObjectId());
            mAirSession.pinInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e != null) {
                        Log.w(TAG, "onSaveInstanceState() -> " +
                                "failed to save session to the local data store", e);
                    }
                }
            });
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.d(TAG, "onRestoreInstanceState()");
        final String sessionId = savedInstanceState.getString("sessionId");
        if (sessionId != null ) {
            Task.callInBackground(new Callable<AirSession>() {
                @Override
                public AirSession call() throws Exception {
                    return ParseQuery.getQuery(AirSession.class)
                            .fromLocalDatastore()
                            .get(sessionId);
                }
            }).continueWith(new Continuation<AirSession, AirSession>() {
                @Override
                public AirSession then(Task<AirSession> task) throws Exception {
                    if (task.isCompleted()) {
                        mAirSession = task.getResult();
                        return mAirSession;
                    } else {
                        Log.d(TAG, "task was not finished", task.getError());
                        throw new Exception(task.getError());
                    }
                }
            }).onSuccess(new Continuation<AirSession, Void>() {
                @Override
                public Void then(Task<AirSession> task) throws Exception {
                    if (task.isCompleted()) {
                        task.getResult().unpinInBackground(new DeleteCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    Log.w(TAG, "failed to unpin session", e);
                                }
                            }
                        });
                    }
                    return null;
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGPSEnabled();

        if (gpsBound) {
            // add listener
            try {
                gpsService.addListener(gpsServiceListener);
            } catch (RemoteException ex) {
                Log.d(TAG, "onStart() -> add listener failed", ex);
            }

            // hide notification
            try {
                gpsService.hideNotification();
            } catch (RemoteException ex) {
                Log.d(TAG, "onStart() -> hide notification failed", ex);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_tracking, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                showLogoutDialog();
                return true;
            case R.id.menu_connect_hr:
                performConnect();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    private void loadPlaneData() {
        final TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        text1.setText(ParseTools.getUserFullName(ParseUser.getCurrentUser()));
        final TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text2.setText("Loading...");

        boolean serviceIsRunning = false;
        try {
            serviceIsRunning = gpsService.isRunning();
        } catch (RemoteException ex) {
            // suppress this
        }

        String planeId = getIntent().getStringExtra(SELECTED_PLANE_PARSE_ID);
        if (serviceIsRunning) {
            try {
                planeId = gpsService.getAircraftId();
            } catch (RemoteException ex) {
                Log.w(TAG, "failed to get planeId from service", ex);
                serviceIsRunning = false;
            }
        }

        // load plane from Parse
        ParseQuery query = ParseQuery.getQuery(Aircraft.class);
//        if (!serviceIsRunning)
//            query.fromPin();
        query.getInBackground(planeId, new GetCallback<Aircraft>() {
            @Override
            public void done(Aircraft parseObject, ParseException e) {
                if (e == null) {
                    onPlaneLoaded(parseObject);
                } else {
                    Toast.makeText(TrackingActivity.this, "Plane not found.", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(TrackingActivity.this, PlanesActivity.class));
                    finish();
                }
            }
        });
    }

    private void onPlaneLoaded(Aircraft plane) {
        mPlane = plane;

        // update view
        TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text1.setText(mPlane.getName() + " :: " + mPlane.getAircraftId());
        text2.setText(ParseTools.getUserFullName(ParseUser.getCurrentUser()));

        // notify user
        Toast.makeText(TrackingActivity.this, "Plane: " + plane.getString("name"), Toast.LENGTH_SHORT).show();

        // create tracking session if not created
        try {
            if (gpsService.isRunning()) {
                gpsServiceListener.onTrackingSessionStarted(gpsService.getUserId(), gpsService.getAircraftId(), gpsService.getAirSessionId());
            } else {
                gpsService.startTrackingSession(ParseUser.getCurrentUser().getObjectId(), plane.getObjectId());
            }
        } catch (RemoteException ex) {
            Log.w(TAG, "failed to setup tracking session", ex);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Confirm logout")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        performLogout();
                    }
                }).show();
    }

    private void stopGPSService() {
        boolean showProgress = false;
        if (gpsBound) {
            try {
                if (gpsService.isRunning()) {
                    gpsService.stopTrackingSession();
                    showProgress = true;
                }
            } catch (RemoteException ex) {
                Log.w(TAG, "stopGPSService() failed to close the service", ex);
            }
        }
        if (showProgress) {
            new ProgressDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("Waiting for the service to finish...")
                    .show();
        }
    }

    private void performLogout() {
        performLogout(true);
    }

    public boolean requestEnableBluetooth() {
        if (hrMonitor == null)
            return false;
        BluetoothAdapter bluetoothAdapter = hrMonitor.getBluetoothAdapter();
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
                // show dialog to enable bluetoth
                if (!requestEnableBluetooth())
                    return;

                // call to initialize service
               if (!gpsService.initBLE()) {
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
        final BluetoothAdapter bluetoothAdapter = hrMonitor.getBluetoothAdapter();

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

        dialogBuilder.setTitle("Select heart rate monitor");
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
                hrMonitor.close();
                hrMonitor = LeHRMonitor.getMonitor(TrackingActivity.this);
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
                    gpsService.connectHRMonitor(deviceAddress);
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
            }, 100000);

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


    private void performLogout(boolean openLoginActivity) {
        // Parse log out
        ParseUser.logOut();

        if (!openLoginActivity)
            finish();

        stopGPSService();
    }


    private void checkGPSEnabled() {
        if (gpsMonitor.isGPSEnabled()) {
            // GPS is available!
            if (!gpsMonitor.isRunning())
                gpsMonitor.start();
            return;
        }

        // Ask user to enable GPS or logout
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Geo Location Services")
                .setMessage("Geo Location Services and GPS are not available. Please, enable GPS Satellites in Settings to continue.")
                .setNegativeButton("Log out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        performLogout(true);
                    }
                })
                .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        gpsMonitor.openLocationSettings();
                    }
                }).show();
    }

    private final GPSServiceListener.Stub gpsServiceListener =  new GPSServiceListener.Stub() {

        @Override
        public void onLocationChanged(final Location location) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (location.hasSpeed())
                        mSpeedView.setText(String.format("%.2f km/h", location.getSpeed()*3.6f));
                    if (location.hasAltitude())
                        mAltitudeView.setText(String.format("%d m", (int) location.getAltitude()));

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                }
            });

        }

        @Override
        public void onHeartRateChanged(int heartRate) throws RemoteException {

        }

        @Override
        public void onStressChanged(double stress) throws RemoteException {

        }

        @Override
        public void onTrackingSessionStarted(String userId, String aircraftId, final String airSessionId) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mStopButton.setEnabled(true);
                        if (!TrackingActivity.this.isFinishing())
                            Toast.makeText(TrackingActivity.this, "Tracking session is running. SessionId="
                                    + airSessionId, Toast.LENGTH_SHORT).show();
                    } catch(Exception ex) {
                        // suppress this
                    }
                }
            });
        }

        @Override
        public void onTrackingSessionFinished() throws RemoteException {
            //Toast.makeText(TrackingActivity.this, "Tracking session finished.", Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!TrackingActivity.this.isFinishing()) {
                            startActivity(new Intent(TrackingActivity.this, LoginActivity.class));
                            TrackingActivity.this.finish();
                        }
                    } catch(Exception ex) {
                        // suppress this
                    }
                }
            });

        }
    };
}
