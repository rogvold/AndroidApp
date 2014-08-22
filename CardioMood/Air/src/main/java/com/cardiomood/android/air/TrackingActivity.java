package com.cardiomood.android.air;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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
import com.cardiomood.android.air.gps.GPSServiceApi;
import com.cardiomood.android.air.gps.GPSServiceListener;
import com.cardiomood.android.air.tools.ParseTools;
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

import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class TrackingActivity extends Activity {

    public static final String SELECTED_PLANE_PARSE_ID = "com.cardiomood.android.air.extra.SELECTED_PLANE_PARSE_ID";
    public static final String GET_PLANE_FROM_SERVICE = "com.cardiomood.android.air.extra.GET_PLANE_FROM_SERVICE";

    private static final String TAG = TrackingActivity.class.getSimpleName();

    private GPSMonitor gpsMonitor;

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
        }

        return super.onOptionsItemSelected(item);
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
