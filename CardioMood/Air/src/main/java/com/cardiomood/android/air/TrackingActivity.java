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
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.data.Aircraft;
import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.gps.GPSMonitor;
import com.cardiomood.android.air.service.TrackingService;
import com.cardiomood.android.air.tools.Constants;
import com.cardiomood.android.sync.parse.ParseTools;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.ReachabilityTest;
import com.cardiomood.android.tools.ui.TouchEffect;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class TrackingActivity extends ActionBarActivity implements GoogleMap.OnMyLocationChangeListener {

    public static final String SELECTED_PLANE_PARSE_ID = "com.cardiomood.android.air.extra.SELECTED_PLANE_PARSE_ID";

    private static final int REQUEST_ENABLE_BT = 2;

    private static final String TAG = TrackingActivity.class.getSimpleName();

    private GPSMonitor gpsMonitor;
    private LeHRMonitor hrMonitor;
    private Handler mHandler;
    private PreferenceHelper prefHelper;

    private boolean mScanning = false;
    private AlertDialog alertSelectDevice;


    // current state variables
    private String aircraftId = null;
    private Aircraft mPlane = null;
    private Long mCurrentSessionId = null;
    private int hrmState = LeHRMonitor.INITIAL_STATUS;

    private Messenger trackingService;
    private boolean serviceBound;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            trackingService = new Messenger(service);
            serviceBound = true;

            // subscribe for cardio and gps data notification
            registerClient();

            // request current status of the service
            requestServiceStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
            mStopButton.setEnabled(false);
            trackingService = null;
        }
    };

    protected class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //this switch reads the information in the message (usually just
                //an integer) and will do something depending on which integer is sent
                case TrackingService.MSG_GET_STATUS:
                    long sessionId = msg.getData().getLong("sessionId", -1L);
                    if (sessionId != -1L) {
                        mCurrentSessionId = sessionId;
                        aircraftId = msg.getData().getString("aircraftId");
                    } else {
                        aircraftId = getIntent().getStringExtra(SELECTED_PLANE_PARSE_ID);
                        if (aircraftId == null) {
                            finish();
                        }
                    }
                    onConnectionStatusChanged(-1, msg.arg1, msg.getData().getString("deviceAddress"));
                    if (sessionId > 0L) {
                        long startTimestamp = msg.getData().getLong("startTimestamp", 0);
                        onTrackingSessionStarted(TrackingService.RESULT_SUCCESS, startTimestamp);
                    } else {
                        // at this point we can start new tracking session
                        mStartButton.setEnabled(true);
                    }
                    break;
                case TrackingService.MSG_CONNECTION_STATUS_CHANGED:
                    onConnectionStatusChanged(msg.arg1, msg.arg2, msg.getData().getString("deviceAddress"));
                    break;
                case TrackingService.MSG_INIT_HRM:
                    if (msg.arg1 == TrackingService.RESULT_FAIL) {
                        Toast.makeText(TrackingActivity.this, "Failed to initialize Bluetooth SMART.", Toast.LENGTH_SHORT).show();
                        resetHRM();
                    } else {
                        // TODO: assuming RESULT_SUCCESS!
                        performConnect();
                    }
                    break;
                case TrackingService.MSG_CONNECT_HRM:
                    if (msg.arg1 == TrackingService.RESULT_FAIL) {
                        resetHRM();
                        Toast.makeText(TrackingActivity.this, "Failed to connect.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case TrackingService.MSG_HR_DATA:
                    onCardioDataReceived(msg);
                    break;
                case TrackingService.MSG_START_SESSION:
                    long startTimestamp = msg.getData().getLong("startTimestamp", 0);
                    sessionId = msg.getData().getLong("sessionId", -1L);
                    if (sessionId != -1L) {
                        mCurrentSessionId = sessionId;
                    }
                    onTrackingSessionStarted(msg.arg1, startTimestamp);
                    break;
                case TrackingService.MSG_END_SESSION:
                    long endTimestamp = msg.getData().getLong("endTimestamp", 0);
                    startTimestamp = msg.getData().getLong("startTimestamp", 0);
                    onTrackingSessionFinished(msg.arg1, startTimestamp, endTimestamp);
                    break;
                case TrackingService.MSG_BATTERY_LEVEL:
                    // TODO: update battery level in the UI
                    break;
                case TrackingService.MSG_LOCATION_DATA:
                    onLocationDataReceived(msg);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

    }

    protected Messenger mMessenger = new Messenger(new IncomingHandler());

    // view
    private SupportMapFragment mMapFragment;
    private GoogleMap mMap;
    private CardView mControlPanelView;
    private View mCurrentUserView;
    private TextView mInternetView;
    private TextView mAltitudeView;
    private TextView mSpeedView;
    private Button mStopButton;
    private Button mStartButton;
    private TextView mHRMonitorStatusView;
    private Button mConnectHRMonitorButton;
    private TextView mHeartRateView;
    private TextView mDeviceNameView;
    private CardView mMapOverlay;
    private TextView mOverlayAircraftName;
    private TextView mOverlayCallName;
    private TextView mOverlayDistance;
    private TextView mOverlaySpeed;
    private TextView mOverlayHeight;
    private ProgressDialog finishingProgressDialog;
    private Button mShowControlPanelButton;

    // check current state
    private Timer checkStatusTimer = new Timer("check_status");
    private TimerTask checkInternetTask = null;
    private TimerTask checkGPSTask = null;
    private long lastLocationUpdate = 0;
    private Location lastLocation;

    // objects on map
    private Circle mapCircle;
    private Map<String, Marker> markers = new HashMap<String, Marker>();
    private Map<String, Map<String, Object>> nearbyPlanes = new HashMap<String, Map<String, Object>>();
    private List<String> radarList = new ArrayList<String>();
    private String selectedAircraftId = null;
    private GestureDetector overlayGestureDetector;
    private boolean drawTrack = false;
    private Polyline route = null;
    private PolylineOptions routeOpts = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set max priority for UI thread for better rendering
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        Log.d(TAG, "onCreate() savedInstanceState=" + savedInstanceState);

        // check if signed in
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null || !currentUser.isAuthenticated()) {
            performLogout(false);
            return;
        }

        prefHelper = new PreferenceHelper(this, true);

        // initialize view
        setContentView(R.layout.activity_tracking);

        mControlPanelView = (CardView) findViewById(R.id.control_panel);

        mCurrentUserView = findViewById(R.id.current_user_box);
        final TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        text1.setText(ParseTools.getUserFullName(currentUser));
        final TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text2.setText(currentUser.getEmail());

        mInternetView = (TextView) findViewById(R.id.internet);
        mAltitudeView = (TextView) findViewById(R.id.altitude);
        mSpeedView = (TextView) findViewById(R.id.speed);

        mStopButton = (Button) findViewById(R.id.stop_button);
        mStopButton.setOnTouchListener(TouchEffect.FADE_ON_TOUCH);
        mStopButton.setEnabled(false);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(TrackingActivity.this)
                        .setCancelable(true)
                        .setTitle(R.string.titile_confirm_finish)
                        .setMessage(R.string.message_confirm_finish)
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                stopTracking();
                            }
                        }).show();
            }
        });

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnTouchListener(TouchEffect.FADE_ON_TOUCH);
        mStartButton.setEnabled(false);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceBound && !isServiceRunning()) {
                    startTracking();
                    mStartButton.setEnabled(false);
                }
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            findViewById(R.id.hr_monitor_block).setVisibility(View.GONE);
        }

        mHRMonitorStatusView = (TextView) findViewById(R.id.hr_monitor_status);
        mHeartRateView = (TextView) findViewById(R.id.heart_rate);
        mDeviceNameView = (TextView) findViewById(R.id.hrm_device_name);
        mConnectHRMonitorButton = (Button) findViewById(R.id.connect_hr_monitor_button);
        mConnectHRMonitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performConnect();
            }
        });

        mShowControlPanelButton = (Button) findViewById(R.id.show_hide_control_panel_button);
        mShowControlPanelButton.setOnTouchListener(TouchEffect.FADE_ON_TOUCH);
        mShowControlPanelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleControlPanel();
            }
        });

        mMapOverlay = (CardView) findViewById(R.id.map_overlay);
        mOverlayAircraftName = (TextView) findViewById(R.id.overlay_aircraft_name);
        mOverlayCallName = (TextView) findViewById(R.id.overlay_call_name);
        mOverlayDistance = (TextView) findViewById(R.id.overlay_distance);
        mOverlayHeight = (TextView) findViewById(R.id.overlay_height);
        mOverlaySpeed = (TextView) findViewById(R.id.overlay_speed);

        overlayGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return true;
            }
        });
        overlayGestureDetector.setOnDoubleTapListener(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mMapOverlay.setVisibility(View.GONE);
                selectedAircraftId = null;
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mMapOverlay.setOnTouchListener(new TouchEffect() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return overlayGestureDetector.onTouchEvent(event);
                }
            });
        }

        hrMonitor = LeHRMonitor.getMonitor(this);
        gpsMonitor = new GPSMonitor(this);
        mHandler = new Handler();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");

        checkStatusTimer.cancel();

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // start service
        Intent intent = new Intent(this, TrackingService.class);
        startService(intent);

        // Bind GPSService
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // initialize mapFragment
        if (mMapFragment == null)
            mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // init google map
        if (mMap == null) {
            mMap = mMapFragment.getMap();
            if (mMap != null) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                mMap.setMyLocationEnabled(true);
                gpsMonitor = new GPSMonitor(this);
                Location lastLocation = gpsMonitor.getLastKnownLocation();
                this.lastLocation = lastLocation;
                if (lastLocation != null) {
                    LatLng myLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    // setup initial map coordinated
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 14));
                } else {
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
                }
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        Map<String, Object> planeInfo = nearbyPlanes.get(marker.getId());
                        if (planeInfo != null) {
                            selectedAircraftId = (String) planeInfo.get("aircraftId");
                            mOverlayDistance.setText("? m");
                            mOverlayHeight.setText("? m");
                            mOverlayHeight.setTextColor(Color.BLACK);
                            mOverlaySpeed.setText("? m");

                            try {
                                Aircraft aircraft = ParseQuery.getQuery(Aircraft.class)
                                        .fromPin("planes")
                                        .get(selectedAircraftId);
                                if (aircraft != null) {
                                    mOverlayAircraftName.setText(aircraft.getName());
                                    mOverlayCallName.setText("Loading...");
                                }
                            } catch (Exception ex) {
                                mOverlayAircraftName.setText("Unknown");
                                mOverlayCallName.setText("N/A");
                            }

                            mMapOverlay.setVisibility(View.VISIBLE);
                        }
                        return true;
                    }
                });
                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        if (mControlPanelView.isShown()) {
                            toggleControlPanel();
                        }
                    }
                });
            }
        }
    }



    @Override
    protected void onStop() {
        super.onStop();

        if (mMap != null) {
            mMap.setOnMyLocationChangeListener(null);
        }

        if (serviceBound) {
            // unregister client & unbind service
            unregisterClient();
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        checkInternetTask.cancel();
        checkGPSTask.cancel();
        checkInternetTask = null;
        checkGPSTask = null;
        checkStatusTimer.purge();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkInternetTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mInternetView.setText("Checking...");
                        new ReachabilityTest(TrackingActivity.this, "api.parse.com", 80, new ReachabilityTest.Callback() {
                            @Override
                            public void onReachabilityTestPassed() {
                                mInternetView.setText("Connected");
                                mInternetView.setTextColor(Color.rgb(0, 128, 0));
                                Toast.makeText(TrackingActivity.this, "Reachable!", Toast.LENGTH_SHORT);
                            }

                            @Override
                            public void onReachabilityTestFailed() {
                                mInternetView.setText("Not connected!");
                                mInternetView.setTextColor(Color.RED);
                                Toast.makeText(TrackingActivity.this, "Not reachable!", Toast.LENGTH_SHORT);
                            }
                        }).execute();
                    }
                });
            }

        };

        checkGPSTask = new TimerTask() {

            @Override
            public void run() {
                if (System.currentTimeMillis() - lastLocationUpdate >= 10000) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isServiceRunning()) {
                                mAltitudeView.setText("No GPS");
                                mSpeedView.setText("No GPS");
                            }
                        }
                    });
                }
            }
        };

        checkStatusTimer.schedule(checkInternetTask, 500, 10000);
        checkStatusTimer.schedule(checkGPSTask, 1000, 10000);

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            // Google Play Services available
            checkGPSEnabled();
        } else {
            Toast.makeText(this, "Google Play Services are not available. " +
                    "Some features will be disabled.", Toast.LENGTH_SHORT).show();
        }

        drawTrack = prefHelper.getBoolean(Constants.CONFIG_DRAW_TRACK, true, true);
        Log.d(TAG, "onResume(): config.draw_track = " + drawTrack);
        if (route != null) {
            route.setVisible(drawTrack);
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
            case R.id.menu_settings:
                openSettingsActivity();
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

    @Override
    public void onBackPressed() {
        if (isServiceRunning()) {
            super.onBackPressed();
        } else {
            startActivity(new Intent(this, PlanesActivity.class));
            finish();
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        lastLocationUpdate = System.currentTimeMillis();
        lastLocation = location;
        if (location.hasSpeed())
            mSpeedView.setText(String.format("%.2f km/h", location.getSpeed() * 3.6f));
        if (location.hasAltitude())
            mAltitudeView.setText(String.format("%d m", (int) location.getAltitude()));
        else
            mAltitudeView.setText("N/A");

        if (mMap != null) {
            // draw radar circle
            LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mapCircle == null)
                mapCircle = addMapCircle(myLatLng);
            int radius = prefHelper.getInt(Constants.CONFIG_RADAR_RADIUS, Constants.DEFAULT_RADAR_RADIUS);
            mapCircle.setCenter(myLatLng);
            mapCircle.setRadius(radius);
            CameraUpdate cameraUpdate = null;
            if (location.hasBearing()) {
                CameraPosition p = mMap.getCameraPosition();
                cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(myLatLng, p.zoom, p.tilt, location.getBearing()));
            } else {
                cameraUpdate = CameraUpdateFactory.newLatLng(myLatLng);
            }

            if (routeOpts != null) {
                routeOpts.add(myLatLng);
            }
//            List<LatLng> points = route.getPoints();
//            points.add(myLatLng);
//            route.setPoints(points);
            if (route != null) {
                route.setVisible(drawTrack);
            }

            mMap.animateCamera(cameraUpdate);
        }
    }

    private void onTrackingSessionStarted(int responseCode, long startTimestamp) {
        if (responseCode == TrackingService.RESULT_SUCCESS) {
            mStartButton.setEnabled(false);
            mStartButton.setVisibility(View.GONE);
            mStopButton.setEnabled(true);
            mStopButton.setVisibility(View.VISIBLE);

            // auto hide control panel
            if (mControlPanelView.isShown()) {
                toggleControlPanel();
            }

            loadSessionData(mCurrentSessionId);
        } else {
            // TODO: handle failure
        }
    }

    private void onTrackingSessionFinished(int responseCode, long startTimestamp, long endTimestamp) {
        if (route != null) {
            route.remove();
            route = null;
            routeOpts = null;
        }
        Toast.makeText(TrackingActivity.this, "Tracking session finished.", Toast.LENGTH_SHORT).show();
        if (!isFinishing()) {
            mStopButton.setEnabled(false);
            if (finishingProgressDialog != null && finishingProgressDialog.isShowing()) {
                finishingProgressDialog.dismiss();
                finishingProgressDialog = null;
            }
            if (mCurrentSessionId != null) {
                Intent intent = new Intent(TrackingActivity.this, DebriefingActivity.class);
                intent.putExtra(DebriefingActivity.EXTRA_SESSION_ID, mCurrentSessionId);
                startActivity(intent);
            } else {
                startActivity(new Intent(TrackingActivity.this, LoginActivity.class));
            }
            mCurrentSessionId = null;

            finish();
        }

    }

    private void onConnectionStatusChanged(int oldStatus, int newStatus, String deviceAddress) {
        hrmState = newStatus;

        if (oldStatus >= 0) {
            if (oldStatus == LeHRMonitor.CONNECTING_STATUS &&
                    (newStatus == LeHRMonitor.READY_STATUS || newStatus == LeHRMonitor.DISCONNECTING_STATUS)) {
                Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Status changed from " + oldStatus + " to " + newStatus + "!", Toast.LENGTH_SHORT).show();
            }
        }

        if (newStatus == LeHRMonitor.CONNECTED_STATUS) {
            mHRMonitorStatusView.setText(deviceAddress);
            mConnectHRMonitorButton.setVisibility(View.GONE);
            mHeartRateView.setVisibility(View.VISIBLE);
            mDeviceNameView.setText("Connected");
            mConnectHRMonitorButton.setEnabled(true);
        }
        if (newStatus == LeHRMonitor.CONNECTING_STATUS) {
            mHRMonitorStatusView.setText(R.string.hrm_connecting);
            mDeviceNameView.setText(deviceAddress);
            mConnectHRMonitorButton.setEnabled(false);
        }
        if (newStatus == LeHRMonitor.DISCONNECTING_STATUS) {
            mHRMonitorStatusView.setText(R.string.hrm_disconnected);
            mConnectHRMonitorButton.setVisibility(View.VISIBLE);
            mDeviceNameView.setText("No device");
            mHeartRateView.setVisibility(View.GONE);
            mConnectHRMonitorButton.setEnabled(true);
        }
        if (newStatus == LeHRMonitor.READY_STATUS) {
            mHRMonitorStatusView.setText(R.string.hrm_ready);
            mConnectHRMonitorButton.setVisibility(View.VISIBLE);
            mDeviceNameView.setText(R.string.hrm_no_device);
            mHeartRateView.setVisibility(View.GONE);
            mConnectHRMonitorButton.setEnabled(true);
        }


        if (oldStatus == LeHRMonitor.CONNECTED_STATUS) {
            // monitor is disconnected
            mHRMonitorStatusView.setText(R.string.hrm_not_connected);
            mHeartRateView.setVisibility(View.GONE);
            mConnectHRMonitorButton.setVisibility(View.VISIBLE);
        }
    }

    private void onCardioDataReceived(Message msg) {
        mHeartRateView.setText(msg.arg1 + " bpm");
    }

    private void onLocationDataReceived(Message msg) {
        // TODO: should use data from google maps!
//        Location location = msg.getData().getParcelable("location");
//        if (location != null) {
//            lastLocationUpdate = System.currentTimeMillis();
//            lastLocation = location;
//            if (location.hasSpeed())
//                mSpeedView.setText(String.format("%.2f km/h", location.getSpeed() * 3.6f));
//            if (location.hasAltitude())
//                mAltitudeView.setText(String.format("%d m", (int) location.getAltitude()));
//            else
//                mAltitudeView.setText("N/A");
//
//            if (mMap != null) {
//                // draw radar circle
//                LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//                if (mapCircle == null)
//                    mapCircle = addMapCircle(myLatLng);
//                int radius = prefHelper.getInt(Constants.CONFIG_RADAR_RADIUS, Constants.DEFAULT_RADAR_RADIUS);
//                mapCircle.setCenter(myLatLng);
//                mapCircle.setRadius(radius);
//                CameraUpdate cameraUpdate = null;
//                if (location.hasBearing()) {
//                    CameraPosition p = mMap.getCameraPosition();
//                    cameraUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(myLatLng, p.zoom, p.tilt, location.getBearing()));
//                } else {
//                    cameraUpdate = CameraUpdateFactory.newLatLng(myLatLng);
//                }
//
//                mMap.animateCamera(cameraUpdate);
//            }
//        }
    }

    private void registerClient() {
        if (serviceBound) {
            Message msg = Message.obtain(null, TrackingService.MSG_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            try {
                trackingService.send(msg);
            } catch (RemoteException ex) {
                Log.w(TAG, "registerClient() failed", ex);
            }
        }
    }

    private void unregisterClient() {
        if (serviceBound) {
            Message msg = Message.obtain(null, TrackingService.MSG_UNREGISTER_CLIENT);
            msg.replyTo = mMessenger;
            try {
                trackingService.send(msg);
            } catch (RemoteException ex) {
                Log.w(TAG, "unregisterClient() failed", ex);
            }
        }
    }

    private void requestServiceStatus() {
        if (serviceBound) {
            Message msg = Message.obtain(null, TrackingService.MSG_GET_STATUS);
            msg.replyTo = mMessenger;
            try {
                trackingService.send(msg);
            } catch (RemoteException ex) {
                Log.w(TAG, "requestConnectionStatus() failed", ex);
            }
        }
    }

    private void startTracking() {
        if (serviceBound) {
            Message msg = Message.obtain(null, TrackingService.MSG_START_SESSION);
            msg.getData().putString("parseUserId", ParseUser.getCurrentUser().getObjectId());
            msg.getData().putString("aircraftId", aircraftId);
            msg.replyTo = mMessenger;

            try {
                trackingService.send(msg);
            } catch (RemoteException ex) {
                Log.e(TAG, "startTracking() failed", ex);
            }
        }
    }

    private void requestEndSession() {
        if (isServiceRunning()) {
            try {
                Message msg = Message.obtain(null, TrackingService.MSG_END_SESSION);
                msg.replyTo = mMessenger;
                trackingService.send(msg);
            } catch (RemoteException ex) {
                Log.e(TAG, "requestEndSession() failed", ex);
            }
        }
    }

    private void connectDevice(String deviceAddress) {
        if (serviceBound && hrmState == LeHRMonitor.READY_STATUS) {
            try {
                Message msg = Message.obtain(null, TrackingService.MSG_CONNECT_HRM);
                msg.replyTo = mMessenger;
                msg.getData().putString("deviceAddress", deviceAddress);
                trackingService.send(msg);
            } catch (RemoteException ex) {
                Log.w(TAG, "connectDeviceAddress() failed to send MSG_CONNECT", ex);
            }
        } else {
            throw new IllegalStateException("Incorrect monitor state!");
        }
    }

    private void loadSessionData(final long sessionId) {
        mCurrentSessionId = sessionId;

        final TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        text1.setText(ParseTools.getUserFullName(ParseUser.getCurrentUser()));
        final TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text2.setText("Loading...");

        Task.callInBackground(new Callable<AirSessionEntity>() {
            @Override
            public AirSessionEntity call() throws Exception {
                AirSessionDAO sessionDao = HelperFactory.getHelper().getAirSessionDao();
                return sessionDao.queryForId(sessionId);
            }
        }).onSuccess(new Continuation<AirSessionEntity, String>() {
            @Override
            public String then(Task<AirSessionEntity> task) throws Exception {
                AirSessionEntity session = task.getResult();
                onSessionLoaded();
                return session.getSyncAircraftId();
            }
        }, Task.UI_THREAD_EXECUTOR).onSuccess(new Continuation<String, Aircraft>() {
            @Override
            public Aircraft then(Task<String> task) throws Exception {
                Aircraft plane = ParseObject.createWithoutData(Aircraft.class, task.getResult());
                plane.fetch();
                return plane;
            }
        }, Task.BACKGROUND_EXECUTOR).continueWith(new Continuation<Aircraft, Object>() {
            @Override
            public Object then(Task<Aircraft> task) throws Exception {
                if (task.isFaulted()) {
                    // TODO: handle the problem
                } else if (task.isCompleted()) {
                    mPlane = task.getResult();
                    showPlaneData();
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void showPlaneData() {
        // update view
        TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text1.setText(mPlane.getName() + " :: " + mPlane.getAircraftId());
        text2.setText(ParseTools.getUserFullName(ParseUser.getCurrentUser()));

        // notify user
        Toast.makeText(TrackingActivity.this, "Plane: " + mPlane.getName(), Toast.LENGTH_SHORT).show();
    }

    private void onSessionLoaded() {
        routeOpts = new PolylineOptions()
                .color(Color.BLUE)
                .width(2 /* TODO: density! */)
                .geodesic(true);
        route = mMap.addPolyline(routeOpts);
        route.setVisible(drawTrack);

        mMap.setOnMyLocationChangeListener(this);
    }

    private void toggleControlPanel() {
        boolean shown = mControlPanelView.isShown();
        if (shown) {
            mControlPanelView.setVisibility(View.GONE);
            getSupportActionBar().hide();
        } else {
            getSupportActionBar().show();
            mControlPanelView.setVisibility(View.VISIBLE);
        }
    }

    private void openSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
    }


    private void resetHRM() {
        try {
            Message msg = Message.obtain(null, TrackingService.MSG_RESET_HRM);
            trackingService.send(msg);
        } catch (RemoteException ex) {
            Log.w(TAG, "resetHRM() failed", ex);
        }
    }


    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_message)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        performLogout();
                    }
                }).show();
    }

    private void stopTracking() {
        boolean showProgress = false;
        if (isServiceRunning()) {
            requestEndSession();
            showProgress = true;
        }
        if (showProgress) {
            finishingProgressDialog = new ProgressDialog(this);
            finishingProgressDialog.setIndeterminate(true);
            finishingProgressDialog.setCancelable(false);
            finishingProgressDialog.setMessage(getText(R.string.waiting_for_service_to_finish));
            finishingProgressDialog.show();
            finishingProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finishingProgressDialog = null;
                }
            });
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // waiting dialog is still showing. Got some network problem???
                    // try to finish service correctly
                    try {
                        if (finishingProgressDialog != null && finishingProgressDialog.isShowing()) {
                            finishingProgressDialog.dismiss();
                            finishingProgressDialog = null;
                            stopService(new Intent(TrackingActivity.this, TrackingService.class));
                            onTrackingSessionFinished(TrackingService.RESULT_FAIL, 0L, 0L);
                        }
                    } catch (Exception ex) {
                        Log.w(TAG, "stopGPSService(): failed to stop the service", ex);
                    }
                }
            }, 30 * 1000L);
        }
    }

    private Circle addMapCircle(LatLng latLng) {
        int radius = prefHelper.getInt(Constants.CONFIG_RADAR_RADIUS, Constants.DEFAULT_RADAR_RADIUS);
        return mapCircle = mMap.addCircle(
                    new CircleOptions()
                            .center(latLng)
                            .radius(radius)
                            .strokeWidth(1)
                            .strokeColor(Color.argb(180, 255, 0, 0))
                            .fillColor(Color.argb(32, 255, 0, 0))
            );
    }

    private void updateNearbyPlanes(Location loc) {
        if (loc == null)
            return;
        Map<String, Object> params = new HashMap<String, Object>();
//        int radius = prefHelper.getInt(Constants.CONFIG_RADAR_RADIUS, Constants.DEFAULT_RADAR_RADIUS);
//        params.put("d", radius);
        params.put("lat", loc.getLatitude());
        params.put("lon", loc.getLongitude());
        if (mPlane != null)
            params.put("aircraftId", mPlane.getObjectId());
        if (loc.hasAltitude())
            params.put("alt", loc.getAltitude());
        params.put("t", System.currentTimeMillis());

        ParseCloud.callFunctionInBackground("getNearbyAircrafts", params, new FunctionCallback<List<HashMap<String, Object>>>() {
            @Override
            public void done(List<HashMap<String, Object>> aircrafts, ParseException e) {
                if (e != null) {
                    Log.w(TAG, "getNearbyAircrafts() cloud code failed with exception", e);
                    refreshNearbyPlanes(Collections.EMPTY_LIST);
                } else {
                    Log.d(TAG, "getNearbyAircrafts() returned: " + aircrafts);
                    refreshNearbyPlanes(aircrafts);
                }
            }
        });
    }

    private void refreshNearbyPlanes(List<HashMap<String, Object>> planes) {

        if (planes == null)
            planes = Collections.emptyList();

        int radius = prefHelper.getInt(Constants.CONFIG_RADAR_RADIUS, Constants.DEFAULT_RADAR_RADIUS);

        // remove other planes from the map
        Iterator<Map.Entry<String, Marker>> it = markers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Marker> entry = it.next();
            entry.getValue().remove();
        }
        markers.clear();
        nearbyPlanes.clear();

        LatLng myLatLng = (lastLocation != null)
                ? new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()) : null;

        List<String> radarList = new ArrayList<String>(planes.size());
        for (Map<String, Object> planeInfo: planes) {
            String aircraftId = planeInfo.get("aircraftId").toString();
            String userId = planeInfo.get("userId").toString();
            if (mPlane.getObjectId().equals(aircraftId))
                continue;
            String sessionId = planeInfo.get("sessionId").toString();
            Date updatedAt = (Date) planeInfo.get("updatedAt");
            Map<String, Object> lastPoint = (Map<String, Object>) planeInfo.get("lastPoint");
            if (lastPoint != null) {
                Number lat = (Number) lastPoint.get("lat");
                Number lon = (Number) lastPoint.get("lon");
                Number bea = (Number) lastPoint.get("bea");
                Number vel = (Number) lastPoint.get("vel");
                Number alt = (Number) lastPoint.get("alt");
                LatLng latLng = new LatLng(lat.doubleValue(), lon.doubleValue());
                Marker marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_airplane_black))
                                .flat(true)
                                .anchor(0.5f, 0.5f)
                                .rotation(bea != null ? bea.floatValue() : 0)
                );
                markers.put(aircraftId, marker);
                nearbyPlanes.put(marker.getId(), planeInfo);

                if (myLatLng != null) {
                    double distance = SphericalUtil.computeDistanceBetween(myLatLng, latLng);
                    if (distance <= radius) {
                        // add this plane into the radarList
                        radarList.add(aircraftId);
                    }
                }
            }
        }

        if (this.radarList.size() > radarList.size()) {
            CommonTools.vibrate(TrackingActivity.this, new long[]{0, 500, 200, 200, 200, 500}, -1);
        } else if (this.radarList.size() < radarList.size()) {
            CommonTools.vibrate(TrackingActivity.this, new long[]{0, 200, 200, 500, 200, 200}, -1);
        }

        this.radarList = radarList;

        if (selectedAircraftId != null) {
            Marker marker = markers.get(selectedAircraftId);
            if (marker == null) {
                selectedAircraftId = null;
                mMapOverlay.setVisibility(View.GONE);
            } else {
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_airplane_red));
                Map<String, Object> planeInfo = nearbyPlanes.get(marker.getId());
                Map<String, Object> lastPoint = (Map<String, Object>) planeInfo.get("lastPoint");
                if (lastPoint != null) {
                    Number lat = (Number) lastPoint.get("lat");
                    Number lon = (Number) lastPoint.get("lon");
                    Number bea = (Number) lastPoint.get("bea");
                    Number vel = (Number) lastPoint.get("vel");
                    Number alt = (Number) lastPoint.get("alt");

                    try {
                        Aircraft aircraft = ParseQuery.getQuery(Aircraft.class)
                                .fromPin("planes")
                                .get(selectedAircraftId);
                        if (aircraft != null) {
                            mOverlayAircraftName.setText(aircraft.getName());
                            mOverlayCallName.setText(aircraft.getCallName());
                        }
                    } catch (Exception ex) {
                        mOverlayAircraftName.setText("Unknown");
                        mOverlayCallName.setText("N/A");
                    }

                    mOverlaySpeed.setText((vel != null ? Math.round(vel.floatValue()*3.6f) : "?") + " km/h");
                    if (lastLocation != null && lastLocation.hasAltitude() && alt!=null) {
                        int delta = Math.round(alt.floatValue() - (float) lastLocation.getAltitude());
                        if (delta > 0) {
                            mOverlayHeight.setText("+" + delta + " m");
                            mOverlayHeight.setTextColor(Color.GREEN);
                        } else if (delta < 0) {
                            mOverlayHeight.setText("-" + delta + " m");
                            mOverlayHeight.setTextColor(Color.RED);
                        } else {
                            mOverlayHeight.setText("±0 m");
                            mOverlayHeight.setTextColor(Color.BLACK);
                        }
                    } else {
                        mOverlayHeight.setText((alt != null ? alt.floatValue() : "?") + " m");
                        mOverlayHeight.setTextColor(Color.BLACK);
                    }
                    if (lastLocation != null && lat != null && lon != null) {
                        double d = SphericalUtil.computeDistanceBetween(
                                new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()),
                                new LatLng(lat.doubleValue(), lon.doubleValue()));
                        mOverlayDistance.setText(Math.round(d) + " m");
                    } else {
                        mOverlayDistance.setText("? m");
                    }
                    mMapOverlay.setVisibility(View.VISIBLE);
                }
            }
        } else {
            mMapOverlay.setVisibility(View.GONE);
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

    private void performConnect() {
        if (!requestEnableBluetooth()) {
            // this will show dialog to enable bluetooth
            return;
        }

        if (hrmState == LeHRMonitor.INITIAL_STATUS || hrmState == LeHRMonitor.DISCONNECTING_STATUS) {
            // call init first!
            Message msg = Message.obtain(null, TrackingService.MSG_INIT_HRM);
            msg.replyTo = mMessenger;
            try {
                trackingService.send(msg);
            } catch (RemoteException ex) {
                Log.w(TAG, "performConnect() failed to send MSG_INIT_HRM", ex);
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
        Message msg = Message.obtain(null, TrackingService.MSG_DISCONNECT_HRM);
        try {
            trackingService.send(msg);
        } catch (RemoteException ex) {
            Log.w(TAG, "performDisconnect() failed to send MSG_DISCONNECT_HRM", ex);
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

        dialogBuilder.setTitle(R.string.title_select_hrm);
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
                connectDevice(deviceAddress);
                alertSelectDevice.cancel();
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
        stopTracking();

        // Parse log out
        ParseUser.logOut();

        if (!openLoginActivity)
            finish();
    }

    private boolean isServiceRunning() {
        return serviceBound && mCurrentSessionId != null;
    }

    private void checkGPSEnabled() {
        if (gpsMonitor.isGPSEnabled()) {
            // GPS is available!
            return;
        }

        // Ask user to enable GPS or logout
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.title_gps_disabled)
                .setMessage(R.string.message_gps_disabled)
                .setNegativeButton(R.string.log_out_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        performLogout(true);
                    }
                })
                .setPositiveButton(R.string.go_to_gps_settings_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        gpsMonitor.openLocationSettings();
                    }
                }).show();
    }
}
