package com.cardiomood.android.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import com.cardiomood.android.db.HeartRateDBContract;
import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.dao.UserDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.data.CardioMoodServer;
import com.cardiomood.data.DataServiceHelper;
import com.cardiomood.data.async.ServerResponseCallback;
import com.cardiomood.data.json.CardioDataItem;
import com.cardiomood.data.json.CardioSession;
import com.cardiomood.data.json.CardioSessionWithData;
import com.cardiomood.data.json.JsonError;
import com.cardiomood.data.json.JsonRRInterval;
import com.cardiomood.data.json.JsonResponse;
import com.flurry.android.FlurryAgent;
import com.haarman.listviewanimations.itemmanipulation.contextualundo.ContextualUndoAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by danshin on 01.11.13.
 */
public class HistoryFragment extends Fragment implements ContextualUndoAdapter.DeleteItemCallback, ListView.OnItemClickListener {

    private ListView listView;
    private View root;
    private ArrayAdapter<HeartRateSession> listAdapter = null;
    private ContextualUndoAdapter undoAdapter = null;
    private PreferenceHelper pHelper;
    private DataServiceHelper serviceHelper;
    private ProgressDialog pDialog = null;

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

        serviceHelper = new DataServiceHelper(CardioMoodServer.INSTANCE.getService(), pHelper);
        serviceHelper.refreshToken();

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

    @Override
    public void deleteItem(int i) {
        HeartRateSession session = listAdapter.getItem(i);
        if (isPredefinedSession(session.getId())) {
            Toast.makeText(getActivity(), R.string.cannot_delete_predefined_data, Toast.LENGTH_SHORT).show();
            return;
        }
        new DeleteItemTask(session).execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        HeartRateSession session = listAdapter.getItem(position);
        if (session != null) {
            Toast.makeText(getActivity(), R.string.opening_measurement, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), SessionDetailsActivity.class);
            intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, session.getId());
            getActivity().startActivity(intent);
        }
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
            public void onError(JsonError error) {

            }
        });
    }

    private void refresh() {
        if (initial && hasUpdatedSessions()) {
            sync();
            return;
        }

        Activity activity = getActivity();
        if (activity == null)
            return;

        if (listAdapter != null) {
            listAdapter.clear();
            listAdapter.notifyDataSetChanged();
        }
        listAdapter = new SessionsArrayAdapter(activity, new ArrayList<HeartRateSession>(100));
        SessionsEndlessAdapter endlessAdapter = new SessionsEndlessAdapter(listAdapter, getActivity().getApplicationContext());
        undoAdapter = new ContextualUndoAdapter(endlessAdapter, R.layout.history_item_undo, R.id.btn_undo_deletion);
        undoAdapter.setAbsListView(listView);
        undoAdapter.setDeleteItemCallback(HistoryFragment.this);
        listView.setAdapter(undoAdapter);
    }

    private boolean hasUpdatedSessions() {
        HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO();
        List<HeartRateSession> sessions = sessionDAO.getSessions(HeartRateDBContract.Sessions.COLUMN_NAME_STATUS + " = ?", new String[]{String.valueOf(SessionStatus.COMPLETED)});
        return !sessions.isEmpty();
    }

    private class DeleteItemTask extends AsyncTask<Void, Void, Boolean> {

        private HeartRateSessionDAO dao = new HeartRateSessionDAO();
        private HeartRateSession session = null;
        private Handler handler = new Handler();

        private DeleteItemTask(HeartRateSession session) {
            this.session = session;
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            try {

                if (session.getExternalId() != null) {
                    final JsonResponse<String> response = serviceHelper.deleteSession(session.getExternalId());
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
                HeartRateSession session = dao.findById(sessionId);
                if (session != null) {
                    dao.delete(sessionId);
                    logSessionDeletedEvent(session);
                }
                return true;
            } catch (Exception ex) {
                Log.w("HistoryFragment", "exception in doInBackground()", ex);
                return false;
            }
        }

        private void logSessionDeletedEvent(HeartRateSession session) {
            Map<String, String> args = new HashMap<String, String>();
            args.put("sessionId", session.getId()+"");
            args.put("sessionName", session.getName());
            args.put("total_sessions", dao.getCount()+"");
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

        private UserDAO userDAO;
        private HeartRateSessionDAO sessionDAO;
        private HeartRateDataItemDAO itemDAO;
        private Long userId;

        private SyncTask(Context context) {
            this.context = context;
            userDAO = new UserDAO();
            sessionDAO = new HeartRateSessionDAO();
            itemDAO = new HeartRateDataItemDAO();
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

            try {
                List<HeartRateSession> sessions = sessionDAO.getSessions(
                        "((" + HeartRateSession.COLUMN_NAME_EXTERNAL_ID + " is null and "
                                + HeartRateSession.COLUMN_NAME_STATUS + " <> '" + SessionStatus.IN_PROGRESS + "') or ("
                                + HeartRateSession.COLUMN_NAME_STATUS + " == '" + SessionStatus.COMPLETED + "')) and "
                                + HeartRateSession.COLUMN_NAME_USER_ID + "=?",
                        new String[]{String.valueOf(userId)}
                );

                int progress = 0;
                for (HeartRateSession session : sessions) {
                    synchronizeSession(session);
                    progress++;
                    publishProgress(context.getString(R.string.progress_sending_data) +" " + Math.round(100.0f * progress / sessions.size()) + "%");
                }

                progress = 0;
                JsonResponse<List<CardioSession>> response = serviceHelper.getSessions();
                if (JsonResponse.RESPONSE_OK.equals(response.getResponseCode())) {
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

        private void synchronizeSession(HeartRateSession session) {
            if (isPredefinedSession(session.getId()))
                return;
            if (session.getExternalId() == null) {
                // Create Session on the server
                JsonResponse<CardioSession> response1 = serviceHelper.createSession();
                if (JsonResponse.RESPONSE_OK.equals(response1.getResponseCode())) {
                    CardioSession cardioSession = response1.getData();
                    rewriteSessionData(session, cardioSession);
                }
            } else if (session.getStatus() == SessionStatus.COMPLETED) {
                CardioSession cardioSession = new CardioSession();
                cardioSession.setId(session.getExternalId());
                rewriteSessionData(session, cardioSession);
            }
        }

        private void rewriteSessionData(HeartRateSession session, CardioSession cardioSession) {
            SessionStatus oldStatus = session.getStatus();
            cardioSession.setDataClassName("JsonRRInterval");
            cardioSession.setName(session.getName());
            cardioSession.setDescription(session.getDescription());
            cardioSession.setCreationTimestamp(session.getDateStarted() == null ? 0 : session.getDateStarted().getTime());
            session.setExternalId(cardioSession.getId());
            session.setStatus(SessionStatus.SYNCHRONIZING);
            sessionDAO.merge(session);

            // Upload sessionData
            List<HeartRateDataItem> items = itemDAO.getItemsBySessionId(session.getId());
            CardioSessionWithData sessionWithData = new CardioSessionWithData(cardioSession);
            List<CardioDataItem> dataItems = new ArrayList<CardioDataItem>(items.size());
            long i = 0;
            for (HeartRateDataItem hrItem: items) {
                CardioDataItem cardioDataItem = new CardioDataItem();
                cardioDataItem.setNumber(i++);
                cardioDataItem.setCreationTimestamp(hrItem.getTimeStamp().getTime());
                cardioDataItem.setSessionId(cardioSession.getId());
                cardioDataItem.setDataItem(new JsonRRInterval((int) hrItem.getRrTime()).toString());
                dataItems.add(cardioDataItem);
            }
            sessionWithData.setDataItems(dataItems);
            JsonResponse<String> response2 = serviceHelper.rewriteCardioSessionData(sessionWithData);
            if (JsonResponse.RESPONSE_OK.equals(response2.getResponseCode())) {
                session.setStatus(SessionStatus.SYNCHRONIZED);
                sessionDAO.merge(session);
            } else {
                session.setStatus(oldStatus);
                sessionDAO.merge(session);
            }
        }

        private void synchronizeCardioSession(CardioSession cardioSession) {
            List<HeartRateSession> sessions = sessionDAO.getSessions(HeartRateSession.COLUMN_NAME_EXTERNAL_ID + "=?", new String[] {String.valueOf(cardioSession.getId())});
            if (sessions.isEmpty()) {
                // create session
                HeartRateSession session = new HeartRateSession();
                session.setUserId(userId);
                session.setStatus(SessionStatus.SYNCHRONIZING);
                session.setDateStarted(new Date(cardioSession.getCreationTimestamp()));
                session.setExternalId(cardioSession.getId());
                session.setDescription(cardioSession.getDescription());
                session.setName(cardioSession.getName());
                session = sessionDAO.insert(session);

                JsonResponse<CardioSessionWithData> response = serviceHelper.getSessionData(cardioSession.getId());
                if (JsonResponse.RESPONSE_OK.equals(response.getResponseCode())) {
                    List<CardioDataItem> dataItems = response.getData().getDataItems();
                    if (dataItems == null)
                        dataItems = Collections.emptyList();
                    List<HeartRateDataItem> items = new ArrayList<HeartRateDataItem>(dataItems.size());
                    for (CardioDataItem dataItem: dataItems) {
                        HeartRateDataItem item = new HeartRateDataItem();
                        item.setSessionId(session.getId());
                        item.setTimeStamp(new Date(dataItem.getCreationTimestamp()));
                        JsonRRInterval rr = JsonRRInterval.fromJson(dataItem.getDataItem());
                        item.setRrTime(rr.getR());
                        if (rr.getR() > 0)
                            item.setHeartBeatsPerMinute(Math.round(60*1000.0f/rr.getR()));
                        items.add(item);
                    }
                    itemDAO.bulkInsert(items);
                    session.setStatus(SessionStatus.SYNCHRONIZED);
                    sessionDAO.merge(session);
                }

            }
        }
    }

}