package com.cardiomood.android.air;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.data.AirSession;
import com.cardiomood.android.air.data.Aircraft;
import com.cardiomood.android.air.gps.GPSMonitor;
import com.cardiomood.android.air.gps.GPSService;
import com.cardiomood.android.air.tools.ParseTools;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.parse.DeleteCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class TrackingActivity extends Activity implements LocationListener {

    public static final String SELECTED_PLANE_PARSE_ID = "com.cardiomood.android.air.extra.SELECTED_PLANE_PARSE_ID";
    public static final String GET_PLANE_FROM_SERVICE = "com.cardiomood.android.air.extra.GET_PLANE_FROM_SERVICE";

    private static final String TAG = TrackingActivity.class.getSimpleName();

    private GPSMonitor gpsMonitor;

    // current state variables
    private Aircraft mPlane = null;
    private volatile AirSession mAirSession = null;

    private GPSService gpsService;
    private boolean gpsBound;
    private ServiceConnection gpsConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            GPSService.LocalBinder binder = (GPSService.LocalBinder) service;
            gpsService = binder.getService();
            gpsBound = true;

            gpsService.hideNotification();

            loadPlaneData();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            gpsBound = false;
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
            performExit();
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

        gpsMonitor = new GPSMonitor(this, this);
        Location lastLocation = gpsMonitor.getLastKnownLocation();
        if (lastLocation != null) {
            // setup initial map coordinated
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 14));
        }

        mStopButton = (Button) findViewById(R.id.stop_button);
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
                                performExit();
                            }
                        }).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");

        if (gpsMonitor != null && gpsMonitor.isRunning()) {
            gpsMonitor.stop();
        }

        if (mPlane != null) {
            mPlane.unpinInBackground(new DeleteCallback() {
                @Override
                public void done(ParseException e) {
                    if (e != null) {
                        Log.w(TAG, "failed to unpin plane", e);
                    }
                }
            });
        }

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (gpsMonitor.isGPSEnabled() && !gpsMonitor.isRunning())
            gpsMonitor.start();

        // Bind GPSService
        Intent intent = new Intent(this, GPSService.class);
        bindService(intent, gpsConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unbind GPSService if bound
        if (gpsBound) {
            if (GPSService.isServiceStarted && gpsService.getAirSession() != null)
                gpsService.showNotification();
            unbindService(gpsConnection);
        }

        if (gpsMonitor.isRunning())
            gpsMonitor.stop();
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
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadPlaneData() {
        final TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        text1.setText(ParseTools.getUserFullName(ParseUser.getCurrentUser()));
        final TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text2.setText("Loading...");

        boolean getPlaneFromService = getIntent().getBooleanExtra(GET_PLANE_FROM_SERVICE, false);
        if (getPlaneFromService) {
            Aircraft plane = gpsService.getAircraft();
            if (plane != null) {
                onPlaneLoaded(plane);
                return;
            }
        }

        // load plane from Parse
        String planeId = getIntent().getStringExtra(SELECTED_PLANE_PARSE_ID);
        ParseQuery.getQuery(Aircraft.class)
                .fromPin()
                .getInBackground(planeId, new GetCallback<Aircraft>() {
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
        if (gpsService.getAircraft() == null)
            gpsService.setAircraft(plane);

        // update view
        TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text1.setText(mPlane.getName() + " :: " + mPlane.getAircraftId());
        text2.setText(ParseTools.getUserFullName(ParseUser.getCurrentUser()));

        // notify user
        Toast.makeText(TrackingActivity.this, "Plane: " + plane.getString("name"), Toast.LENGTH_SHORT).show();

        // create tracking session if not created
        if (gpsService.getAirSession() == null) {
            createTrackingSession();
        } else {
            onTrackingSessionCreated(gpsService.getAirSession());
        }
    }

    private void createTrackingSession() {
        Task.callInBackground(new Callable<AirSession>() {

            @Override
            public AirSession call() throws Exception {
                AirSession airSession = ParseObject.create(AirSession.class);
                airSession.setAircraftId(mPlane.getObjectId());
                airSession.setUserId(ParseUser.getCurrentUser().getObjectId());
                airSession.save();
                return airSession;
            }
        }).continueWith(new Continuation<AirSession, AirSession>() {

            @Override
            public AirSession then(Task<AirSession> task) throws Exception {
                if (task.isCancelled()) {
                    // the task was cancelled
                } else if (task.isFaulted()) {
                    // error: something went wrong
                    Exception ex = task.getError();
                    Log.e(TAG, "createSession failed", ex);
                    Toast.makeText(TrackingActivity.this,
                            "Real time monitoring is not possible.", Toast.LENGTH_SHORT).show();
                } else if (task.isCompleted()) {
                    // tracking session created
                    onTrackingSessionCreated(task.getResult());
                }
                return mAirSession;
            }
        });
    }

    private void onTrackingSessionCreated(AirSession session) {
        mAirSession = session;
        if (gpsService.getAirSession() == null)
            gpsService.setAirSession(session);
        if (!gpsService.isRunning()) {
            startTracking();
        }
    }

    private void startTracking() {
        // start tracking
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!gpsService.isRunning())
                    gpsService.start();
            }
        });
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

    private void performLogout() {
        performLogout(true);
    }

    private void performExit() {
        if (gpsBound) {
            if (gpsService.isRunning()) {
                gpsService.close();
            }
        }
        // ... and also stop the service
        Intent intent = new Intent(this, GPSService.class);
        stopService(intent);

        startActivity(new Intent(this, PlanesActivity.class));
        finish();
    }

    private void performLogout(boolean openLoginActivity) {
        // close GPSService
        if (gpsBound) {
            if (gpsService.isRunning()) {
                gpsService.close();
            }
        }
        // ... and also stop the service
        Intent intent = new Intent(this, GPSService.class);
        stopService(intent);

        ParseUser.logOut();
        if (openLoginActivity)
            startActivity(new Intent(this, LoginActivity.class));
        finish();
    }



    @Override
    public void onLocationChanged(Location location) {
        if (location.hasSpeed())
            mSpeedView.setText(String.format("%.2f km/h", location.getSpeed()*3.6f));
        if (location.hasAltitude())
            mAltitudeView.setText(String.format("%d m", (int) location.getAltitude()));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.i(TAG, "onStatusChanged(): s=" + s);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.i(TAG, "onProviderEnabled(): s="+s);
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.i(TAG, "onProviderDisabled(): s="+s);
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
                        performExit();
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
}
