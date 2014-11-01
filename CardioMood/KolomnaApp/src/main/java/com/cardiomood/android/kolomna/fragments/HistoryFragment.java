package com.cardiomood.android.kolomna.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.kolomna.R;
import com.cardiomood.android.kolomna.SessionViewActivity;
import com.cardiomood.android.kolomna.db.CardioItemDAO;
import com.cardiomood.android.kolomna.db.CardioSessionDAO;
import com.cardiomood.android.kolomna.db.HelperFactory;
import com.cardiomood.android.kolomna.db.entity.CardioItemEntity;
import com.cardiomood.android.kolomna.db.entity.CardioSessionEntity;
import com.cardiomood.android.kolomna.parse.CardioSession;
import com.cardiomood.android.kolomna.tools.Constants;
import com.cardiomood.android.sync.ormlite.SyncHelper;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.filter.PisarukArtifactFilter;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.parse.ParseObject;
import com.parse.ParseUser;

import org.json.JSONArray;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * A fragment representing a list of Items.
 * <p/>
 */
public class HistoryFragment extends ListFragment {

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

        refreshSessionList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_history, menu);
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

    private void refreshSessionList() {
        Task.callInBackground(new Callable<List<CardioSessionEntity>>() {
            @Override
            public List<CardioSessionEntity> call() throws Exception {
                CardioSessionDAO dao = HelperFactory.getHelper().getCardioSessionDao();
                return dao.queryBuilder()
                        .orderBy("creation_timestamp", false)
                        .where().eq("sync_user_id", ParseUser.getCurrentUser().getObjectId())
                        .and().ne("deleted", true)
                        .and().ne("end_timestamp", 0L)
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

        Task.callInBackground(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                long sync = System.currentTimeMillis();
                syncHelper.synObjects(
                        CardioSessionEntity.class,
                        true,
                        new SyncHelper.SyncCallback<CardioSessionEntity>() {
                            @Override
                            public void onSaveLocally(CardioSessionEntity localObject, ParseObject remoteObject) {
                                try {
                                    CardioSessionDAO sessionDao = HelperFactory.getHelper().getCardioSessionDao();
                                    CardioItemDAO itemDao = HelperFactory.getHelper().getCardioItemDao();
                                    Dao.CreateOrUpdateStatus status = sessionDao.createOrUpdate(localObject);
                                    if (status.isUpdated()) {
                                        DeleteBuilder<CardioItemEntity, Long> del = itemDao.deleteBuilder();
                                        del.where().eq("session_id", localObject.getId());
                                        del.delete();
                                    }
                                    JSONArray rrs = ((CardioSession) remoteObject).getRrs();
                                    JSONArray times = ((CardioSession) remoteObject).getT();
                                    for (int i=0; i<rrs.length(); i++) {
                                        CardioItemEntity item = new CardioItemEntity();
                                        item.setRr(rrs.getInt(i));
                                        item.setBpm(Math.round(60 * (item.getRr() / 1000.0f)));
                                        item.setT(times.getLong(i));
                                        item.setSession(localObject);
                                        itemDao.create(item);
                                    }

                                    if (localObject.getEndTimestamp() == 0L) {
                                        // update endTimestamp
                                        if (rrs.length() == 0) {
                                            localObject.setEndTimestamp(localObject.getStartTimestamp());
                                        } else {
                                            localObject.setEndTimestamp(rrs.getLong(rrs.length() - 1));
                                        }
                                    }
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            }

                            @Override
                            public void onSaveRemotely(CardioSessionEntity localObject, ParseObject remoteObject) {
                                try {
                                    CardioItemDAO itemDao = HelperFactory.getHelper().getCardioItemDao();
                                    List<CardioItemEntity> items = itemDao.queryBuilder()
                                            .orderBy("_id", true)
                                            .where().eq("session_id", localObject.getId())
                                            .query();

                                    List<Long> t = new ArrayList<Long>(items.size());
                                    List<Integer> rrs = new ArrayList<Integer>(items.size());
                                    for (CardioItemEntity item: items) {
                                        t.add(item.getT());
                                        rrs.add(item.getRr());
                                    }

                                    remoteObject.put("rrs", rrs);
                                    remoteObject.put("times", t);
                                    if (((CardioSession) remoteObject).getEndTimestamp() == 0L) {
                                        remoteObject.put("endTimestamp", localObject.getEndTimestamp());
                                    }

                                    // calculate stress
                                    double[][] stress = calculateStress(t, rrs);
                                    remoteObject.remove("stressTimes");
                                    List<Double> values = new ArrayList<Double>(stress[0].length);
                                    for (double time: stress[0]) {
                                        values.add(time);
                                    }
                                    remoteObject.addAll("stressTimes", values);

                                    values = new ArrayList<Double>(stress[1].length);
                                    for (double time: stress[1]) {
                                        values.add(time);
                                    }
                                    remoteObject.remove("stressValues");
                                    remoteObject.addAll("stressValues", values);
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        }
                );
                return sync;
            }
        }).continueWith(
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

                        refreshSessionList();
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        pDialog = null;
                        return null;
                    }
                },
                Task.UI_THREAD_EXECUTOR
        );
    }

    private double[][] calculateStress(List<Long> times, List<Integer> rrs) {
        double t[] = new double[times.size()];
        double r[] = new double[rrs.size()];

        // put into double[] arrays
        for (int i=0; i < Math.min(r.length, t.length); i++) {
            t[i] = times.get(i);
            r[i] = rrs.get(i);
        }

        // filter out artifacts
        r = new PisarukArtifactFilter().doFilter(r);

        // calculate stress
        return HeartRateUtils.getSI(r, t, 2 * 60 * 1000, 5000);
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


}
