package com.cardiomood.android.air;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.AircraftDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.AircraftEntity;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class HistoryActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final String TAG = HistoryActivity.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    private ListView airSessionsListView;

    private ArrayAdapter<AirSessionInfo> sessionArrayAdapter;
    private List<AirSessionInfo> airSessions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        airSessionsListView = (ListView) findViewById(R.id.air_sessions);
        airSessionsListView.setOnItemClickListener(this);

        // initialize planes list
        airSessions = new ArrayList<AirSessionInfo>();
        sessionArrayAdapter = new AirSessionListArrayAdapter(this, airSessions);
        airSessionsListView.setAdapter(sessionArrayAdapter);

        refreshSessionList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.acitivity_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AirSessionInfo info = sessionArrayAdapter.getItem(position);
        Intent intent = new Intent(this, DebriefingActivity.class);
        intent.putExtra(DebriefingActivity.EXTRA_SESSION_ID, info.syncId);
        startActivity(intent);
    }

    private void refreshSessionList() {
        Task.callInBackground(new Callable<List<AirSessionEntity>>() {
            @Override
            public List<AirSessionEntity> call() throws Exception {
                AirSessionDAO dao = HelperFactory.getHelper().getAirSessionDao();
                return dao.queryBuilder()
                        .orderBy("creation_timestamp", false)
                        .where().eq("sync_user_id", ParseUser.getCurrentUser().getObjectId())
                        .and().ne("deleted", true)
                        .query();
            }
        }).continueWithTask(new Continuation<List<AirSessionEntity>, Task<List<AirSessionInfo>>>() {
            @Override
            public Task<List<AirSessionInfo>> then(Task<List<AirSessionEntity>> task) throws Exception {
                if (task.isFaulted()) {
                    Toast.makeText(HistoryActivity.this, "Task failed with exception: "
                            + task.getError().getMessage(), Toast.LENGTH_SHORT).show();
                } else if (task.isCompleted()) {
                    return extractSessionInfoAsync(task.getResult());
                }
                return null;
            }
        }).continueWith(new Continuation<List<AirSessionInfo>, Object>() {
            @Override
            public Object then(Task<List<AirSessionInfo>> task) throws Exception {
                if (HistoryActivity.this.isFinishing()) {
                    return null;
                }

                if (task.isFaulted()) {
                    Toast.makeText(HistoryActivity.this, "Task failed with exception: "
                            + task.getError().getMessage(), Toast.LENGTH_SHORT).show();
                } else if (task.isCompleted()) {
                    airSessions.clear();
                    airSessions.addAll(task.getResult());
                    sessionArrayAdapter.notifyDataSetChanged();
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    public Task<List<AirSessionInfo>> extractSessionInfoAsync(final List<AirSessionEntity> sessions) {
        return Task.callInBackground(new Callable<List<AirSessionInfo>>() {
            @Override
            public List<AirSessionInfo> call() throws Exception {
                AircraftDAO aircraftDAO = HelperFactory.getHelper().getAircraftDao();
                List<AirSessionInfo> result = new ArrayList<AirSessionInfo>(sessions.size());
                for (AirSessionEntity entity: sessions) {
                    AirSessionInfo info = new AirSessionInfo();
                    info.id = entity.getId();
                    info.creationDate = entity.getCreationDate();
                    info.lastUpdated = entity.getSyncDate();
                    info.name = entity.getName();
                    info.syncId = entity.getSyncId();
                    info.planeSyncId = entity.getSyncAircraftId();
                    AircraftEntity aircraft = aircraftDAO.findBySyncId(info.planeSyncId);
                    if (aircraft != null) {
                        info.planeId = aircraft.getId();
                        info.planeName = aircraft.getName();
                        info.planeCallName = aircraft.getCallName();
                        info.planeNumber = aircraft.getAircraftId();
                        info.planeType = aircraft.getAircraftType();
                        result.add(info);
                    }
                }
                return result;
            }
        });
    }

    public class AirSessionListArrayAdapter extends ArrayAdapter<AirSessionInfo> {

        public AirSessionListArrayAdapter(Context context, List<AirSessionInfo> src) {
            super(context, R.layout.two_lines_layout, src);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View itemView = inflater.inflate(R.layout.two_lines_layout, parent, false);
            itemView.setBackgroundResource(R.drawable.list_selector_background);
            AirSessionInfo info = getItem(position);

            TextView text1 = (TextView) itemView.findViewById(android.R.id.text1);
            text1.setTypeface(null, Typeface.NORMAL);
            if (info.name == null || info.name.trim().isEmpty()) {
                text1.setText(info.planeName + " " + info.planeNumber);
            } else {
                text1.setText(info.name.trim());
            }

            TextView text2 = (TextView) itemView.findViewById(android.R.id.text2);
            text2.setText(DATE_FORMAT.format(info.creationDate));

            return itemView;
        }
    }

    public static class AirSessionInfo {
        long id;
        String syncId;
        Date creationDate;
        Date lastUpdated;
        String name;
        long planeId;
        String planeSyncId;
        String planeType;
        String planeName;
        String planeCallName;
        String planeNumber;
    }
}
