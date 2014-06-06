package com.cardiomood.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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
import com.cardiomood.android.db.entity.HRSessionEntity;
import com.cardiomood.android.db.entity.RRIntervalEntity;
import com.cardiomood.android.db.entity.SessionStatus;
import com.cardiomood.android.db.entity.UserEntity;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.data.CardioMoodServer;
import com.cardiomood.data.DataServiceHelper;
import com.cardiomood.data.async.ServerResponseCallback;
import com.cardiomood.data.async.ServerResponseCallbackRetry;
import com.cardiomood.data.json.CardioDataItem;
import com.cardiomood.data.json.CardioSession;
import com.cardiomood.data.json.CardioSessionWithData;
import com.cardiomood.data.json.JSONError;
import com.cardiomood.data.json.JSONResponse;
import com.cardiomood.data.json.JsonRRInterval;
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

;

/**
 * Created by danshin on 01.11.13.
 */
public class HistoryFragment extends Fragment
        implements ListView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = HistoryFragment.class.getSimpleName();

    private ListView listView;
    private View root;
    private SessionsArrayAdapter listAdapter = null;
    private PreferenceHelper pHelper;
    private DataServiceHelper serviceHelper;
    private ProgressDialog pDialog = null;
    private ActionMode mActionMode = null;
    private Long userId = null;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        pHelper = new PreferenceHelper(getActivity(), true);

        userId = pHelper.getLong(ConfigurationConstants.USER_ID);

        root = inflater.inflate(R.layout.fragment_history, container, false);
        listView = (ListView) root.findViewById(R.id.sessionList);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
        setHasOptionsMenu(true);

        serviceHelper = new DataServiceHelper(CardioMoodServer.INSTANCE.getService(), pHelper);

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
                    initial = false;
                }
            }, 1000);
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
                sync();
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

    public void deleteItem(int i) {
        HRSessionEntity session = listAdapter.getItem(i);
        if (isPredefinedSession(session.getId())) {
            Toast.makeText(getActivity(), R.string.cannot_delete_predefined_data, Toast.LENGTH_SHORT).show();
            return;
        }
        new DeleteItemTask(session).execute();
    }

    public void renameItem(int i) {
        final HRSessionEntity itemSession = listAdapter.getItem(i);
        final long sessionId = itemSession.getId();
        RuntimeExceptionDao<HRSessionEntity, Long> sessionDAO = getHelper().getRuntimeExceptionDao(HRSessionEntity.class);
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.dialog_input_text, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        userInput.setText(sessionDAO.queryForId(sessionId).getName());

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                final RuntimeExceptionDao<HRSessionEntity, Long> dao = getHelper().getRuntimeExceptionDao(HRSessionEntity.class);
                                final HRSessionEntity session = dao.queryForId(sessionId);
                                String newName = userInput.getText() == null ? "" : userInput.getText().toString();
                                newName = newName.trim();
                                if (newName.isEmpty())
                                    newName = null;
                                session.setName(newName);
                                if (session.getStatus() == SessionStatus.SYNCHRONIZED)
                                    session.setStatus(SessionStatus.COMPLETED);
                                dao.update(session);
                                Toast.makeText(HistoryFragment.this.getActivity(), R.string.session_renamed, Toast.LENGTH_SHORT).show();
                                if (session.getExternalId() != null) {
                                    serviceHelper.updateSessionInfo(session.getExternalId(), session.getName(), session.getDescription(), new ServerResponseCallbackRetry<CardioSession>() {
                                        @Override
                                        public void retry() {
                                            serviceHelper.updateSessionInfo(sessionId, session.getName(), session.getDescription(), this);
                                        }

                                        @Override
                                        public void onResult(CardioSession result) {
                                            session.setStatus(SessionStatus.SYNCHRONIZED);
                                            session.setName(result.getName());
                                            session.setDescription(result.getDescription());
                                            dao.update(session);
                                        }

                                        @Override
                                        public void onError(JSONError error) {
                                            Log.d(TAG, "updateSessionInfo failed, error="+error);
                                        }
                                    });
                                }
                                itemSession.setName(session.getName());
                                listAdapter.notifyDataSetChanged();
                                FlurryAgent.logEvent("session_renamed");
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionMode == null) {
            HRSessionEntity session = listAdapter.getItem(position);
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
        final Activity activity = getActivity();
        if (activity == null)
            return;

        pDialog = new ProgressDialog(activity);

        serviceHelper.checkInternetAvailable(activity, new ServerResponseCallback<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                if (result) {
                    new SyncTask(activity).execute();
                } else {
                    refresh();
                    Toast.makeText(activity, "Data sever is not accessible. Try again later.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(JSONError error) {

            }
        });
    }

    private void refresh() {
        if (mActionMode != null) {
            return;
        }

        if (initial && hasUpdatedSessions()) {
            sync();
            return;
        }

        Activity activity = getActivity();
        if (activity == null)
            return;

        if (listAdapter != null) {
            listAdapter.setSelectedItem(-1);
            listAdapter.clear();
            listAdapter.notifyDataSetChanged();
        }
        listAdapter = new SessionsArrayAdapter(activity, getHelper(), new ArrayList<HRSessionEntity>(100));
        SessionsEndlessAdapter endlessAdapter = new SessionsEndlessAdapter(listAdapter, getActivity(), getHelper());
        listView.setAdapter(endlessAdapter);
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
        }
        return databaseHelper;
    }

    private boolean hasUpdatedSessions() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("status", SessionStatus.COMPLETED);
        values.put("user_id", userId);
        RuntimeExceptionDao<HRSessionEntity, Long> sessionDAO = getHelper()
                .getRuntimeExceptionDao(HRSessionEntity.class);
        try {
            long count = sessionDAO.queryBuilder()
                    .where().eq("status", SessionStatus.COMPLETED)
                    .and().eq("user_id", userId)
                    .countOf();
            return (count > 0);
        } catch (SQLException ex) {
            Log.w(TAG, "hasUpdatedSessions() failed", ex);
            return false;
        }
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

    private class DeleteItemTask extends AsyncTask<Void, Void, Boolean> {
        private HRSessionEntity session = null;
        private Handler handler = new Handler();
        RuntimeExceptionDao<HRSessionEntity, Long> hrSessionDAO;

        private DeleteItemTask(HRSessionEntity session) {
            this.session = session;
            hrSessionDAO = getHelper().getRuntimeExceptionDao(HRSessionEntity.class);
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            try {

                if (session.getExternalId() != null) {
                    final JSONResponse<String> response = serviceHelper.deleteSession(session.getExternalId());
                    Log.e("Fuck!", response.toString());
                    if (!response.isOk()) {
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                if (getActivity() != null) {
                                    if (response.getError() != null)
                                        Toast.makeText(getActivity(), response.getError().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        return null;
                    }
                }

                long sessionId = session.getId();
                HRSessionEntity session = hrSessionDAO.queryForId(sessionId);
                if (session != null) {
                    hrSessionDAO.deleteById(sessionId);
                    logSessionDeletedEvent(session);
                }
                return true;
            } catch (Exception ex) {
                Log.w("HistoryFragment", "exception in doInBackground()", ex);
                return false;
            }
        }

        private void logSessionDeletedEvent(HRSessionEntity session) {
            Map<String, String> args = new HashMap<String, String>();
            args.put("sessionId", session.getId()+"");
            args.put("sessionName", session.getName());
            args.put("total_sessions", hrSessionDAO.countOf()+"");
            FlurryAgent.logEvent("session_deleted", args);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (getActivity() != null) {
                if (result == null)
                    return;
                if (!result) {
                    Toast.makeText(getActivity(), R.string.failed_to_romove_session, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.item_removed, Toast.LENGTH_SHORT).show();
                    listAdapter.remove(session);
                    listAdapter.notifyDataSetChanged();
                }

            }
        }
    }

    private class SyncTask extends AsyncTask {

        private Context context = null;

        private RuntimeExceptionDao<UserEntity, Long> userDAO;
        private RuntimeExceptionDao<HRSessionEntity, Long> sessionDAO;
        private RuntimeExceptionDao<RRIntervalEntity, Long> itemDAO;
        private Long userId;

        private SyncTask(Context context) {
            this.context = context;
            userDAO = getHelper().getRuntimeExceptionDao(UserEntity.class);
            sessionDAO = getHelper().getRuntimeExceptionDao(HRSessionEntity.class);
            itemDAO = getHelper().getRuntimeExceptionDao(RRIntervalEntity.class);
            userId = pHelper.getLong(ConfigurationConstants.USER_ID, -1);
        }

        @Override
        protected void onPreExecute() {
            if (userId >= 0) {
                pDialog.setMessage("Synchronizing data...");
                pDialog.setIndeterminate(true);
                pDialog.setCancelable(false);
                pDialog.show();
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            if (userId < 0)
                return;
            pDialog.setMessage("100% - completed.");

           refresh();

            pDialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            pDialog.setMessage((String) values[0]);
        }

        @Override
        protected Object doInBackground(Object[] params) {
            if (userId < 0)
                return null;

            if (serviceHelper.isTokenExpired()) {
                Log.w(TAG, "SyncTask -> token is expired");
                serviceHelper.refreshToken(true);
            }

            try {

                QueryBuilder<HRSessionEntity, Long> qb = sessionDAO.queryBuilder();
                Where w = qb.where();
                w.isNull("external_id");
                w.ne("status", SessionStatus.IN_PROGRESS);
                w.and(w, w);
                w.or().eq("status", SessionStatus.COMPLETED);
                w.eq("user_id", userId);
                PreparedQuery<HRSessionEntity> pq = w.and(w, w).prepare();
                List<HRSessionEntity> sessions = sessionDAO.query(pq);

                int progress = 0;
                for (HRSessionEntity session : sessions) {
                    synchronizeSession(session);
                    progress++;
                    publishProgress(context.getString(R.string.progress_sending_data) +" " + Math.round(100.0f * progress / sessions.size()) + "%");
                }

                progress = 0;
                JSONResponse<List<CardioSession>> response = serviceHelper.getSessions();
                if (JSONResponse.RESPONSE_OK.equals(response.getResponseCode())) {
                    List<CardioSession> cardioSessions = response.getData();
                    if (cardioSessions == null)
                        return null;
                    for (CardioSession cardioSession : cardioSessions) {
                        synchronizeCardioSession(cardioSession);
                        progress++;
                        publishProgress(context.getText(R.string.progress_receiving_data) + " " + Math.round(100.0f * progress / cardioSessions.size()) + "%");
                    }
                }
            } catch (Exception ex) {
                Log.e("HistoryFragment", "SyncTask.doInBackground() exception", ex);
            }
         return null;
        }

        private void synchronizeSession(HRSessionEntity session) {
            if (isPredefinedSession(session.getId()))
                return;
            if (session.getExternalId() == null) {
                // Create Session on the server
                JSONResponse<CardioSession> response1 = serviceHelper.createSession("JsonRRInterval");
                if (JSONResponse.RESPONSE_OK.equals(response1.getResponseCode())) {
                    CardioSession cardioSession = response1.getData();
                    rewriteSessionData(session, cardioSession);
                }
            } else if (session.getStatus() == SessionStatus.COMPLETED) {
                CardioSession cardioSession = new CardioSession();
                cardioSession.setId(session.getExternalId());
                rewriteSessionData(session, cardioSession);
            }
        }

        private void rewriteSessionData(HRSessionEntity session, CardioSession cardioSession) {
            SessionStatus oldStatus = session.getStatus();
            cardioSession.setDataClassName("JsonRRInterval");
            cardioSession.setName(session.getName());
            cardioSession.setDescription(session.getDescription());
            cardioSession.setCreationTimestamp(session.getDateStarted() == null ? 0 : session.getDateStarted().getTime());
            session.setExternalId(cardioSession.getId());
            session.setStatus(SessionStatus.SYNCHRONIZING);
            sessionDAO.update(session);

            // Upload sessionData
            try {
                List<RRIntervalEntity> items = itemDAO.queryBuilder().orderBy("_id", true)
                        .where().eq("session_id", session.getId()).query();
                CardioSessionWithData sessionWithData = new CardioSessionWithData(cardioSession);
                List<CardioDataItem> dataItems = new ArrayList<CardioDataItem>(items.size());
                long i = 0;
                for (RRIntervalEntity hrItem : items) {
                    CardioDataItem cardioDataItem = new CardioDataItem();
                    cardioDataItem.setNumber(i++);
                    cardioDataItem.setCreationTimestamp(hrItem.getTimeStamp().getTime());
                    cardioDataItem.setSessionId(cardioSession.getId());
                    cardioDataItem.setDataItem(new JsonRRInterval((int) hrItem.getRrTime()).toString());
                    dataItems.add(cardioDataItem);
                }
                sessionWithData.setDataItems(dataItems);
                JSONResponse<String> response2 = serviceHelper.rewriteCardioSessionData(sessionWithData);
                if (JSONResponse.RESPONSE_OK.equals(response2.getResponseCode())) {
                    session.setStatus(SessionStatus.SYNCHRONIZED);
                    sessionDAO.update(session);
                } else {
                    session.setStatus(oldStatus);
                    sessionDAO.update(session);
                }
            } catch (SQLException ex) {
                session.setStatus(oldStatus);
                sessionDAO.update(session);
            }
        }

        private void synchronizeCardioSession(CardioSession cardioSession) {
            try {
                long count = sessionDAO.queryBuilder()
                        .where().eq("user_id", userId)
                        .and().eq("external_id", cardioSession.getId())
                        .countOf();

                if (count == 0) {
                    // create session locally
                    HRSessionEntity session = new HRSessionEntity();
                    session.setUserId(userId);
                    session.setStatus(SessionStatus.SYNCHRONIZING);
                    session.setDateStarted(new Date(cardioSession.getCreationTimestamp()));
                    session.setExternalId(cardioSession.getId());
                    session.setDescription(cardioSession.getDescription());
                    session.setName(cardioSession.getName());
                    session = sessionDAO.createIfNotExists(session);

                    JSONResponse<CardioSessionWithData> response = serviceHelper.getSessionData(cardioSession.getId());
                    if (JSONResponse.RESPONSE_OK.equals(response.getResponseCode())) {
                        List<CardioDataItem> dataItems = response.getData().getDataItems();
                        if (dataItems == null)
                            dataItems = Collections.emptyList();
                        final List<RRIntervalEntity> items = new ArrayList<RRIntervalEntity>(dataItems.size());
                        long duration = 0;
                        for (CardioDataItem dataItem : dataItems) {
                            RRIntervalEntity item = new RRIntervalEntity();
                            item.setSessionId(session.getId());
                            item.setTimeStamp(new Date(dataItem.getCreationTimestamp()));
                            JsonRRInterval rr = JsonRRInterval.fromJson(dataItem.getDataItem());
                            item.setRrTime(rr.getR());
                            duration += rr.getR();
                            if (rr.getR() > 0)
                                item.setHeartBeatsPerMinute(Math.round(60 * 1000.0f / rr.getR()));
                            items.add(item);
                        }
                        itemDAO.callBatchTasks(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                for (RRIntervalEntity item: items) {
                                    itemDAO.create(item);
                                }
                                return null;
                            }
                        });
                        session.setDateEnded(new Date(session.getDateStarted().getTime() + duration));
                        session.setStatus(SessionStatus.SYNCHRONIZED);
                        sessionDAO.update(session);
                    }

                }
            } catch (SQLException ex) {
                Log.w(TAG, "synchronizeCardioSession() failed", ex);
            }
        }
    }

}