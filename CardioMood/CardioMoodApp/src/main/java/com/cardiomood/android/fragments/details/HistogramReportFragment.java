package com.cardiomood.android.fragments.details;

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
import com.cardiomood.math.histogram.Histogram;
import com.flurry.android.FlurryAgent;
import com.shinobicontrols.charts.CategoryAxis;
import com.shinobicontrols.charts.ChartView;
import com.shinobicontrols.charts.ColumnSeries;
import com.shinobicontrols.charts.DataAdapter;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.stat.StatUtils;

import java.text.DateFormat;
import java.util.List;


public class HistogramReportFragment extends Fragment {

    private static final String TAG = HistogramReportFragment.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    public static final String ARG_SESSION_ID = "com.cardiomood.android.fragments.extra.SESSION_ID";

    private HeartRateDataItemDAO hrDAO;
    private HeartRateSessionDAO sessionDAO;

    // Components in this fragment view:
    private ScrollView scrollView;
    private LinearLayout progress;

    private TextView sessionName;
    private TextView sessionDate;

    private ShinobiChart histogramChart;

    // Fragment parameters
    private long sessionId;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment OveralSessionReportFragment.
     */
    public static HistogramReportFragment newInstance(long sessionId) {
        HistogramReportFragment fragment = new HistogramReportFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    public HistogramReportFragment() {
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
        View v = inflater.inflate(R.layout.fragment_histogram_report, container, false);
        progress = (LinearLayout) v.findViewById(R.id.progress);
        scrollView = (ScrollView) v.findViewById(R.id.scrollView);

        // Spectrum chart
        histogramChart = ((ChartView) v.findViewById(R.id.histogram_chart)).getShinobiChart();
        histogramChart.setTitle("Histogram");
        histogramChart.setLicenseKey(ConfigurationConstants.SHINOBI_CHARTS_API_KEY);

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
        inflater.inflate(R.menu.fragment_histogram_report, menu);
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

        // Histogram Chart
        Histogram histogram = new Histogram(rr, 50);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.getStyle().setInterSeriesSetPadding(0.0f);
        xAxis.enableGesturePanning(true);
//        xAxis.enableGestureZooming(true);
        histogramChart.setXAxis(xAxis);

        NumberAxis yAxis = new NumberAxis();
        histogramChart.setYAxis(yAxis);

        DataAdapter<String, Double> dataAdapter2 = new SimpleDataAdapter<String, Double>();
        double maxRR = StatUtils.max(rr);
        double minRR = StatUtils.min(rr);
        for (double x=minRR-200; x<=maxRR+200; x+=50) {
            if (x <= maxRR)
                dataAdapter2.add(new DataPoint<String, Double>(String.valueOf((int)x), (double)histogram.getCountFor(x)));
            else
                dataAdapter2.add(new DataPoint<String, Double>(String.valueOf((int)x), 0.0));
        }

        ColumnSeries series2 = new ColumnSeries();
        series2.setDataAdapter(dataAdapter2);
        histogramChart.addSeries(series2);

        histogramChart.redrawChart();
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

        double[] rr = null;

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

            rr = new double[items.size()];
            for (int i=0; i<items.size(); i++) {
                rr[i] = items.get(i).getRrTime();
            }
            math = new HeartRateMath(rr);
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            HistogramReportFragment.this.sessionName.setText(sessionName);
            HistogramReportFragment.this.sessionDate.setText(sessionDate);
            initCharts(math);
            hideProgress();
        }
    }

}
