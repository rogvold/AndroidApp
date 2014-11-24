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

import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.AircraftDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.AircraftEntity;
import com.cardiomood.android.air.tools.Constants;
import com.cardiomood.android.sync.ormlite.SyncHelper;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.ui.TouchEffect;
import com.google.gson.Gson;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;


public class HistoryActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = HistoryActivity.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    private static final Gson GSON = new Gson();

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
        intent.putExtra(DebriefingActivity.EXTRA_SESSION_ID, info.syncId);
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
//                syncHelper.synObjects(AirSessionEntity.class,
//                        true, new SyncHelper.SyncCallback<AirSessionEntity>() {
//                            @Override
//                            public void onSaveLocally(AirSessionEntity localObject, ParseObject remoteObject) throws Exception {
//                                try {
//                                    if (localObject.getId() != null) {
//                                        // already exists...
//                                        if (remoteObject.getBoolean("deleted")) {
//                                            return;
//                                        }
//                                        if (localObject.isDeleted() && remoteObject.getBoolean("deleted")) {
//                                            return;
//                                        }
//                                        // remote object was recovered (un-deleted)
//                                    }
//                                    DataPointDAO pointDao = HelperFactory.getHelper().getDataPointDao();
//                                    // delete old points first!
//                                    Log.d(TAG, "SyncCallback.onSaveLocally() deleting points for session " + localObject.getSyncId());
//                                    DeleteBuilder<DataPointEntity, Long> del = pointDao.deleteBuilder();
//                                    del.where().eq("sync_session_id", localObject.getSyncId());
//                                    del.delete();
//
//                                    publishSyncProgress("Downloading data for session " + localObject.getSyncId());
//                                    ParseQuery<ParseObject> parseQuery = ParseQuery.getQuery("AirSessionPoint")
//                                            .whereEqualTo("sessionId", localObject.getSyncId())
//                                            .orderByAscending("t");
//                                    List<ParseObject> remoteObjects = ParseTools.findAllParseObjects(parseQuery);
//                                    Log.d(TAG, "SyncCallback.onSaveLocally() saving data points for session: " + remoteObjects.size());
//
//                                    for (ParseObject point : remoteObjects) {
//                                        DataPointEntity entity = SyncEntity.fromParseObject(point, DataPointEntity.class);
//                                        entity.setSync(true);
//                                        pointDao.create(entity);
//                                    }
//
//                                    if (remoteObjects.isEmpty()) {
//                                        localObject.setDeleted(true);
//                                        localObject.setSyncDate(new Date());
//                                    }
//                                } catch (Exception ex) {
//                                    Log.e(TAG, "onSaveLocally() failed with exception", ex);
//                                    throw new SyncException(ex);
//                                }
//                            }
//
//                            @Override
//                            public void onSaveRemotely(AirSessionEntity localObject, ParseObject remoteObject) throws Exception {
//                                // submit data points that don't have "is_sync = true"
//                                // Assuming local object already has sync_id
//                                publishSyncProgress("Uploading data for session " + localObject.getSyncId());
//                                try {
//                                    DataPointDAO dao = HelperFactory.getHelper().getDataPointDao();
//                                    List<DataPointEntity> items = dao.queryBuilder()
//                                            .orderBy("_id", true)
//                                            .where().eq("sync_session_id", localObject.getSyncId())
//                                            .and().ne("is_sync", true)
//                                            .query();
//                                    Iterator<DataPointEntity> it = items.iterator();
//                                    while (it.hasNext()) {
//                                        List<DataPointEntity> chunk = new ArrayList<DataPointEntity>(50);
//                                        for (int j=0; j<50 && it.hasNext(); j++) {
//                                            chunk.add(it.next());
//                                        }
//
//                                        Map<String, Object> params = new HashMap<String, Object>();
//                                        params.put("sessionId", localObject.getSyncId());
//                                        params.put("points", new JSONArray(GSON.toJson(chunk)));
//
//                                        // send to parse
//                                        ParseCloud.callFunction("saveNewPoints", params);
//
//                                        // update sync flag
//                                        for (DataPointEntity dp: chunk) {
//                                            dp.setSync(true);
//                                            dao.update(dp);
//                                        }
//
//                                    }
//                                } catch (Exception ex) {
//                                    Log.e(TAG, "onSaveRemotely() failed with exception", ex);
//                                    throw new SyncException(ex);
//                                }
//                            }
//                        });
                return syncDate;
            }
        }).continueWith(new Continuation<Long, Object>() {
            @Override
            public Date then(Task<Long> task) throws Exception {
                if (task.isFaulted()) {
                    Toast.makeText(HistoryActivity.this, "Faulted", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "sync failed", task.getError());
                } else if (task.isCompleted()) {
                    //prefHelper.putLong(Constants.CONFIG_LAST_SYNC_TIMESTAMP, task.getResult());
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
}
