package com.cardiomood.android.mipt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.mipt.components.HeartRateGraphView;
import com.cardiomood.android.mipt.db.CardioItemDAO;
import com.cardiomood.android.mipt.db.CardioSessionDAO;
import com.cardiomood.android.mipt.db.DatabaseHelper;
import com.cardiomood.android.mipt.db.entity.CardioItemEntity;
import com.cardiomood.android.mipt.db.entity.CardioSessionEntity;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import org.androidannotations.annotations.AfterExtras;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

@EActivity(R.layout.activity_session_view)
public class SessionViewActivity extends Activity {

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

    private static final String EXTRA_SESSION_ID = "com.cardiomood.android.mipt.EXTRA_SESSION_ID";
    private static final String EXTRA_RENAME_SESSION = "com.cardiomood.android.mipt.EXTRA_RENAME_SESSION";


    @Extra(EXTRA_SESSION_ID)
    protected Long sessionId;
    @Extra(EXTRA_RENAME_SESSION)
    protected boolean renameSession;

    // DAO objects
    @OrmLiteDao(helper = DatabaseHelper.class, model = CardioSessionEntity.class)
    protected CardioSessionDAO sessionDAO;
    @OrmLiteDao(helper = DatabaseHelper.class, model = CardioItemEntity.class)
    protected CardioItemDAO itemDAO;

    private CardioSessionEntity mSession;

    @ViewById(R.id.session_name)
    protected TextView sessionName;
    @ViewById(R.id.session_start_date)
    protected TextView startDate;
    @ViewById(R.id.graph_container)
    protected LinearLayout chartContainer;

    private GraphView mGraphView;
    private GraphViewSeries mHeartRateSeries;

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


    @AfterExtras
    protected void afterExtras() {
        if (sessionId == null) {
            Toast.makeText(this, "Session ID is null. Closing...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @AfterViews
    protected void afterViews() {
        // Init Graph View
        mGraphView = new HeartRateGraphView(this);

        mHeartRateSeries = new GraphViewSeries(new GraphView.GraphViewData[0]);
        mGraphView.addSeries(mHeartRateSeries);
        chartContainer.addView(mGraphView);

        // load data in background
        loadSessionDataInBackground(sessionId);
    }

    @Background(id = "load_session")
    protected void loadSessionDataInBackground(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is null");
        }
        try {
            CardioSessionEntity session = sessionDAO.queryForId(sessionId);
            List<CardioItemEntity> items = itemDAO.queryBuilder()
                    .orderBy("_id", true).where().eq("session_id", sessionId)
                    .query();
            onSessionLoaded(session, items);
        } catch (Exception ex) {
            onSessionLoadFailed(ex, sessionId);
        }
    }

    @UiThread
    protected void onSessionLoaded(CardioSessionEntity session, List<CardioItemEntity> items) {
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
            CardioItemEntity item = items.get(i);
            data[i] = new GraphView.GraphViewData(t, 60*1000.d/item.getRr());
            t += item.getRr();
        }
        mHeartRateSeries.resetData(data);
    }

    @UiThread
    protected void onSessionLoadFailed(Exception ex, Long sessionId) {
        Toast.makeText(this, "Failed to load session with ID=" + sessionId, Toast.LENGTH_SHORT).show();
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


    @UiThread
    protected void showToast(CharSequence message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @UiThread
    protected void onSessionRenamed() {
        String name = mSession.getName();
        if (name == null || name.trim().isEmpty()) {
            sessionName.setText("<Untitled Session>");
        } else {
            sessionName.setText(name.trim());
        }
    }
}
