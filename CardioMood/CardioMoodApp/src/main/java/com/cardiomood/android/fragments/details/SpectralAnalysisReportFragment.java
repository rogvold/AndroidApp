package com.cardiomood.android.fragments.details;

import android.os.Bundle;

import com.cardiomood.android.R;
import com.cardiomood.android.db.entity.ContinuousSessionEntity;
import com.cardiomood.math.spectrum.SpectralAnalysis;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.NumberRange;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.List;


public class SpectralAnalysisReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = SpectralAnalysisReportFragment.class.getSimpleName();

    private SpectralAnalysis sa = null;

    public SpectralAnalysisReportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Axis getXAxis() {
        return new NumberAxis();
    }

    @Override
    protected Axis getYAxis() {
        return new NumberAxis();
    }

    @Override
    protected void collectDataInBackground(ContinuousSessionEntity session, double[] time, double[] rrFiltered) {
        sa = new SpectralAnalysis(time, rrFiltered);
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("Spectral Power");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("Frequency, Hz");
        xAxis.getStyle().getTickStyle().setLabelTextSize(10);
        xAxis.getStyle().getTitleStyle().setTextSize(12);

        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("Power, ms2/Hz");
        yAxis.getStyle().getTickStyle().setLabelTextSize(10);
        yAxis.getStyle().getTitleStyle().setTextSize(12);

        // prepare source data
        double time[] = new double[rr.length];
        for (int i=1; i<rr.length; i++)
            time[i] = time[i-1] + rr[i];
        double bpm[] = new double[rr.length];
        for (int i=0; i<rr.length; i++)
            bpm[i] = 1000*60/rr[i];

        // Spectrum Chart
        double[] power = sa.getPower();
        PolynomialSplineFunction spectrum = sa.getSplinePower();


        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        xAxis.setDefaultRange(new NumberRange(0.0, 0.45));

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s: series)
            chart.removeSeries(s);

        SimpleDataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
        double maxFreq = sa.toFrequency(power.length-1), freq = 0;
        while(freq < maxFreq) {
            dataAdapter2.add(new DataPoint<Double, Double>(freq, spectrum.value(freq)));
            freq += 0.0005;
        }

        LineSeries series2 = new LineSeries();
        series2.setDataAdapter(dataAdapter2);
        chart.addSeries(series2);
        chart.redrawChart();
    }

    @Override
    protected int getBottomCustomLayoutId() {
        return R.layout.fragment_spectral_analysis_report_bottom;
    }
}
