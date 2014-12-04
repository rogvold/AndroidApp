package com.cardiomood.android.mipt.fragments;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.mipt.R;
import com.cardiomood.android.mipt.SessionViewActivity;
import com.cardiomood.android.mipt.db.CardioItemDAO;
import com.cardiomood.android.mipt.db.CardioSessionDAO;
import com.cardiomood.android.mipt.db.HelperFactory;
import com.cardiomood.android.mipt.db.entity.CardioItemEntity;
import com.cardiomood.android.mipt.db.entity.CardioSessionEntity;
import com.cardiomood.android.mipt.parse.CardioSession;
import com.cardiomood.android.mipt.tools.Constants;
import com.cardiomood.android.sync.ormlite.SyncHelper;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.filter.PisarukArtifactFilter;
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

/**
 * A fragment representing a list of Items.
 * <p/>
 */
public class HistoryFragment extends ListFragment
        implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private static final String TAG = HistoryFragment.class.getSimpleName();

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    private ArrayAdapter<CardioSessionEntity> mSessionAdapter;
    private List<CardioSessionEntity> mCardioSessions = new ArrayList<CardioSessionEntity>();

    private PreferenceHelper mPrefHelper;
    private ProgressDialog pDialog = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HistoryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mPrefHelper = new PreferenceHelper(getActivity(), true);

        mSessionAdapter = new CardioSessionArrayAdapter(getActivity(), mCardioSessions);
        setListAdapter(mSessionAdapter);

        setHasOptionsMenu(true);
        refreshSessionList(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_history, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = new SearchView(getActivity());
        MenuItemCompat.setActionView(menuItem, searchView);
//        searchView.setSearchableInfo(
//                searchManager.getSearchableInfo(getActivity().getComponentName()));

        EditText txtSearch = ((EditText) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text));
        txtSearch.setHintTextColor(Color.DKGRAY);
        txtSearch.setTextColor(Color.WHITE);
        txtSearch.setHint("Search in history");

        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);

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


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        CardioSessionEntity entity = mSessionAdapter.getItem(position);
        Intent intent = new Intent(getActivity(), SessionViewActivity.class);
        intent.putExtra(SessionViewActivity.EXTRA_SESSION_ID, entity.getId());
        startActivity(intent);
    }

    private void refreshSessionList(String query) {
        final String q = (query == null || query.trim().isEmpty())
                ? "" : query.trim();
        Task.callInBackground(new Callable<List<CardioSessionEntity>>() {
            @Override
            public List<CardioSessionEntity> call() throws Exception {
                CardioSessionDAO dao = HelperFactory.getHelper().getCardioSessionDao();
                return dao.queryBuilder()
                        .orderBy("creation_timestamp", false)
                        .where().eq("sync_user_id", ParseUser.getCurrentUser().getObjectId())
                        .and().ne("deleted", true)
                        .and().ne("end_timestamp", 0L)
                        .and().like("name", "%" + q + "%")
                        .query();
            }
        }).continueWith(new Continuation<List<CardioSessionEntity>, Object>() {
            @Override
            public Object then(Task<List<CardioSessionEntity>> task) throws Exception {
                if (HistoryFragment.this.getActivity() == null) {
                    return null;
                }
                if (HistoryFragment.this.getActivity().isFinishing()) {
                    return null;
                }
                if (task.isFaulted()) {
                    Toast.makeText(HistoryFragment.this.getActivity(), "Task failed with exception: "
                            + task.getError().getMessage(), Toast.LENGTH_SHORT).show();
                } else if (task.isCompleted()) {
                    mCardioSessions.clear();
                    mCardioSessions.addAll(task.getResult());
                    mSessionAdapter.notifyDataSetChanged();
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    public void sync() {
        pDialog = new ProgressDialog(getActivity());
        pDialog.setIndeterminate(true);
        pDialog.setMessage("Synchronizing session data...");
        pDialog.setCancelable(false);
        pDialog.show();

        final Date lastSyncDate = new Date(mPrefHelper.getLong(Constants.APP_LAST_SYNC_TIMESTAMP, 0L));
        final SyncHelper syncHelper = new SyncHelper(HelperFactory.getHelper());
        syncHelper.setUserId(ParseUser.getCurrentUser().getObjectId());
        syncHelper.setLastSyncDate(lastSyncDate);

        Task.callInBackground(
                new Callable<Long>() {

                    @Override
                    public Long call() throws Exception {
                      long sync = System.currentTimeMillis();
                      syncHelper.synObjects(CardioSessionEntity.class, true, new SyncCallback());
                      return sync;
                    }
                }
        ).continueWith(
                new Continuation<Long, Object>() {

                    @Override
                    public Object then(Task<Long> task) throws Exception {
                        if (task.isFaulted()) {
                            Log.w(TAG, "Sync failed with exception", task.getError());
                            if (getActivity() != null)
                                Toast.makeText(getActivity(), "Sync failed.", Toast.LENGTH_SHORT).show();
                        } else if (task.isCompleted()) {
                            mPrefHelper.putLong(Constants.APP_LAST_SYNC_TIMESTAMP, task.getResult());
                        }

                        refreshSessionList(null);
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        pDialog = null;
                        return null;
                    }
                },
                Task.UI_THREAD_EXECUTOR);
    }

    private double[][] calculateStress(List<Long> times, List<Integer> rrs) {
        double t[] = new double[times.size()];
        double r[] = new double[rrs.size()];

        // put into double[] arrays
        for (int i = 0; i < Math.min(r.length, t.length); i++) {
            t[i] = times.get(i);
            r[i] = rrs.get(i);
        }

        // filter out artifacts
        r = new PisarukArtifactFilter().doFilter(r);

        // calculate stress
        return HeartRateUtils.getSI(r, t, 2 * 60 * 1000, 5000);
    }

    @Override
    public boolean onClose() {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        refreshSessionList(s);
        return false;
    }


    public class CardioSessionArrayAdapter extends ArrayAdapter<CardioSessionEntity> {

        public CardioSessionArrayAdapter(Context context, List<CardioSessionEntity> src) {
            super(context, R.layout.two_lines_layout, src);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View itemView = inflater.inflate(R.layout.two_lines_layout, parent, false);
            //itemView.setBackgroundResource(R.drawable.list_selector_background);
            CardioSessionEntity entity = getItem(position);

            TextView text1 = (TextView) itemView.findViewById(android.R.id.text1);
            text1.setTypeface(null, Typeface.NORMAL);
            if (entity.getName() == null || entity.getName().trim().isEmpty()) {
                text1.setText("Session " + DATE_FORMAT.format(new Date(entity.getStartTimestamp())));
            } else {
                text1.setText(entity.getName().trim());
            }

            TextView text2 = (TextView) itemView.findViewById(android.R.id.text2);
            text2.setText("Last updated: " + DATE_FORMAT.format(entity.getSyncDate()));

            return itemView;
        }
    }

    private class SyncCallback implements SyncHelper.SyncCallback<CardioSessionEntity> {

        private final int CHUNK_SIZE = 3000;

        @Override
        public void onSaveLocally(CardioSessionEntity localObject, ParseObject remoteObject) throws Exception {
            CardioSessionDAO sessionDao = HelperFactory.getHelper().getCardioSessionDao();
            CardioItemDAO itemDao = HelperFactory.getHelper().getCardioItemDao();
            Dao.CreateOrUpdateStatus status = sessionDao.createOrUpdate(localObject);
            if (status.isUpdated()) {
                DeleteBuilder<CardioItemEntity, Long> del = itemDao.deleteBuilder();
                del.where().eq("session_id", localObject.getId());
                del.delete();
            }

            List<ParseObject> chunks = ParseQuery.getQuery("CardioDataChunk")
                    .whereEqualTo("sessionId", remoteObject.getObjectId())
                    .orderByAscending("number")
                    .find();

            long lastT = localObject.getStartTimestamp();
            for (ParseObject chunk: chunks) {
                JSONArray rrs = chunk.getJSONArray("rrs");
                JSONArray times = chunk.getJSONArray("times");
                for (int i = 0; i < rrs.length(); i++) {
                    CardioItemEntity item = new CardioItemEntity();
                    item.setRr(rrs.getInt(i));
                    item.setBpm(Math.round(60 * (item.getRr() / 1000.0f)));
                    item.setT(times.getLong(i));
                    item.setSession(localObject);
                    itemDao.create(item);

                    if (item.getT() < 1000000000000000L) {
                        lastT = item.getT() + localObject.getStartTimestamp();
                    } else {
                        lastT = item.getT();
                    }
                }
            }
            if (localObject.getEndTimestamp() == 0L) {
                // update endTimestamp
                localObject.setEndTimestamp(lastT);
            }
        }

        @Override
        public void onSaveRemotely(CardioSessionEntity localObject, ParseObject remoteObject) throws Exception {
            CardioItemDAO itemDao = HelperFactory.getHelper().getCardioItemDao();
            GenericRawResults<String[]> results = itemDao.queryBuilder()
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
            List<Integer> allRRs = new ArrayList<Integer>();
            List<Long> allTs = new ArrayList<Long>();
            try {
                Iterator<String[]> it = results.iterator();
                int number = 1;
                do {
                    long firstT = -1l;
                    List<Long> t = new ArrayList<Long>(CHUNK_SIZE);
                    List<Integer> rrs = new ArrayList<Integer>(CHUNK_SIZE);
                    for (int i = 0; i < CHUNK_SIZE && it.hasNext(); i++) {
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
                    allTs.addAll(t);
                    allRRs.addAll(rrs);
                    number++;
                } while (it.hasNext());
                if (allRRs.isEmpty()) {
                    remoteObject.put("deleted", true);
                    localObject.setDeleted(true);
                    HelperFactory.getHelper().getCardioSessionDao().update(localObject);
                }
            } finally {
                results.close();
            }

            if (((CardioSession) remoteObject).getEndTimestamp() == 0L) {
                remoteObject.put("endTimestamp", localObject.getEndTimestamp());
            }
        }
    }


}
