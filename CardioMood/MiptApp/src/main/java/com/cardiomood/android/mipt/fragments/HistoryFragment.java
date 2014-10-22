package com.cardiomood.android.mipt.fragments;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
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

import com.cardiomood.android.mipt.R;
import com.cardiomood.android.mipt.db.CardioItemDAO;
import com.cardiomood.android.mipt.db.CardioSessionDAO;
import com.cardiomood.android.mipt.db.HelperFactory;
import com.cardiomood.android.mipt.db.SyncEngine;
import com.cardiomood.android.mipt.db.entity.CardioItemEntity;
import com.cardiomood.android.mipt.db.entity.CardioSessionEntity;
import com.cardiomood.android.mipt.parse.CardioSession;
import com.cardiomood.android.mipt.tools.Constants;
import com.cardiomood.android.tools.PreferenceHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
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

    public static HistoryFragment newInstance() {
        HistoryFragment fragment = new HistoryFragment();
        return fragment;
    }

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
        super.onListItemClick(l, v, position, id);
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
        SyncEngine.getInstance().setUserId(ParseUser.getCurrentUser().getObjectId());
        SyncEngine.getInstance().setLastSyncDate(lastSyncDate);

        Task.callInBackground(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                long sync = System.currentTimeMillis();
                SyncEngine.getInstance().synObjects(CardioSession.class, CardioSessionEntity.class,
                        true, new SyncEngine.SyncCallback<CardioSession, CardioSessionEntity>() {
                            @Override
                            public void onSaveLocally(CardioSessionEntity localObject, CardioSession remoteObject) {
                                try {
                                    CardioSessionDAO sessionDao = HelperFactory.getHelper().getCardioSessionDao();
                                    CardioItemDAO itemDao = HelperFactory.getHelper().getCardioItemDao();
                                    Dao.CreateOrUpdateStatus status = sessionDao.createOrUpdate(localObject);
                                    if (status.isUpdated()) {
                                        DeleteBuilder<CardioItemEntity, Long> del = itemDao.deleteBuilder();
                                        del.where().eq("session_id", localObject.getId());
                                        del.delete();
                                    }
                                    JSONArray rrs = remoteObject.getRrs();
                                    JSONArray times = remoteObject.getT();
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
                            public void onSaveRemotely(CardioSessionEntity localObject, CardioSession remoteObject) {
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
                                    if (remoteObject.getEndTimestamp() == 0L) {
                                        remoteObject.put("endTimestamp", localObject.getEndTimestamp());
                                    }
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        });
                return sync;
            }
        }).continueWith(
                new Continuation<Long, Object>() {
                    @Override
                    public Object then(Task<Long> task) throws Exception {
                        if (task.isFaulted()) {
                            if (getActivity() != null)
                                Toast.makeText(getActivity(), "Sync failed.", Toast.LENGTH_SHORT).show();
                        } else if (task.isCompleted()) {
                            mPrefHelper.putLong(Constants.APP_LAST_SYNC_TIMESTAMP, task.getResult());
                        }

                        refreshSessionList();
                        pDialog.dismiss();
                        pDialog = null;
                        return null;
                    }
                },
                Task.UI_THREAD_EXECUTOR
        );
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
                text1.setText("Session " + DATE_FORMAT.format(entity.getCreationDate()));
            } else {
                text1.setText(entity.getName().trim());
            }

            TextView text2 = (TextView) itemView.findViewById(android.R.id.text2);
            text2.setText("Last updated: " + DATE_FORMAT.format(entity.getSyncDate()));

            return itemView;
        }
    }


}
