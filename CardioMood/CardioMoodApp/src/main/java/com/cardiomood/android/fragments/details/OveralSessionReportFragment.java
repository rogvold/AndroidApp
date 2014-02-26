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
import com.flurry.android.FlurryAgent;
import com.shinobicontrols.charts.ChartView;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.text.DateFormat;
import java.util.List;


public class OveralSessionReportFragment extends Fragment {

    private static final String TAG = OveralSessionReportFragment.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    public static final String ARG_SESSION_ID = "com.cardiomood.android.fragments.extra.SESSION_ID";

    private HeartRateDataItemDAO hrDAO;
    private HeartRateSessionDAO sessionDAO;

    // Components in this fragment view:
    private ScrollView scrollView;
    private LinearLayout progress;

    private TextView sessionName;
    private TextView sessionDate;
    private TextView meanHeartRate;
    private TextView meanStressIndex;

    private ShinobiChart heartRateChart;
    private ShinobiChart spectrumChart;

    // Fragment parameters
    private long sessionId;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment OveralSessionReportFragment.
     */
    public static OveralSessionReportFragment newInstance(long sessionId) {
        OveralSessionReportFragment fragment = new OveralSessionReportFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    public OveralSessionReportFragment() {
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
        View v = inflater.inflate(R.layout.fragment_overal_session_report, container, false);
        progress = (LinearLayout) v.findViewById(R.id.progress);
        scrollView = (ScrollView) v.findViewById(R.id.scrollView);

        // Heart rate chart
        heartRateChart = ((ChartView) v.findViewById(R.id.heart_rate_chart)).getShinobiChart();
        heartRateChart.setTitle("Heart Rate");
        heartRateChart.setLicenseKey(ConfigurationConstants.SHINOBI_CHARTS_API_KEY);

//        // Spectrum chart
//        spectrumChart = ((ChartView) v.findViewById(R.id.spectrum_chart)).getShinobiChart();
//        spectrumChart.setTitle("Spectrum");
//        spectrumChart.setLicenseKey(ConfigurationConstants.SHINOBI_CHARTS_API_KEY);

        sessionName = (TextView) v.findViewById(R.id.session_title);
        sessionDate = (TextView) v.findViewById(R.id.session_date);
        meanHeartRate = (TextView) v.findViewById(R.id.mean_hear_rate);
        meanStressIndex = (TextView) v.findViewById(R.id.mean_stress);

        showProgress();

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        new DataLoadingTask().execute(sessionId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_overal_session_report, menu);
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
        double time[] = hrm.getTime();
        double bpm[] = new double[rr.length];
        for (int i=0; i<rr.length; i++)
            bpm[i] = 1000*60/rr[i];

        SplineInterpolator interpol = new SplineInterpolator();
        PolynomialSplineFunction f = interpol.interpolate(time, bpm);

        // Heart Rate Chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        heartRateChart.setXAxis(xAxis);

        NumberAxis yAxis = new NumberAxis();
        heartRateChart.setYAxis(yAxis);

        SimpleDataAdapter<Double, Double> dataAdapter1 = new SimpleDataAdapter<Double, Double>();
        double t = 0;
        while(t < time[time.length-1]) {
            dataAdapter1.add(new DataPoint<Double, Double>(t/1000, f.value(t)));
            t += 50;
        }

        LineSeries series1 = new LineSeries();
        series1.setDataAdapter(dataAdapter1);
        heartRateChart.addSeries(series1);
        heartRateChart.redrawChart();

        // Spectrum Chart
//        SpectralAnalysis sa = new SpectralAnalysis(time, rr);
//        double[] power = sa.getPower();
//        PolynomialSplineFunction spectrum = sa.getSplinePower();
//
//        xAxis = new NumberAxis();
//        xAxis.enableGesturePanning(true);
//        xAxis.enableGestureZooming(true);
//        spectrumChart.setXAxis(xAxis);
//
//        yAxis = new NumberAxis();
//        spectrumChart.setYAxis(yAxis);
//
//        SimpleDataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
//        double maxFreq = sa.toFrequency(power.length-1), freq = 0;
//        while(freq < maxFreq) {
//            dataAdapter2.add(new DataPoint<Double, Double>(freq, spectrum.value(freq)));
//            freq += 0.0005;
//        }
//
//        LineSeries series2 = new LineSeries();
//        series2.setDataAdapter(dataAdapter2);
//        spectrumChart.addSeries(series2);
//        spectrumChart.redrawChart();
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
        private String meanStress = null;
        private String meanHeartRate = null;
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
            this.meanHeartRate = "" + Math.round(60*1000/math.getMean());
            this.meanStress = "" + Math.round(math.getTotalStressIndex());
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            OveralSessionReportFragment.this.sessionName.setText(sessionName);
            OveralSessionReportFragment.this.sessionDate.setText(sessionDate);
            OveralSessionReportFragment.this.meanHeartRate.setText(meanHeartRate);
            OveralSessionReportFragment.this.meanStressIndex.setText(meanStress);
            initCharts(math);
            hideProgress();
        }
    }

}