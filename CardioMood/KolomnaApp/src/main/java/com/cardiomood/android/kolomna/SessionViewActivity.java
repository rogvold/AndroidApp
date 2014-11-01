package com.cardiomood.android.kolomna;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.kolomna.components.HeartRateGraphView;
import com.cardiomood.android.kolomna.db.CardioItemDAO;
import com.cardiomood.android.kolomna.db.CardioSessionDAO;
import com.cardiomood.android.kolomna.db.HelperFactory;
import com.cardiomood.android.kolomna.db.entity.CardioSessionEntity;
import com.j256.ormlite.dao.GenericRawResults;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class SessionViewActivity extends ActionBarActivity {

    private static final String TAG = SessionViewActivity.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

    public static final String EXTRA_SESSION_ID = "com.cardiomood.android.kolomna.EXTRA_SESSION_ID";
    public static final String EXTRA_RENAME_SESSION = "com.cardiomood.android.kolomna.EXTRA_RENAME_SESSION";


    protected Long sessionId;
    protected boolean renameSession;

    // DAO objects
    protected CardioSessionDAO sessionDAO;
    protected CardioItemDAO itemDAO;

    private CardioSessionEntity mSession;

    @InjectView(R.id.session_name)
    protected TextView sessionName;
    @InjectView(R.id.session_start_date)
    protected TextView startDate;
    @InjectView(R.id.graph_container)
    protected LinearLayout chartContainer;

    private GraphView mGraphView;
    private GraphViewSeries mHeartRateSeries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_view);

        if (savedInstanceState == null) {
            renameSession = getIntent().getBooleanExtra(EXTRA_RENAME_SESSION, false);
        }
        sessionId = getIntent().getLongExtra(EXTRA_SESSION_ID, -1);

        if (sessionId == null || sessionId < 0) {
            Toast.makeText(this, "Session ID is null. Closing...", Toast.LENGTH_SHORT).show();
            finish();
        }

        ButterKnife.inject(this);

        // Init Graph View
        mGraphView = new HeartRateGraphView(this);

        mHeartRateSeries = new GraphViewSeries(new GraphView.GraphViewData[0]);
        mGraphView.addSeries(mHeartRateSeries);
        chartContainer.addView(mGraphView);

        try {
            sessionDAO = HelperFactory.getHelper().getCardioSessionDao();
            itemDAO = HelperFactory.getHelper().getCardioItemDao();
        } catch (SQLException ex) {
            Log.e(TAG, "onCreate() failed to obtain DAO", ex);
            Toast.makeText(this, "Failed to obtain DAO", Toast.LENGTH_SHORT).show();
            finish();
        }

        // load data in background
        loadSessionDataInBackground(sessionId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_session_view, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_rename:
                showRenameDialog();
                return true;
            case R.id.menu_refresh:
                loadSessionDataInBackground(sessionId);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void loadSessionDataInBackground(final Long sessionId) {
        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                if (sessionId == null) {
                    throw new IllegalArgumentException("Session ID is null");
                }
                CardioSessionEntity session = sessionDAO.queryForId(sessionId);
                GenericRawResults<String[]> results = itemDAO.queryBuilder()
                        .orderBy("_id", true)
                        .selectColumns("_id", "rr")
                        .where().eq("session_id", sessionId)
                        .queryRaw();
                try {
                    List<Integer> items = new ArrayList<Integer>();
                    for (String[] row: results) {
                        items.add(Integer.valueOf(row[1]));
                    }
                    onSessionLoaded(session, items);
                } finally {
                    results.close();
                }
                return null;
            }
        }).continueWith(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {
                if (task.isFaulted()) {
                    onSessionLoadFailed(task.getError(), sessionId);
                }
                return null;
            }
        });


        try {

        } catch (Exception ex) {
            onSessionLoadFailed(ex, sessionId);
        }
    }

    protected void onSessionLoaded(final CardioSessionEntity session, final List<Integer> items) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSession = session;
                if (renameSession) {
                    renameSession = false;
                    showRenameDialog();
                }

                if (session.getName() == null || session.getName().trim().isEmpty()) {
                    sessionName.setText("<Untitled Session>");
                } else {
                    sessionName.setText(session.getName().trim());
                }
                startDate.setText(DATE_FORMAT.format(new Date(session.getStartTimestamp())));

                GraphView.GraphViewData[] data = new GraphView.GraphViewData[items.size()];
                long t = 0;
                for (int i=0; i<data.length; i++) {
                    Integer item = items.get(i);
                    data[i] = new GraphView.GraphViewData(t, 60*1000.d/ item);
                    t += item;
                }
                mHeartRateSeries.resetData(data);
            }
        });
    }

    protected void onSessionLoadFailed(Exception ex, Long sessionId) {
        showToast("Failed to load session with ID=" + sessionId);
    }

    private void showRenameDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_input_text, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        userInput.setText(mSession.getName());

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String newName = userInput.getText() == null ? "" : userInput.getText().toString();
                                newName = newName.trim();
                                if (newName.isEmpty())
                                    newName = null;
                                renameSessionInBackground(mSession, newName);
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

    protected void renameSessionInBackground(CardioSessionEntity session, String newName) {
        try {
            sessionDAO.refresh(session);
            session.setName(newName);
            session.setSyncDate(new Date());
            sessionDAO.update(session);
            showToast("Session renamed");
            onSessionRenamed();
        } catch (Exception ex) {
            showToast("Failed to rename session");
        }
    }

    protected void showToast(final CharSequence message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SessionViewActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void onSessionRenamed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String name = mSession.getName();
                if (name == null || name.trim().isEmpty()) {
                    sessionName.setText("<Untitled Session>");
                } else {
                    sessionName.setText(name.trim());
                }
            }
        });

    }
}
