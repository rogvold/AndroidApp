package com.cardiomood.android.air;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.LocationDAO;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.LocationEntity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class DebriefingActivity extends ActionBarActivity {

    public static final String EXTRA_SESSION_ID = "com.cardiomood.android.air.EXTRA_SESSION_ID";

    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private long sessionId;

    private volatile AirSessionEntity mSession;

    private AirSessionDAO sessionDao;
    private LocationDAO pointDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debriefing);

        sessionId = getIntent().getLongExtra(EXTRA_SESSION_ID, -1L);
        if (sessionId == -1L) {
            Toast.makeText(this, "Provide a valid sessionId", Toast.LENGTH_SHORT).show();
            finish();
        }

        // init Google Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        if (map != null) {
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        try {
            sessionDao = HelperFactory.getHelper().getAirSessionDao();
            pointDao = HelperFactory.getHelper().getLocationDao();
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to initialize DAO-objects", Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
            finish();
        }

        loadSessionData();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_debriefing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_rename:
                showRenameDialog();
                return true;
//            case R.id.menu_refresh:
//                loadSessionDataInBackground(sessionId);
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadSessionData() {
        Task.callInBackground(new Callable<List<LocationEntity>>() {
            @Override
            public List<LocationEntity> call() throws Exception {
                AirSessionEntity sessionEntity = sessionDao.queryForId(sessionId);
                DebriefingActivity.this.mSession = sessionEntity;

                if (sessionEntity != null) {
                    return pointDao.queryBuilder()
                            .orderBy("t", true)
                            .where().eq("session_id", sessionId)
                            .query();
                }
                return null;
            }

        }).onSuccess(new Continuation<List<LocationEntity>, Object>() {
            @Override
            public Object then(Task<List<LocationEntity>> listTask) throws Exception {
                onSessionRenamed();
                List<LocationEntity> points = listTask.getResult();
                PolylineOptions opt = new PolylineOptions()
                        .width(3)
                        .color(Color.BLUE);
                if (points.size() > 0) {
                    for (int i = 1; i < points.size(); i++) {
                        opt.add(
                                new LatLng(points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude()),
                                new LatLng(points.get(i).getLatitude(), points.get(i).getLongitude())
                        );
                    }
                    drawRoute(
                            opt,
                            new LatLng(
                                    points.get(points.size() - 1).getLatitude(),
                                    points.get(points.size() - 1).getLongitude()
                            )
                    );
                } else {
                    drawRoute(opt, null);
                }

                return null;
            }
        }).continueWith(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {
                if (task.isFaulted()) {
                    Toast.makeText(DebriefingActivity.this, "Failed to load data due to exception!\n"
                            + task.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void drawRoute(final PolylineOptions opt, final LatLng zoomPoint) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (opt.getPoints().size() < 2) {
                    Toast.makeText(DebriefingActivity.this, "This session is empty.", Toast.LENGTH_SHORT).show();
                }
                if (map != null) {
                    map.addPolyline(opt);
                    if (zoomPoint != null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(zoomPoint, 13));
                    }
                }
            }
        });
    }

    private void showRenameDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_input_text, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        userInput.setText(mSession.getName());

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String newName = userInput.getText() == null ? "" : userInput.getText().toString();
                                newName = newName.trim();
                                if (newName.isEmpty())
                                    newName = null;
                                renameSessionInBackground(mSession, newName);
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }
                )
                .setTitle(R.string.rename_session);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    protected void renameSessionInBackground(AirSessionEntity session, String newName) {
        try {
            sessionDao.refresh(session);
            session.setName(newName);
            session.setSyncDate(new Date());
            sessionDao.update(session);
            showToast("Session renamed");
            onSessionRenamed();
        } catch (Exception ex) {
            showToast("Failed to rename session");
        }
    }

    protected void showToast(final CharSequence message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DebriefingActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void onSessionRenamed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSession != null) {
                    String name = mSession.getName();
                    if (name == null || name.trim().isEmpty()) {
                        setTitle(R.string.untitled_flight);
                    } else {
                        setTitle(name.trim());
                    }
                }
            }
        });
    }
}
