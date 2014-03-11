package com.cardiomood.android.fragments.details;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
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

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.math.HeartRateMath;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.ChartView;
import com.shinobicontrols.charts.ShinobiChart;

import java.text.DateFormat;

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

    // Components in this fragment view:
    private ScrollView scrollView;
    private LinearLayout progressView;
    private ContentLoadingProgressBar progress;
    private TextView sessionName;
    private TextView sessionDate;
    private ShinobiChart chart;
    private FrameLayout topCustomSection;
    private FrameLayout bottomCustomSection;


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

        chart = ((ChartView) v.findViewById(R.id.chart)).getShinobiChart();
        chart.setLicenseKey(ConfigurationConstants.SHINOBI_CHARTS_API_KEY);

        int topCustomLayoutId = getTopCustomLayoutId();
        if (topCustomLayoutId != -1) {
            inflater.inflate(topCustomLayoutId, topCustomSection, true);
        }

        int bottomCustomLayoutId = getBottomCustomLayoutId();
        if (bottomCustomLayoutId != -1) {
            inflater.inflate(bottomCustomLayoutId, bottomCustomSection, true);
        }

        v.invalidate();

        if (xAxis == null)
            xAxis = getXAxis();
        if (yAxis == null)
            yAxis = getYAxis();

        chart.setXAxis(xAxis);
        chart.setYAxis(yAxis);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        refresh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_session_report, menu);
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

    protected void refresh() {
        new DataLoadingTask().execute(sessionId);
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

    protected abstract Axis getXAxis();
    protected abstract Axis getYAxis();
    protected abstract HeartRateMath collectDataInBackground(HeartRateSession session);
    protected abstract void displayData(HeartRateMath math);


    private class DataLoadingTask extends AsyncTask<Long, Void, HeartRateMath> {

        private HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO();
        private String name;
        private String date;

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected HeartRateMath doInBackground(Long... params) {
            long sessionId = params[0];
            HeartRateSession session = sessionDAO.findById(sessionId);
            name = session.getName();
            if (name == null)
                name = "";
            else name = name.trim();
            if (name.isEmpty()) {
                name = getString(R.string.dafault_measurement_name) + " #" + session.getId();
            }
            if (session.getDateStarted() != null)
                date = DATE_FORMAT.format(session.getDateStarted());
            return collectDataInBackground(session);
        }

        @Override
        protected void onPostExecute(HeartRateMath math) {
            AbstractSessionReportFragment.this.sessionName.setText(name);
            if (date != null)
                sessionDate.setText(date);
            else sessionDate.setVisibility(View.GONE);
            displayData(math);
            hideProgress();
        }

        public HeartRateSessionDAO getSessionDAO() {
            return sessionDAO;
        }

        public void setSessionDAO(HeartRateSessionDAO sessionDAO) {
            this.sessionDAO = sessionDAO;
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

}
