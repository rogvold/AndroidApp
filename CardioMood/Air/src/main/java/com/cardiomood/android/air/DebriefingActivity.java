package com.cardiomood.android.air;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.DataPointEntity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class DebriefingActivity extends Activity {

    public static final String EXTRA_SESSION_ID = "com.cardiomood.android.air.EXTRA_SESSION_ID";

    private MapFragment mapFragment;
    private GoogleMap map;
    private long sessionId;

    private volatile AirSessionEntity sessionEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debriefing);

        sessionId = getIntent().getLongExtra(EXTRA_SESSION_ID, -1L);
        if (sessionId < 0) {
            Toast.makeText(this, "Provide a valid sessionId", Toast.LENGTH_SHORT).show();
            finish();
        }

        // init Google Map
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    private void loadSessionData() {
        Task.callInBackground(new Callable<List<DataPointEntity>>() {
            @Override
            public List<DataPointEntity> call() throws Exception {
                AirSessionDAO sessionDao = HelperFactory.getHelper().getAirSessionDao();
                AirSessionEntity sessionEntity = sessionDao.queryForId(sessionId);
                DebriefingActivity.this.sessionEntity = sessionEntity;

                if (sessionEntity != null) {
                    RuntimeExceptionDao<DataPointEntity, Long> dao = HelperFactory.getHelper().getRuntimeExceptionDao(DataPointEntity.class);
                    return dao.queryBuilder()
                            .orderBy("creation_timestamp", true)
                            .where().eq("session_id", sessionId)
                            .query();
                }
                return null;
            }

        }).continueWith(new Continuation<List<DataPointEntity>, Object>() {
            @Override
            public Object then(Task<List<DataPointEntity>> listTask) throws Exception {
                if (listTask.isFaulted()) {
                    Toast.makeText(DebriefingActivity.this, "Failed to load data due to exception!\n"+listTask.getError().getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                } else if (listTask.isCompleted()) {
                     drawRoute(listTask.getResult());
                }
                return null;
            }
        });
    }

    private void drawRoute(final List<DataPointEntity> points) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (map != null) {
                    for (int i = 1; i < points.size(); i++) {
                        Polyline line = map.addPolyline(
                                new PolylineOptions()
                                        .add(
                                                new LatLng(points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude()),
                                                new LatLng(points.get(i).getLatitude(), points.get(i).getLongitude())
                                        )
                                        .width(5)
                                        .color(Color.BLUE)
                        );
                    }
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(
                                    points.get(points.size() - 1).getLatitude(),
                                    points.get(points.size() - 1).getLongitude()
                            ),
                            13
                    ));
                }
            }
        });
    }
}
