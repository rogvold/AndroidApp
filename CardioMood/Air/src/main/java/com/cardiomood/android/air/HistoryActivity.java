package com.cardiomood.android.air;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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

import com.cardiomood.android.air.data.AirSession;
import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.AircraftDAO;
import com.cardiomood.android.air.db.CardioItemDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.LocationDAO;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.AircraftEntity;
import com.cardiomood.android.air.db.entity.CardioItemEntity;
import com.cardiomood.android.air.db.entity.LocationEntity;
import com.cardiomood.android.air.tools.Constants;
import com.cardiomood.android.sync.ormlite.SyncHelper;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.ui.TouchEffect;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class HistoryActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = HistoryActivity.class.getSimpleName();
    private static final DateFormat DATE_FORMAT =
            DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    private ListView airSessionsListView;

    private ArrayAdapter<AirSessionInfo> sessionArrayAdapter;
    private List<AirSessionInfo> airSessions;

    private PreferenceHelper prefHelper;
    private SyncHelper syncHelper;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if signed in
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        // create PreferenceHelper
        prefHelper = new PreferenceHelper(this, true);
        long lastSyncDate = prefHelper.getLong(Constants.CONFIG_LAST_SYNC_TIMESTAMP, 0L);

        // create SyncHelper
        syncHelper = new SyncHelper(HelperFactory.getHelper());
        syncHelper.setUserId(ParseUser.getCurrentUser().getObjectId());
        syncHelper.setLastSyncDate(new Date(lastSyncDate));


        setContentView(R.layout.activity_history);

        airSessionsListView = (ListView) findViewById(R.id.air_sessions);
        airSessionsListView.setOnItemClickListener(this);

        // initialize sessions list
        airSessions = new ArrayList<>();
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
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_refresh:
                sync();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AirSessionInfo info = sessionArrayAdapter.getItem(position);
        Intent intent = new Intent(this, DebriefingActivity.class);
        intent.putExtra(DebriefingActivity.EXTRA_SESSION_ID, info.id);
        startActivity(intent);
        Toast.makeText(this, R.string.loading_session_data, Toast.LENGTH_LONG).show();
    }

    private void sync() {
        pDialog = new ProgressDialog(this);
        pDialog.setIndeterminate(true);
        pDialog.setMessage("Synchronizing session data...");
        pDialog.setCancelable(false);
        pDialog.show();

        // TODO: implement progress display

        Task.callInBackground(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                long syncDate = System.currentTimeMillis();
                syncHelper.synObjects(AircraftEntity.class, false, null);
                syncHelper.synObjects(AirSessionEntity.class,
                        true, new SyncCallback());
                return syncDate;
            }
        }).continueWith(new Continuation<Long, Object>() {
            @Override
            public Date then(Task<Long> task) throws Exception {
                if (task.isFaulted()) {
                    Toast.makeText(HistoryActivity.this, "Faulted", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "sync failed", task.getError());
                } else if (task.isCompleted()) {
                    prefHelper.putLong(Constants.CONFIG_LAST_SYNC_TIMESTAMP, task.getResult());
                }

                refreshSessionList();
                if (pDialog != null) {
                    pDialog.dismiss();
                }
                pDialog = null;

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

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
                    } else {
                        // unknown aircraft
                        info.planeId = -1;
                        info.planeName = "Unknown Aircraft";
                        info.planeCallName = "";
                        info.planeNumber = "";
                        info.planeType = "";
                    }
                    result.add(info);
                }
                return result;
            }
        });
    }

    private void publishSyncProgress(final CharSequence message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pDialog != null && pDialog.isShowing()) {
                    pDialog.setMessage(message);
                }
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

            itemView.setOnTouchListener(TouchEffect.FADE_ON_TOUCH);

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

    private static class SyncCallback implements SyncHelper.SyncCallback<AirSessionEntity> {

        private static final int CARDIO_CHUNK_SIZE = 3000;
        private static final int LOCATION_CHUNK_SIZE = 500;

        @Override
        public void onSaveLocally(AirSessionEntity localObject, ParseObject remoteObject) throws Exception {
            AirSessionDAO sessionDao = HelperFactory.getHelper().getAirSessionDao();
            LocationDAO locItemDao = HelperFactory.getHelper().getLocationDao();
            CardioItemDAO cardioItemDao = HelperFactory.getHelper().getCardioItemDao();

            Dao.CreateOrUpdateStatus status = sessionDao.createOrUpdate(localObject);
            if (status.isUpdated()) {
                // delete cardio items
                DeleteBuilder del = cardioItemDao.deleteBuilder();
                del.where().eq("session_id", localObject.getId());
                del.delete();

                // delete location items
                del = locItemDao.deleteBuilder();
                del.where().eq("session_id", localObject.getId());
                del.delete();
            }

            // load cardio data items
            List<ParseObject> cardioChunks = ParseQuery.getQuery("CardioDataChunk")
                    .whereEqualTo("sessionId", remoteObject.getObjectId())
                    .orderByAscending("number")
                    .find();

            long lastT = localObject.getCreationDate().getTime();
            for (ParseObject chunk: cardioChunks) {
                JSONArray rrs = chunk.getJSONArray("rrs");
                JSONArray times = chunk.getJSONArray("times");
                for (int i = 0; i < rrs.length(); i++) {
                    CardioItemEntity item = new CardioItemEntity();
                    item.setRr(rrs.getInt(i));
                    item.setBpm(Math.round(60 * (item.getRr() / 1000.0f)));
                    item.setT(times.getLong(i));
                    item.setSession(localObject);
                    cardioItemDao.create(item);
                    if (item.getT() > lastT) {
                        lastT = item.getT();
                    }
                }
            }

            // load cardio data items
            List<ParseObject> chunks = ParseQuery.getQuery("LocationDataChunk")
                    .whereEqualTo("sessionId", remoteObject.getObjectId())
                    .orderByAscending("number")
                    .find();

            for (ParseObject chunk: chunks) {
                JSONArray lat = chunk.getJSONArray("lat");
                JSONArray lon = chunk.getJSONArray("lon");
                JSONArray alt = chunk.getJSONArray("alt");
                JSONArray acc = chunk.getJSONArray("acc");
                JSONArray vel = chunk.getJSONArray("vel");
                JSONArray bea = chunk.getJSONArray("bea");
                JSONArray times = chunk.getJSONArray("times");
                for (int i = 0; i < lat.length(); i++) {
                    LocationEntity item = new LocationEntity();
                    item.setLatitude(lat.getDouble(i));
                    item.setLongitude(lon.getDouble(i));
                    if (!alt.isNull(i)) {
                        item.setAltitude(alt.getDouble(i));
                    }
                    if (!acc.isNull(i)) {
                        item.setAccuracy((float) acc.getDouble(i));
                    }
                    if (!vel.isNull(i)) {
                        item.setVelocity((float) vel.getDouble(i));
                    }
                    if (!bea.isNull(i)) {
                        item.setAccuracy((float) bea.getDouble(i));
                    }
                    item.setT(times.getLong(i));
                    item.setSession(localObject);
                    locItemDao.create(item);
                    if (item.getT() > lastT) {
                        lastT = item.getT();
                    }
                }
            }

            if (localObject.getEndDate() == 0L) {
                // update endTimestamp
                localObject.setEndDate(lastT + localObject.getCreationDate().getTime());
            }
        }

        @Override
        public void onSaveRemotely(AirSessionEntity localObject, ParseObject remoteObject) throws Exception {
            CardioItemDAO cardioItemDao = HelperFactory.getHelper().getCardioItemDao();
            LocationDAO locItemDao = HelperFactory.getHelper().getLocationDao();
            GenericRawResults<String[]> results = cardioItemDao.queryBuilder()
                    .selectColumns("_id", "rr", "t")
                    .orderBy("_id", true)
                    .where().eq("session_id", localObject.getId())
                    .queryRaw();
            if (remoteObject.getObjectId() == null) {
                // remote object is new
                remoteObject.save();
            } else {
                // remote object already exists
                // assuming the data points already up-to-date
                return;
            }

            // TODO: delete all cardio data chunks for this session!
            try {
                Iterator<String[]> it = results.iterator();
                int number = 1;
                do {
                    long firstT = -1l;
                    List<Long> t = new ArrayList<>(CARDIO_CHUNK_SIZE);
                    List<Integer> rrs = new ArrayList<>(CARDIO_CHUNK_SIZE);
                    for (int i = 0; i < CARDIO_CHUNK_SIZE && it.hasNext(); i++) {
                        if (it.hasNext()) {
                            String[] row = it.next();
                            Integer rrValue = Integer.valueOf(row[1]);
                            Long tValue = Long.valueOf(row[2]);
                            rrs.add(rrValue);
                            if (firstT >= 0) {
                                firstT = tValue;
                            }
                            t.add(tValue - firstT);
                        }
                    }
                    ParseObject chunk = ParseObject.create("CardioDataChunk");
                    chunk.put("sessionId", remoteObject.getObjectId());
                    chunk.put("rrs", rrs);
                    chunk.put("times", t);
                    chunk.put("number", number);
                    chunk.save();
                    number++;
                } while (it.hasNext());
            } finally {
                results.close();
            }

            results = locItemDao.queryBuilder()
                    .selectColumns("_id", "lat", "lon", "alt", "acc", "vel", "bea", "t")
                    .orderBy("_id", true)
                    .where().eq("session_id", localObject.getId())
                    .queryRaw();
            try {
                Iterator<String[]> it = results.iterator();
                int number = 1;
                do {
                    long firstT = -1l;
                    List<Long> t = new ArrayList<>(LOCATION_CHUNK_SIZE);
                    List<Double> lat  = new ArrayList<>(LOCATION_CHUNK_SIZE);
                    List<Double> lon  = new ArrayList<>(LOCATION_CHUNK_SIZE);
                    List<Double> alt  = new ArrayList<>(LOCATION_CHUNK_SIZE);
                    List<Double> acc  = new ArrayList<>(LOCATION_CHUNK_SIZE);
                    List<Double> vel  = new ArrayList<>(LOCATION_CHUNK_SIZE);
                    List<Double> bea  = new ArrayList<>(LOCATION_CHUNK_SIZE);
                    for (int i = 0; i < LOCATION_CHUNK_SIZE && it.hasNext(); i++) {
                        if (it.hasNext()) {
                            String[] row = it.next();
                            lat.add(Double.valueOf(row[1]));
                            lon.add(Double.valueOf(row[2]));
                            alt.add(row[3] != null ? Double.valueOf(row[3]) : null);
                            acc.add(row[4] != null ? Double.valueOf(row[4]) : null);
                            vel.add(row[5] != null ? Double.valueOf(row[5]) : null);
                            bea.add(row[6] != null ? Double.valueOf(row[6]) : null);

                            Long tValue = Long.valueOf(row[7]);
                            if (firstT >= 0) {
                                firstT = tValue;
                            }
                            t.add(tValue - firstT);
                        }
                    }

                    ParseObject chunk = ParseObject.create("LocationDataChunk");
                    chunk.put("sessionId", remoteObject.getObjectId());
                    chunk.put("lat", lat);
                    chunk.put("lon", lon);
                    chunk.put("vel", vel);
                    chunk.put("acc", acc);
                    chunk.put("alt", alt);
                    chunk.put("bea", bea);
                    chunk.put("times", t);
                    chunk.put("number", number);
                    chunk.save();
                    number++;
                } while (it.hasNext());
            } finally {
                results.close();
            }

            if (((AirSession) remoteObject).getEndDate() == 0L) {
                remoteObject.put("endDate", localObject.getEndDate());
            }
        }
    }
}
