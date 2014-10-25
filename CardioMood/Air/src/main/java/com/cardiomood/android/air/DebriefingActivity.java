package com.cardiomood.android.air;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.DataPointDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.DataPointEntity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class DebriefingActivity extends Activity {

    public static final String EXTRA_SESSION_ID = "com.cardiomood.android.air.EXTRA_SESSION_ID";

    private MapFragment mapFragment;
    private GoogleMap map;
    private String sessionSyncId;

    private volatile AirSessionEntity mSession;

    private AirSessionDAO sessionDao;
    private DataPointDAO pointDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debriefing);

        sessionSyncId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        if (sessionSyncId == null) {
            Toast.makeText(this, "Provide a valid sessionSyncId", Toast.LENGTH_SHORT).show();
            finish();
        }

        // init Google Map
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        try {
            sessionDao = HelperFactory.getHelper().getAirSessionDao();
            pointDao = HelperFactory.getHelper().getDataPointDao();
        } catch (Exception ex) {
            Toast.makeText(this, "Faile to initialize DAO-objects", Toast.LENGTH_SHORT).show();
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
        Task.callInBackground(new Callable<List<DataPointEntity>>() {
            @Override
            public List<DataPointEntity> call() throws Exception {
                AirSessionEntity sessionEntity = sessionDao.findBySyncId(sessionSyncId);
                DebriefingActivity.this.mSession = sessionEntity;

                if (sessionEntity != null) {
                    return pointDao.queryBuilder()
                            .orderBy("creation_timestamp", true)
                            .where().eq("sync_session_id", sessionSyncId)
                            .query();
                }
                return null;
            }

        }).onSuccess(new Continuation<List<DataPointEntity>, Object>() {
            @Override
            public Object then(Task<List<DataPointEntity>> listTask) throws Exception {
                onSessionRenamed();
                List<DataPointEntity> points = listTask.getResult();
                PolylineOptions opt = new PolylineOptions()
                        .width(3)
                        .color(Color.BLUE);
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
                if (map != null) {
                    map.addPolyline(opt);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(zoomPoint, 13));
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
                String name = mSession.getName();
                if (name == null || name.trim().isEmpty()) {
                    setTitle(R.string.untitled_flight);
                } else {
                    setTitle(name.trim());
                }
            }
        });
    }
}
