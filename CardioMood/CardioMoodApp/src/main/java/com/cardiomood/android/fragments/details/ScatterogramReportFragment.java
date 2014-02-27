package com.cardiomood.android.fragments.details;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.math.HeartRateMath;
import com.flurry.android.FlurryAgent;
import com.shinobicontrols.charts.ChartView;
import com.shinobicontrols.charts.DataAdapter;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.NumberRange;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.stat.StatUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;


public class ScatterogramReportFragment extends Fragment {

    private static final String TAG = ScatterogramReportFragment.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    public static final String ARG_SESSION_ID = "com.cardiomood.android.fragments.extra.SESSION_ID";

    private HeartRateDataItemDAO hrDAO;
    private HeartRateSessionDAO sessionDAO;

    // Components in this fragment view:
    private ScrollView scrollView;
    private LinearLayout progress;

    private TextView sessionName;
    private TextView sessionDate;

    private ShinobiChart scatterogramChart;

    // Fragment parameters
    private long sessionId;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment OveralSessionReportFragment.
     */
    public static ScatterogramReportFragment newInstance(long sessionId) {
        ScatterogramReportFragment fragment = new ScatterogramReportFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    public ScatterogramReportFragment() {
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

            hrDAO = new HeartRateDataItemDAO();
            sessionDAO = new HeartRateSessionDAO();

            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_scatterogram_report, container, false);
        progress = (LinearLayout) v.findViewById(R.id.progress);
        scrollView = (ScrollView) v.findViewById(R.id.scrollView);

        // Spectrum chart
        scatterogramChart = ((ChartView) v.findViewById(R.id.scatterogram_chart)).getShinobiChart();
        scatterogramChart.setTitle("Scatterogram");
        scatterogramChart.setLicenseKey(ConfigurationConstants.SHINOBI_CHARTS_API_KEY);

        sessionName = (TextView) v.findViewById(R.id.session_title);
        sessionDate = (TextView) v.findViewById(R.id.session_date);

        new DataLoadingTask().execute(sessionId);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_scatterogram_report, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                FlurryAgent.logEvent("menu_refresh_clicked");
                new DataLoadingTask().execute(sessionId);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void initCharts(HeartRateMath hrm) {
        // prepare source data
        double rr[] = hrm.getRrIntervals();

        NumberAxis xAxis = new NumberAxis();
        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        xAxis.setDefaultRange(new NumberRange(StatUtils.min(rr)-100, StatUtils.max(rr)+100));
        scatterogramChart.setXAxis(xAxis);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setDefaultRange(new NumberRange(StatUtils.min(rr)-100, StatUtils.max(rr)+100));
        yAxis.enableGesturePanning(true);
        yAxis.enableGestureZooming(true);
        scatterogramChart.setYAxis(yAxis);

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(scatterogramChart.getSeries());
        for (Series<?> s: series)
            scatterogramChart.removeSeries(s);

        DataAdapter<Double, Double> dataAdapter1 = new SimpleDataAdapter<Double, Double>();
        for (double i=0; i<1500; i+=50)
            dataAdapter1.add(new DataPoint<Double, Double>(i, i));
        LineSeries series1 = new LineSeries();
        series1.setDataAdapter(dataAdapter1);
        scatterogramChart.addSeries(series1);

        DataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
        for (int i=1; i<rr.length; i++) {
            dataAdapter2.add(new DataPoint<Double, Double>(rr[i-1], rr[i]));
        }

        LineSeries series2 = new LineSeries();
        series2.setDataAdapter(dataAdapter2);
        series2.getStyle().setLineShown(false);
        series2.getStyle().getPointStyle().setPointsShown(true);
        series2.getStyle().getPointStyle().setInnerColor(Color.RED);
        series2.getStyle().getPointStyle().setColor(Color.BLACK);
        series2.getStyle().getPointStyle().setRadius(1.0f);
        scatterogramChart.addSeries(series2);

        scatterogramChart.redrawChart();
    }

    private void showProgress() {
        scrollView.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progress.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
    }

    private class DataLoadingTask extends AsyncTask<Long, Void, Void> {

        private String sessionName = null;
        private String sessionDate = null;
        private HeartRateMath math;

        @Override
        protected void onPreExecute() {
            showProgress();
        }

        @Override
        protected Void doInBackground(Long... params) {
            List<HeartRateDataItem> items = hrDAO.getItemsBySessionId(sessionId);

            HeartRateSession session = sessionDAO.findById(sessionId);
            String name = session.getName();
            if (name == null || name.isEmpty()) {
                name = getText(R.string.dafault_measurement_name) + " #" + sessionId;
            }
            this.sessionName = name;
            this.sessionDate = DATE_FORMAT.format(session.getDateStarted());

            double[] rr = new double[items.size()];
            for (int i=0; i<items.size(); i++) {
                rr[i] = items.get(i).getRrTime();
            }
            math = new HeartRateMath(rr);
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            ScatterogramReportFragment.this.sessionName.setText(sessionName);
            ScatterogramReportFragment.this.sessionDate.setText(sessionDate);
            initCharts(math);
            hideProgress();
        }
    }

}
