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
import android.os.RemoteException;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.data.AirSession;
import com.cardiomood.android.air.data.Aircraft;
import com.cardiomood.android.air.gps.GPSMonitor;
import com.cardiomood.android.air.gps.GPSService;
import com.cardiomood.android.air.gps.GPSServiceApi;
import com.cardiomood.android.air.gps.GPSServiceListener;
import com.cardiomood.android.air.tools.Constants;
import com.cardiomood.android.air.tools.ParseTools;
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
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.parse.DeleteCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

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


public class TrackingActivity extends Activity {

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

            // register listener
            try {
                gpsService.addListener(gpsServiceListener);
            } catch (Exception ex) {
                Log.d(TAG, "onServiceConnected(): api.addListener() failed", ex);
            }

            // hide notification (it is not needed when the UI is active)
            try {
                if (!gpsService.isRunning()) {
                    gpsService.hideNotification();
                } else {
                    gpsService.showNotification();
                }
            } catch (RemoteException ex) {
                Log.d(TAG, "onServiceConnected(): api.hideNotification() failed", ex);
            }

            // check Service status and update UI
            try {
                // check whether the tracking session is started
                if (gpsService.isRunning()) {
                    gpsServiceListener.onTrackingSessionStarted(gpsService.getUserId(), gpsService.getAircraftId(), gpsService.getAirSessionId());
                } else {
                    mStartButton.setVisibility(View.VISIBLE);
                    mStopButton.setVisibility(View.GONE);
                    mStartButton.setEnabled(true);
                    mStopButton.setEnabled(false);
//                    String planeId = getIntent().getStringExtra(SELECTED_PLANE_PARSE_ID);
//                    gpsService.startTrackingSession(ParseUser.getCurrentUser().getObjectId(), planeId);
                }

                // update HR monitor info
                gpsServiceListener.onHRMStatusChanged(gpsService.getHrmAddress(), gpsService.getHrmName(), LeHRMonitor.INITIAL_STATUS, gpsService.getHrmStatus());

            } catch (RemoteException ex) {
                Log.d(TAG, "onServiceConnected(): api.isRunning() failed", ex);
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            gpsBound = false;
            mStopButton.setEnabled(false);
            gpsService = null;
        }
    };


    // view
    private MapFragment mMapFragment;
    private GoogleMap mMap;
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
    private LinearLayout mMapOverlay;
    private TextView mOverlayAircraftName;
    private TextView mOverlayCallName;
    private TextView mOverlayDistance;
    private TextView mOverlaySpeed;
    private TextView mOverlayHeight;
    private ProgressDialog finishingProgressDialog;

    // check current state
    private Timer checkStatusTimer = new Timer("check_status");
    private TimerTask checkInternetTask = null;
    private TimerTask checkGPSTask = null;
    private TimerTask checkNearbyPlanes = null;
    private long lastLocationUpdate = 0;
    private Location lastLocation;

    // objects on map
    private Circle mapCircle;
    private Map<String, Marker> markers = new HashMap<String, Marker>();
    private Map<String, Map<String, Object>> nearbyPlanes = new HashMap<String, Map<String, Object>>();
    private String selectedAircraftId = null;
    private GestureDetector overlayGestureDetector;

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
                                stopGPSService();
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
                startTracking();
            }
        });

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

        mMapOverlay = (LinearLayout) findViewById(R.id.map_overlay);
        mOverlayAircraftName = (TextView) findViewById(R.id.overlay_aircraft_name);
        mOverlayCallName = (TextView) findViewById(R.id.overlay_call_name);
        mOverlayDistance = (TextView) findViewById(R.id.overlay_distance);
        mOverlayHeight = (TextView) findViewById(R.id.overlay_height);
        mOverlaySpeed = (TextView) findViewById(R.id.overlay_speed);

        overlayGestureDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

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
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return true;
            }
        });
        overlayGestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
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
        mMapOverlay.setOnTouchListener(new TouchEffect() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return overlayGestureDetector.onTouchEvent(event);
            }
        });

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
        Intent intent = new Intent(this, GPSService.class);
        startService(intent);

        // Bind GPSService
        bindService(intent, gpsConnection, Context.BIND_AUTO_CREATE);

        // initialize mapFragment
        if (mMapFragment == null)
            mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

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
            }
        }
    }



    @Override
    protected void onStop() {
        super.onStop();

        if (gpsBound) {
            // remove listener
            try {
                gpsService.removeListener(gpsServiceListener);
            } catch (RemoteException ex) {
                Log.d(TAG, "onStop() -> remove listener failed", ex);
            }

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
    protected void onPause() {
        super.onPause();

        checkInternetTask.cancel();
        checkGPSTask.cancel();
        checkNearbyPlanes.cancel();
        checkInternetTask = null;
        checkGPSTask = null;
        checkNearbyPlanes = null;
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
                            try {
                                if (gpsBound && gpsService != null) {
                                    if (gpsService.isRunning()) {
                                        mAltitudeView.setText("No GPS");
                                        mSpeedView.setText("No GPS");
                                    }
                                }
                            } catch (RemoteException ex) {
                                // suppress this!
                            }
                        }
                    });
                }
            }
        };

        checkNearbyPlanes = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (gpsBound && gpsService != null) {
                        if (gpsService.isRunning()) {
                            updateNearbyPlanes(lastLocation);
                        }
                    }
                } catch (RemoteException ex) {
                    // suppress this!
                }
            }
        };

        checkStatusTimer.schedule(checkInternetTask, 500, 10000);
        checkStatusTimer.schedule(checkGPSTask, 1000, 10000);
        checkStatusTimer.schedule(checkNearbyPlanes, 500, 3000);

        if (gpsBound) {
            // add listener
            try {
                gpsService.addListener(gpsServiceListener);
            } catch (RemoteException ex) {
                Log.d(TAG, "onStart() -> add listener failed", ex);
            }

            // hide notification
            try {
                if (!gpsService.isRunning()) {
                    gpsService.hideNotification();
                }
            } catch (RemoteException ex) {
                Log.d(TAG, "onStart() -> hide notification failed", ex);
            }
        }


        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            // Google Play Services available
            checkGPSEnabled();
        } else {
            Toast.makeText(this, "Google Play Services are not available. " +
                    "Some features will be disabled.", Toast.LENGTH_SHORT).show();
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
        if (gpsBound) {
            try {
                if (gpsService.isRunning()) {
                    super.onBackPressed();
                } else {
                    startActivity(new Intent(this, PlanesActivity.class));
                    finish();
                }
            } catch (RemoteException ex) {
                // suppress this
            }
        }
    }

    private void openSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
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
            }
        }

        // load plane from Parse
        ParseQuery query = ParseQuery.getQuery(Aircraft.class);
        query.getInBackground(planeId, new GetCallback<Aircraft>() {
            @Override
            public void done(Aircraft parseObject, ParseException e) {
                if (e == null) {
                    onPlaneLoaded(parseObject);
                } else {

                    try {
                        if (!gpsBound || !gpsService.isRunning()) {
                            Toast.makeText(TrackingActivity.this, "Plane not found.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(TrackingActivity.this, PlanesActivity.class));
                            finish();
                        }
                    } catch (RemoteException ex) {
                        // suppress this
                    }
                }
            }
        });
    }

    private void startTracking() {
        mStartButton.setEnabled(false);
        String planeId = getIntent().getStringExtra(SELECTED_PLANE_PARSE_ID);
        if (planeId != null) {
            try {
                gpsService.startTrackingSession(ParseUser.getCurrentUser().getObjectId(), planeId);
            } catch (RemoteException ex) {
                mStartButton.setEnabled(true);
            }
        } else {
            mStartButton.setEnabled(true);
        }
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
            finishingProgressDialog = new ProgressDialog(this);
            finishingProgressDialog.setIndeterminate(true);
            finishingProgressDialog.setCancelable(false);
            finishingProgressDialog.setMessage(getText(R.string.waiting_for_service_to_finish));
            finishingProgressDialog.show();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (finishingProgressDialog != null && finishingProgressDialog.isShowing()) {
                            finishingProgressDialog.dismiss();
                            stopService(new Intent(TrackingActivity.this, GPSService.class));
                            gpsServiceListener.onTrackingSessionFinished();
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
                            .strokeColor(Color.argb(180, 0, 0, 255))
                            .fillColor(Color.parseColor("#200084d3"))
            );
    }

    private void updateNearbyPlanes(Location loc) {
        if (loc == null)
            return;
        Map<String, Object> params = new HashMap<String, Object>();
        int radius = prefHelper.getInt(Constants.CONFIG_RADAR_RADIUS, Constants.DEFAULT_RADAR_RADIUS);
        params.put("d", radius);
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

        if (nearbyPlanes.size() > planes.size()) {
            CommonTools.vibrate(TrackingActivity.this, new long[]{0, 500, 200, 200, 200, 500}, -1);
        } else if (nearbyPlanes.size() < planes.size()) {
            CommonTools.vibrate(TrackingActivity.this, new long[]{0, 200, 200, 500, 200, 200}, -1);
        }

        // remove other planes from the map
        Iterator<Map.Entry<String, Marker>> it = markers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Marker> entry = it.next();
            entry.getValue().remove();
        }
        markers.clear();
        nearbyPlanes.clear();

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
                Marker marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(new LatLng(lat.doubleValue(), lon.doubleValue()))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_airplane_black))
                                .flat(true)
                                .anchor(0.5f, 0.5f)
                                .rotation(bea != null ? bea.floatValue() : 0)
                );
                markers.put(aircraftId, marker);
                nearbyPlanes.put(marker.getId(), planeInfo);
            }
        }

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

    private final GPSServiceListener.Stub gpsServiceListener =  new GPSServiceListener.Stub() {

        @Override
        public void onLocationChanged(final Location location) throws RemoteException {
            final long t = System.currentTimeMillis();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lastLocationUpdate = t;
                    lastLocation = location;
                    if (location.hasSpeed())
                        mSpeedView.setText(String.format("%.2f km/h", location.getSpeed()*3.6f));
                    if (location.hasAltitude())
                        mAltitudeView.setText(String.format("%d m", (int) location.getAltitude()));
                    else
                        mAltitudeView.setText("N/A");

                    if (mMap != null) {
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
                        mMap.animateCamera(cameraUpdate);
                    }
                }
            });

        }

        @Override
        public void onHeartRateChanged(final int heartRate) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mHeartRateView.setText(heartRate + " bpm");
                }
            });
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
                        mStartButton.setVisibility(View.GONE);
                        mStopButton.setVisibility(View.VISIBLE);
                        mStartButton.setEnabled(false);
                        mStopButton.setEnabled(true);
                        loadPlaneData();
                        if (!TrackingActivity.this.isFinishing())
                            Toast.makeText(TrackingActivity.this, "Tracking session is running. SessionId="
                                    + airSessionId, Toast.LENGTH_SHORT).show();

                        if (gpsService != null && gpsBound) {
                            gpsService.showNotification();
                        }
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
                            mStopButton.setEnabled(false);
                            startActivity(new Intent(TrackingActivity.this, LoginActivity.class));
                            TrackingActivity.this.finish();
                        }
                    } catch(Exception ex) {
                        // suppress this
                    }
                }
            });

        }

        @Override
        public void onHRMStatusChanged(final String address, final String name, final int oldStatus, final int newStatus) throws RemoteException {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (newStatus == LeHRMonitor.CONNECTED_STATUS) {
                        mHRMonitorStatusView.setText(getText(R.string.hrm_connected) + " " + address);
                        mConnectHRMonitorButton.setVisibility(View.GONE);
                        mHeartRateView.setVisibility(View.VISIBLE);
                        mDeviceNameView.setText(name == null ? getText(R.string.hrm_no_name) : name);
                        mConnectHRMonitorButton.setEnabled(true);
                    }
                    if (newStatus == LeHRMonitor.CONNECTING_STATUS) {
                        mHRMonitorStatusView.setText(R.string.hrm_connecting);
                        mDeviceNameView.setText(name == null ? "No name" : name);
                        mConnectHRMonitorButton.setEnabled(false);
                    }
                    if (newStatus == LeHRMonitor.DISCONNECTING_STATUS) {
                        mHRMonitorStatusView.setText(R.string.hrm_disconnected);
                        mConnectHRMonitorButton.setVisibility(View.VISIBLE);
                        mDeviceNameView.setText(name == null ? getText(R.string.hrm_no_name) : name);
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
                }
            });

            if (oldStatus == LeHRMonitor.CONNECTED_STATUS) {
                // monitor is disconnected
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mHRMonitorStatusView.setText(R.string.hrm_not_connected);
                        mHeartRateView.setVisibility(View.GONE);
                        mConnectHRMonitorButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    };
}
