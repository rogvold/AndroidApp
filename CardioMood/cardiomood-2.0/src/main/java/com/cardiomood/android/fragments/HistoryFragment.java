package com.cardiomood.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.cardiomood.android.R;
import com.cardiomood.android.SessionDetailsActivity;
import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.db.entity.CardioItemDAO;
import com.cardiomood.android.db.entity.CardioItemEntity;
import com.cardiomood.android.db.entity.LocationDAO;
import com.cardiomood.android.db.entity.LocationEntity;
import com.cardiomood.android.db.entity.SessionDAO;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.sync.ormlite.SyncEntity;
import com.cardiomood.android.sync.ormlite.SyncHelper;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.ReachabilityTest;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by danshin on 01.11.13.
 */
public class HistoryFragment extends Fragment
        implements ListView.OnItemClickListener, AdapterView.OnItemLongClickListener, SearchView.OnQueryTextListener {

    private static final String TAG = HistoryFragment.class.getSimpleName();

    private ListView listView;
    private View root;
    private SessionsArrayAdapter listAdapter = null;
    private PreferenceHelper pHelper;
    private ProgressDialog pDialog = null;
    private ActionMode mActionMode = null;
    private String userId = null;
    private List<SessionEntity> mSessions = new ArrayList<>();
    private SessionsEndlessAdapter mEndlessAdapter;
    private PreferenceHelper prefHelper;

    // work around for 'view already has a parent...'
    private boolean initial = true;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            FlurryAgent.logEvent("action_mode_started");
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.history_context_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    FlurryAgent.logEvent("menu_delete_item_clicked");
                    if (listAdapter.getSelectedItem() >= 0)
                        deleteItem(listAdapter.getSelectedItem());
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_rename_item:
                    FlurryAgent.logEvent("menu_rename_item_clicked");
                    if (listAdapter.getSelectedItem() >= 0)
                        renameItem(listAdapter.getSelectedItem());
                    mode.finish(); // Action picked, so close the CAB
                    return true;
//                case R.id.menu_share_session:
//                    FlurryAgent.logEvent("menu_share_session_clicked");
//                    if (listAdapter.getSelectedItem() >= 0)
//                        shareSession(listAdapter.getSelectedItem());
//                    mode.finish();
//                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            FlurryAgent.logEvent("action_mode_finished");
            mActionMode = null;
            if (listAdapter != null) {
                listAdapter.setSelectedItem(-1);
                listAdapter.notifyDataSetChanged();
            }
        }
    };

    private DatabaseHelper databaseHelper = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefHelper = new PreferenceHelper(getActivity(), true);
        listAdapter = new SessionsArrayAdapter(getActivity(), mSessions);
        mEndlessAdapter = new SessionsEndlessAdapter(listAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        pHelper = new PreferenceHelper(getActivity(), true);
        root = inflater.inflate(R.layout.fragment_history, container, false);
        listView = (ListView) root.findViewById(R.id.sessionList);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        listView.setAdapter(mEndlessAdapter);
        setHasOptionsMenu(true);

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        // work around for app crash due to 'view already has a parent...' - bug in EndlessAdapter
        userId = ParseUser.getCurrentUser().getObjectId();
        if (initial) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refresh();
                    initial = false;
                }
            }, 1000);
        } else {
            refresh();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_sessions_list, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = new SearchView(getActivity());
        MenuItemCompat.setActionView(menuItem, searchView);
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getActivity().getComponentName()));

        EditText txtSearch = ((EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text));
        txtSearch.setHintTextColor(Color.DKGRAY);
        txtSearch.setTextColor(Color.WHITE);
        txtSearch.setHint("Search in history");
        searchView.setOnQueryTextListener(this);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                sync();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteItem(int i) {
        final SessionEntity session = listAdapter.getItem(i);
        Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                SessionDAO dao = DatabaseHelperFactory.getHelper().getSessionDao();
                session.setDeleted(true);
                session.setSyncDate(new Date());
                return dao.update(session);
            }
        }).continueWith(new Continuation<Integer, Object>() {
            @Override
            public Object then(Task<Integer> task) throws Exception {
                if (task.isFaulted()) {
                    Toast.makeText(getActivity(), R.string.failed_to_romove_session, Toast.LENGTH_SHORT).show();
                } else {
                    // log delete event
                    Map<String, String> args = new HashMap<String, String>();
                    args.put("sessionId", session.getId()+"");
                    args.put("sessionName", session.getName());
                    FlurryAgent.logEvent("session_deleted", args);

                    Toast.makeText(getActivity(), R.string.item_removed, Toast.LENGTH_SHORT).show();
                    listAdapter.remove(session);
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

//    public void shareSession(int i) {
//        SessionEntity session = listAdapter.getItem(i);
//        String txt = "";
//        if (session.getName() != null)
//            txt = session.getName();
//        Intent sendIntent = new Intent();
//        sendIntent.setAction(Intent.ACTION_SEND);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, txt + " \n http://www.idiophrases.com/cardiomoodTest.html");
//        sendIntent.setType("text/plain");
//        startActivity(sendIntent);
//    }

    public void renameItem(int i) {
        final SessionEntity itemSession = listAdapter.getItem(i);
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.dialog_input_text, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        userInput.setText(itemSession.getName());

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                String newName = userInput.getText() == null ? "" : userInput.getText().toString();
                                newName = newName.trim();
                                if (newName.isEmpty())
                                    newName = null;
                                onRenameSession(itemSession, newName);
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

    private void onRenameSession(SessionEntity session, String newName) {
        final String oldName = session.getName();
        final SessionEntity renamedSession = session;
        renamedSession.setName(newName);
        renamedSession.setSyncDate(new Date());
        Task.callInBackground(new Callable<SessionEntity>() {
            @Override
            public SessionEntity call() throws Exception {
                DatabaseHelperFactory.getHelper()
                        .getSessionDao()
                        .update(renamedSession);
                return renamedSession;
            }
        }).continueWith(new Continuation<SessionEntity, Object>() {
            @Override
            public Object then(Task<SessionEntity> task) throws Exception {
                if (task.isFaulted()) {
                    renamedSession.setName(oldName);
                } else if (task.isCompleted()) {
                    Toast.makeText(getActivity(), R.string.session_renamed, Toast.LENGTH_SHORT).show();
                    listAdapter.notifyDataSetChanged();

                    if (!"SYNC_ON_DEMAND".equals(pHelper.getString(ConfigurationConstants.SYNC_STRATEGY, "SYNC_WHEN_MODIFIED"))) {
                        ParseObject parseObject = SyncEntity.toParseObject(renamedSession);
                        parseObject.saveEventually();
                    }

                    FlurryAgent.logEvent("session_renamed");
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionMode == null) {
            SessionEntity session = listAdapter.getItem(position);
            if (session != null) {
                Toast.makeText(getActivity(), R.string.opening_measurement, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), SessionDetailsActivity.class);
                intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, session.getId());
                getActivity().startActivity(intent);
            }
        } else {
            view.setSelected(true);
            listAdapter.setSelectedItem(position);
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }

        super.onDestroy();
    }

    private void sync() {
        new ReachabilityTest(
            getActivity(),
            "api.parse.com",
             80,
             new ReachabilityTest.Callback() {
                 @Override
                 public void onReachabilityTestPassed() {
                    if (!NewMeasurementFragment.inProgress) {
                        performSync(getActivity());
                    } else {
                        simpleRefresh();
                    }
                 }

                 @Override
                 public void onReachabilityTestFailed() {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Server is not available.", Toast.LENGTH_SHORT).show();
                        simpleRefresh();
                    }
                 }
             }
        ).execute();
    }

    private void performSync(Context context) {
        if (context == null)
            return;

        pDialog = new ProgressDialog(context);
        pDialog.setIndeterminate(true);
        pDialog.setMessage("Synchronizing session data...");
        pDialog.setCancelable(false);
        pDialog.show();

        // TODO: implement progress display
        // TODO: do not block UI!

        Task.callInBackground(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                long syncDate = System.currentTimeMillis();
                SyncHelper syncHelper = new SyncHelper(DatabaseHelperFactory.getHelper());
                syncHelper.setUserId(ParseUser.getCurrentUser().getObjectId());
                syncHelper.setLastSyncDate(new Date(prefHelper.getLong(ConfigurationConstants.CONFIG_LAST_SYNC_TIMESTAMP + "-" + userId, 0L)));
                syncHelper.synObjects(SessionEntity.class,
                        true, new SyncCallback());
                return syncDate;
            }
        }).continueWith(new Continuation<Long, Object>() {
            @Override
            public Date then(Task<Long> task) throws Exception {
                if (task.isFaulted()) {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Faulted", Toast.LENGTH_SHORT).show();
                    }
                    Log.w(TAG, "sync failed", task.getError());
                } else if (task.isCompleted()) {
                    prefHelper.putLong(ConfigurationConstants.CONFIG_LAST_SYNC_TIMESTAMP + "-" + userId, task.getResult());
                }

                simpleRefresh();
                if (pDialog != null) {
                    pDialog.dismiss();
                }
                pDialog = null;

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void refresh() {
        if (mActionMode != null) {
            return;
        }

        // started?
        if (initial && "SYNC_ON_START".equals(pHelper.getString(ConfigurationConstants.SYNC_STRATEGY, "SYNC_WHEN_MODIFIED"))) {
            sync();
            return;
        }

        Task.callInBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return hasUpdatedSessions();
            }
        }).continueWith(new Continuation<Boolean, Object>() {
            @Override
            public Object then(Task<Boolean> task) throws Exception {
                Activity activity = getActivity();
                if (activity == null)
                    return null;
                if (!task.isFaulted() && task.isCompleted()) {
                    if ("SYNC_ON_MODIFIED".equals(pHelper.getString(ConfigurationConstants.SYNC_STRATEGY, "SYNC_WHEN_MODIFIED"))) {
                        if (task.getResult()) {
                            sync();
                            return null;
                        }
                    }
                }
                simpleRefresh();
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

    }

    private boolean hasUpdatedSessions() {
        try {
            long lastSyncTimestamp = prefHelper.getLong(ConfigurationConstants.CONFIG_LAST_SYNC_TIMESTAMP + "-" + userId, 0L);
            SessionDAO sessionDao = DatabaseHelperFactory.getHelper().getSessionDao();
            long count = sessionDao.queryBuilder()
                    .where().eq("sync_user_id", ParseUser.getCurrentUser().getObjectId())
                    .and().gt("sync_timestamp", new Date(lastSyncTimestamp))
                    .countOf();
            return count > 0;
        } catch (SQLException ex) {
            // suppress this
        }
        return false;
    }

    private void simpleRefresh() {
        listAdapter.setSelectedItem(-1);
        mEndlessAdapter.refresh();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionMode != null) {
            return false;
        }

        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = getActivity().startActionMode(mActionModeCallback);
        view.setSelected(true);
        listAdapter.setSelectedItem(position);
        listAdapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mEndlessAdapter.getFilter().filter(s);
        return false;
    }

    private class SyncCallback implements SyncHelper.SyncCallback<SessionEntity> {

        private static final int CARDIO_CHUNK_SIZE = 3000;
        private static final int LOCATION_CHUNK_SIZE = 500;

        @Override
        public void onSaveLocally(SessionEntity localObject, ParseObject remoteObject) throws Exception {
            SessionDAO sessionDao = DatabaseHelperFactory.getHelper().getSessionDao();
            LocationDAO locItemDao = DatabaseHelperFactory.getHelper().getLocationDao();
            CardioItemDAO cardioItemDao = DatabaseHelperFactory.getHelper().getCardioItemDao();

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

            if (localObject.getEndTimestamp() == null || localObject.getEndTimestamp() == 0L) {
                // update endTimestamp
                localObject.setEndTimestamp(lastT + localObject.getCreationDate().getTime());
            }
        }

        @Override
        public void onSaveRemotely(SessionEntity localObject, ParseObject remoteObject) throws Exception {
            SessionDAO sessionDao = DatabaseHelperFactory.getHelper().getSessionDao();
            CardioItemDAO cardioItemDao = DatabaseHelperFactory.getHelper().getCardioItemDao();
            LocationDAO locItemDao = DatabaseHelperFactory.getHelper().getLocationDao();
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

            long lastT = 0L;
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
                            lastT = tValue - firstT;
                            t.add(lastT);
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
                            if (tValue - firstT > lastT) {
                                lastT = tValue - firstT;
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

                if (localObject.getEndTimestamp() == null || localObject.getEndTimestamp() == 0L) {
                    localObject.setEndTimestamp(localObject.getStartTimestamp() + lastT);
                    sessionDao.update(localObject);
                    remoteObject.put("endTimestamp", localObject.getEndTimestamp());
                }
            } finally {
                results.close();
            }
        }
    }


}