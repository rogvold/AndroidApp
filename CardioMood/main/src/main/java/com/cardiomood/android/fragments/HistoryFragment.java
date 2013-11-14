package com.cardiomood.android.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.cardiomood.android.R;
import com.cardiomood.android.SessionDetailsActivity;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.tools.config.PreferenceHelper;
import com.haarman.listviewanimations.itemmanipulation.contextualundo.ContextualUndoAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by danshin on 01.11.13.
 */
public class HistoryFragment extends Fragment implements ContextualUndoAdapter.DeleteItemCallback, ListView.OnItemClickListener {

    private ListView listView;
    private View root;
    private ArrayAdapter<HeartRateSession> listAdapter = null;
    private ContextualUndoAdapter undoAdapter = null;
    private PreferenceHelper pHelper;

    // work around for 'view already has a parent...'
    private boolean initial = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        pHelper = new PreferenceHelper(getActivity().getApplicationContext());
        pHelper.setPersistent(true);

        root = inflater.inflate(R.layout.fragment_history, container, false);
        listView = (ListView) root.findViewById(R.id.sessionList);
        listView.setOnItemClickListener(this);
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
        if (initial) {
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            }, 1000);
            initial = false;
        } else {
            refresh();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_sessions_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isPredefinedSession(long sessionId) {
        List<Long> ids = Arrays.asList(
                pHelper.getLong(ConfigurationConstants.DB_GOOD_SESSION_ID),
                pHelper.getLong(ConfigurationConstants.DB_BAD_SESSION_ID),
                pHelper.getLong(ConfigurationConstants.DB_ATHLETE_SESSION_ID),
                pHelper.getLong(ConfigurationConstants.DB_STRESSED_SESSION_ID)
        );
        return ids.contains(sessionId);
    }

    @Override
    public void deleteItem(int i) {
        HeartRateSession session = listAdapter.getItem(i);
        if (isPredefinedSession(session.getId())) {
            Toast.makeText(getActivity(), R.string.cannot_delete_predefined_data, Toast.LENGTH_SHORT).show();
            return;
        }
        new DeleteItemTask().execute(session.getId());
        listAdapter.remove(session);
        Toast.makeText(getActivity(), getText(R.string.item_removed), Toast.LENGTH_SHORT).show();
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (MonitorFragment.isMonitoring) {
            Toast.makeText(getActivity(), R.string.monitoring_is_in_progress, Toast.LENGTH_SHORT).show();
            return;
        }
        HeartRateSession session = listAdapter.getItem(position);
        if (session != null) {
            Toast.makeText(getActivity(), R.string.opening_measurement, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), SessionDetailsActivity.class);
            intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, session.getId());
            getActivity().startActivity(intent);
        }
    }

    private void refresh() {
        if (listAdapter != null) {
            listAdapter.clear();
            listAdapter.notifyDataSetChanged();
        }

        listAdapter = new SessionsArrayAdapter(getActivity(), new ArrayList<HeartRateSession>(100));
        SessionsEndlessAdapter endlessAdapter = new SessionsEndlessAdapter(listAdapter);
        undoAdapter = new ContextualUndoAdapter(endlessAdapter, R.layout.history_item_undo, R.id.btn_undo_deletion);
        undoAdapter.setAbsListView(listView);
        undoAdapter.setDeleteItemCallback(this);
        listView.setAdapter(undoAdapter);
    }

    private class DeleteItemTask extends AsyncTask<Long, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Long... args) {
            try {
                long sessionId = args[0];
                new HeartRateSessionDAO().delete(sessionId);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (getActivity() != null) {
                if (result == null || !result) {
                    Toast.makeText(getActivity(), R.string.failed_to_romove_session, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.item_removed, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}