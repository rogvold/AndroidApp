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
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.stat.StatUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;


public class HistogramReportFragment extends Fragment {

    private static final String TAG = HistogramReportFragment.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    private static final double[] GOOD_RR = new double[] {
            695,710,710,703,710,679,695,664,687,656,671,679,710,703,726,718,710,718,734,695,710,726,718,710,703,710,703,726,703,710,703,726,695,718,710,718,726,710,710,718,695,695,703,695,718,726,734,718,750,734,734,710,718,726,710,718,703,710,687,687,679,679,679,671,664,640,648,632,640,656,656,664,703,703,726,757,757,750,726,734,687,695,679,679,664,679,695,695,679,671,656,648,664,671,679,718,710,742,726,710,679,664,664,640,617,617,617,625,625,664,687,695,695,718,718,718,718,742,726,710,679,687,687,726,718,710,703,710,695,679,671,671,656,632,640,617,632,656,687,718,757,773,789,765,757,742,757,734,742,742,710,726,687,671,640,632,640,664,664,656,671,679,656,656,625,640,625,609,609,601,609,585,585,562,578,570,539,554,539,562,554,546,554,554,585,585,617,617,640,656,671,656,664,664,640,648,671,664,648,656,625,632,617,617,632,617,625,601,609,593,593,570,593,570,578,585,585,609,648,664,671,679,679,687,664,679,656,671,664,679,656,656,640,640,609,617,601,593,570,593,609,625,679,687,718,726,742,734,726,726,718,726,679,703,718,710,710,687,664,664,648,648,648,656,671,687,687,671,664,679,664,671,671,687,671,656,656,648,648,632,632,609,640,648,648,640,679,687,710,695,718,734,734,710,718,718,718,703,664,664,648,671,664,710,734,750,742,734,710,710,679,679,648,656,625,625,617,632,625,617,625,601,593,593,593,601,632,664,687,703,703,695,710,695,687,656,664,648,656,648,664,640,656,640,640,632,617,625,617,640,640,648,656,851,828,781,757,710,710,679,687,664,695,671,671,664,687,656,656,648,640,632,632,609,625,601,601,601,601,617    };

    public static final String ARG_SESSION_ID = "com.cardiomood.android.fragments.extra.SESSION_ID";

    private HeartRateDataItemDAO hrDAO;
    private HeartRateSessionDAO sessionDAO;

    // Components in this fragment view:
    private ScrollView scrollView;
    private LinearLayout progress;

    private TextView sessionName;
    private TextView sessionDate;

    private ShinobiChart histogramChart;
    private ColumnSeries mySeries;
    private ColumnSeries goodSeries;

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
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.getStyle().setInterSeriesSetPadding(2.0f);
        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        xAxis.setMajorTickFrequency(100.0);
        xAxis.getStyle().getTickStyle().setMinorTicksShown(false);
        xAxis.getStyle().getTickStyle().setMajorTicksShown(true);
        xAxis.getStyle().getTickStyle().setLabelTextSize(10);
        histogramChart.setXAxis(xAxis);

        NumberAxis yAxis = new NumberAxis();
        yAxis.getStyle().getTickStyle().setLabelTextSize(10);
        histogramChart.setYAxis(yAxis);

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(histogramChart.getSeries());
        for (Series<?> s: series)
            histogramChart.removeSeries(s);


        histogramChart.addSeries(mySeries = getSeriesForIntervals(rr));
        //histogramChart.addSeries(goodSeries = getSeriesForIntervals(GOOD_RR));

//        histogramChart.getLegend().setVisibility(View.VISIBLE);
//        histogramChart.getLegend().setPlacement(Legend.Placement.INSIDE_PLOT_AREA);

        mySeries.setTitle("My results");
        //goodSeries.setTitle("Good results");

        histogramChart.redrawChart();
    }

    private ColumnSeries getSeriesForIntervals(double rr[]) {
        DataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
        double maxRR = StatUtils.max(rr);
        double minRR = StatUtils.min(rr);
        Histogram histogram = new Histogram(rr, 50);
        if (minRR < 100)
            minRR = 100;
        for (double x=Math.floor((minRR-100)/50)*50; x<=Math.ceil((maxRR+50)/50)*50; x+=50) {
            if (x <= maxRR)
                dataAdapter2.add(new DataPoint<Double, Double>(x, (double)histogram.getCountFor(x)));
            else
                dataAdapter2.add(new DataPoint<Double, Double>(x, 0.0));
        }

        ColumnSeries series2 = new ColumnSeries();
        series2.setDataAdapter(dataAdapter2);
        return series2;
    }

    private void showProgress() {
        scrollView.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        ((ContentLoadingProgressBar) progress.findViewById(R.id.content_loading)).show();
    }

    private void hideProgress() {
        progress.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        ((ContentLoadingProgressBar) progress.findViewById(R.id.content_loading)).hide();
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
