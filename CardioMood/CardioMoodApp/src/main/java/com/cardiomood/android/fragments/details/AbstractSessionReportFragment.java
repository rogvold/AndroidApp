package com.cardiomood.android.fragments.details;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.R;
import com.cardiomood.android.ReportPreviewActivity;
import com.cardiomood.android.SessionDetailsActivity;
import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.entity.HRSessionEntity;
import com.cardiomood.android.db.entity.RRIntervalEntity;
import com.cardiomood.android.db.entity.SessionStatus;
import com.cardiomood.android.dialogs.SaveAsDialog;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.math.filter.ArtifactFilter;
import com.cardiomood.math.filter.PisarukArtifactFilter;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.ChartView;
import com.shinobicontrols.charts.ShinobiChart;

import java.io.File;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by danon on 11.03.14.
 */
public abstract class AbstractSessionReportFragment extends Fragment {

    private static final String TAG = AbstractSessionReportFragment.class.getSimpleName();
    public static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
    public static final String ARG_SESSION_ID = "com.cardiomood.android.fragments.extra.SESSION_ID";

    private Axis xAxis;
    private Axis yAxis;
    private long sessionId;
    private boolean savingInProgress = false;

    // Components in this fragment view:
    private ScrollView scrollView;
    private LinearLayout progressView;
    private ContentLoadingProgressBar progress;
    private TextView sessionName;
    private TextView sessionDate;
    private ChartView chartView;
    private ShinobiChart chart;
    private FrameLayout topCustomSection;
    private FrameLayout bottomCustomSection;

    private int filterCount = 0;
    private int artifactsLeft = 0;
    private boolean refreshing = false;

    private static final ArtifactFilter FILTER = new PisarukArtifactFilter();
    private int artifactsFiltered = 0;
    private SessionDetailsActivity mHostActivity = null;
    private double rr[] = null;
    private DatabaseHelper databaseHelper;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment OveralSessionReportFragment.
     */
    public static <T extends AbstractSessionReportFragment> T newInstance(Class<T> clazz, long sessionId) {
        try {
            T fragment = clazz.newInstance();
            Bundle args = new Bundle();
            args.putLong(ARG_SESSION_ID, sessionId);
            fragment.setArguments(args);
            return fragment;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public AbstractSessionReportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // obtain arguments
            this.sessionId = getArguments().getLong(ARG_SESSION_ID, -1L);
            if (sessionId < 0)
                throw new IllegalArgumentException("Argument ARG_SESSION_ID is required!");
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_session_report, container, false);
        progressView = (LinearLayout) v.findViewById(R.id.progress);
        progress = (ContentLoadingProgressBar) v.findViewById(R.id.content_loading);
        scrollView = (ScrollView) v.findViewById(R.id.scrollView);
        topCustomSection = (FrameLayout) v.findViewById(R.id.topCustomSection);
        bottomCustomSection = (FrameLayout) v.findViewById(R.id.bottomCustomSection);
        sessionName = (TextView) v.findViewById(R.id.session_title);
        sessionDate = (TextView) v.findViewById(R.id.session_date);

        chartView = (ChartView) v.findViewById(R.id.chart);
        chartView.setDrawingCacheEnabled(true);
        chartView.onCreate(savedInstanceState);
        chart = chartView.getShinobiChart();
        chart.setLicenseKey(ConfigurationConstants.SHINOBI_CHARTS_API_KEY);

        int topCustomLayoutId = getTopCustomLayoutId();
        if (topCustomLayoutId != -1) {
            inflater.inflate(topCustomLayoutId, topCustomSection, true);
        }

        int bottomCustomLayoutId = getBottomCustomLayoutId();
        if (bottomCustomLayoutId != -1) {
            inflater.inflate(bottomCustomLayoutId, bottomCustomSection, true);
        }

        if (xAxis == null)
            xAxis = getXAxis();
        else
            chart.removeXAxis(xAxis);

        if (yAxis == null)
            yAxis = getYAxis();
        else
            chart.removeYAxis(yAxis);

        chart.setXAxis(xAxis);
        chart.setYAxis(yAxis);

        v.invalidate();

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        chartView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        chartView.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_session_report, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.menu_save_as);
        item.setEnabled(!savingInProgress);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refresh();
                return true;
            case R.id.menu_save_as:
                showSaveAsDialog();
                return true;
            case R.id.menu_save_this_revision:
                saveThisRevision();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



    protected DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
        }
        return databaseHelper;
    }

    private void saveThisRevision() {
        if (!savingInProgress) {
            savingInProgress = true;
            new SaveThisRevisionTask().execute();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        chartView.onDestroy();

        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mHostActivity != null) {
            mHostActivity.unregisterFragment(this);
            mHostActivity = null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SessionDetailsActivity) {
            mHostActivity = (SessionDetailsActivity) activity;
            mHostActivity.registerFragment(this);
        }
    }

    public void refresh() {
        if (!refreshing) {
            refreshing = true;
            new DataLoadingTask().execute(sessionId);
        }
    }

    protected int getTopCustomLayoutId() {
        return -1;
    }

    protected int getBottomCustomLayoutId() {
        return -1;
    }

    public long getSessionId() {
        return sessionId;
    }

    public int getArtifactsFiltered() {
        return artifactsFiltered;
    }

    public int getFilterCount() {
        return filterCount;
    }

    public void setFilterCount(int filterCount) {
        this.filterCount = filterCount;
    }

    public int getArtifactsLeft() {
        return artifactsLeft;
    }

    public void removeArtifacts() {
        if (artifactsLeft > 0) {
            filterCount++;
            refresh();
        }
    }

    public void undoRemoveArtifacts() {
        if (filterCount > 0) {
            filterCount--;
            refresh();
        }
    }


    protected void showProgress() {
        scrollView.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);
        progress.show();
    }

    protected void hideProgress() {
        progressView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        progress.hide();
    }

    public ShinobiChart getChart() {
        return chart;
    }

    private void showSaveAsDialog() {
        SaveAsDialog dlg = new SaveAsDialog(getActivity(), databaseHelper, sessionId, filterCount);
        dlg.setTitle(R.string.save_as_dlg_title);
        dlg.setSavingCallback(new SaveAsDialog.SavingCallback() {

            @Override
            public void onBeginSave() {
                savingInProgress = true;
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onEndSave(String fileName) {
                savingInProgress = false;
                getActivity().invalidateOptionsMenu();

                if (fileName == null) {
                    return;
                }

                Intent previewIntent = new Intent(getActivity(), ReportPreviewActivity.class);
                previewIntent.putExtra(ReportPreviewActivity.EXTRA_FILE_PATH, fileName);
                startActivity(previewIntent);

                if (!fileName.toLowerCase().endsWith(".png")) {
                    // saved as not *.png
                    return;
                }

                Context context = getActivity();
                if (context == null)
                    return;

                // add item to notification
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                File file = new File(fileName);
                intent.setDataAndType(Uri.fromFile(file), "image/png");

                PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

                Notification.Builder builder = new Notification.Builder(context);
                builder.setContentIntent(pIntent)
                        .setSmallIcon(R.drawable.ic_action_save)
                        .setTicker(getText(R.string.measurement_saved_notification_text))
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setContentTitle(getText(R.string.measurement_saved_notification_title))
                        .setContentText(getText(R.string.measurement_saved_notification_text));

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    // build notification for HoneyComb to ICS
                    notificationManager.notify(1, builder.getNotification());
                } if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    // Notification for Jellybean and above
                    notificationManager.notify(1, builder.build());
                }
            }

            @Override
            public void onError() {
                savingInProgress = false;
                getActivity().invalidateOptionsMenu();
            }
        });
        dlg.saveAsTxt();
    }

    protected RuntimeExceptionDao<HRSessionEntity, Long> getSessionDao() {
        return getHelper().getRuntimeExceptionDao(HRSessionEntity.class);
    }

    protected RuntimeExceptionDao<RRIntervalEntity, Long> getRRIntervalDao() {
        return getHelper().getRuntimeExceptionDao(RRIntervalEntity.class);
    }

    protected abstract Axis getXAxis();
    protected abstract Axis getYAxis();
    protected abstract double[] collectDataInBackground(HRSessionEntity session);
    protected abstract void displayData(double rr[]);

    private class DataLoadingTask extends AsyncTask<Long, Void, double[]> {

        private RuntimeExceptionDao<HRSessionEntity, Long> sessionDAO;
        private String name;
        private String date;
        private int artifactsRemoved = 0;

        private DataLoadingTask() {
            sessionDAO = getSessionDao();
        }

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected double[] doInBackground(Long... params) {
            long sessionId = params[0];
            HRSessionEntity session = sessionDAO.queryForId(sessionId);
            name = session.getName();
            if (name == null)
                name = "";
            else name = name.trim();
            if (name.isEmpty()) {
                name = getString(R.string.dafault_measurement_name) + " #" + session.getId();
            }
            if (session.getDateStarted() != null)
                date = DATE_FORMAT.format(session.getDateStarted());
            rr = collectDataInBackground(session);
            int oldArtifactsFiltered = artifactsFiltered;
            artifactsFiltered = 0;
            for (int i=0; i<filterCount; i++) {
                artifactsFiltered += FILTER.getArtifactsCount(rr);
                rr = FILTER.doFilter(rr);
            }
            artifactsRemoved = artifactsFiltered - oldArtifactsFiltered;
            artifactsLeft = FILTER.getArtifactsCount(rr);
            return rr;
        }

        @Override
        protected void onPostExecute(double[] rr) {
            AbstractSessionReportFragment.this.sessionName.setText(name);
            if (date != null)
                sessionDate.setText(date);
            else sessionDate.setVisibility(View.GONE);
            displayData(rr);
            hideProgress();
            refreshing = false;
            if (filterCount > 0 && artifactsLeft == 0) {
                Toast.makeText(
                        getActivity(),
                        R.string.no_more_artifacts_detected,
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }

    private class SaveThisRevisionTask extends AsyncTask {;

        @Override
        protected Object doInBackground(Object[] params) {
            final RuntimeExceptionDao<HRSessionEntity, Long> sessionDAO = getSessionDao();
            final HRSessionEntity session = sessionDAO.queryForId(sessionId);
            session.setOriginalSessionId(sessionId);
            session.setStatus(SessionStatus.COMPLETED);
            session.setExternalId(null);
            session.setId(null);

            String name = session.getName();
            if (name == null || name.trim().isEmpty()) {
                name = "Measurement #" + sessionId;
            } else name = name.trim();

            name += " (corrected)";
            session.setName(name);

            final RuntimeExceptionDao<RRIntervalEntity, Long> hrDAO = getRRIntervalDao();
            try {
                final List<RRIntervalEntity> items = hrDAO.queryBuilder()
                        .orderBy("_id", true).where().eq("session_id", session.getId())
                        .query();
                int i = 0;
                long duration = 0L;
                for (RRIntervalEntity item : items) {
                    item.setId(null);
                    item.setRrTime(rr[i]);
                    int bpm = 0;
                    if (rr[i] < 1) {
                        bpm = 0;
                    } else {
                        bpm = (int) Math.round(60 * 1000.0 / rr[i]);
                    }
                    duration += Math.round(rr[i]);
                    item.setHeartBeatsPerMinute(bpm);
                    i++;
                }
                session.setDateStarted(new Date());
                session.setDateEnded(new Date(session.getDateStarted().getTime() + duration));
                sessionDAO.callBatchTasks(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        sessionDAO.createIfNotExists(session);
                        for (RRIntervalEntity item : items) {
                            item.setSessionId(session.getId());
                            hrDAO.create(item);
                        }
                        return null;
                    }
                });
                return session;
            } catch (SQLException ex) {
                Log.w(TAG, "SaveThisRevisionTask.doInBackground() failed", ex);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            Activity activity = getActivity();
            HRSessionEntity session = (HRSessionEntity) result;
            if (result != null && activity != null) {
                Intent intent = new Intent(activity, SessionDetailsActivity.class);
                intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, session.getId());
                intent.putExtra(SessionDetailsActivity.POST_RENDER_ACTION_EXTRA, SessionDetailsActivity.RENAME_ACTION);
                startActivity(intent);
                activity.finish();
            }
        }
    }

}
