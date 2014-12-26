package com.cardiomood.android.fragments.details;

import android.graphics.Color;
import android.os.Bundle;

import com.cardiomood.android.R;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.math.spectrum.SpectralAnalysis;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.NumberRange;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.SeriesStyle;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;


public class SpectralAnalysisReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = SpectralAnalysisReportFragment.class.getSimpleName();

    private volatile SpectralAnalysis sa = null;
    private volatile double power[] = null;
    private volatile UnivariateFunction spectrum = null;

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
    protected void collectDataInBackground(SessionEntity session, double[] time, double[] rrFiltered) {
        sa = new SpectralAnalysis(time, rrFiltered);

        // Spectrum Chart
        power = sa.getPower();
        spectrum = sa.interpolatePower(SpectralAnalysis.CONSTRAINED_SPLINE_INTERPOLATOR);
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("Spectral Power");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("Frequency, Hz");

        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("Power, s^2/Hz");



        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        xAxis.setDefaultRange(new NumberRange(0.0, 0.42));

        // Clear
        List<Series<?>> allSeries = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s: allSeries)
            chart.removeSeries(s);

        double maxVal = StatUtils.max(power) * 1e-6 * 1.05d;

        // show ranges
        SimpleDataAdapter<Double, Double> ulfRangeAdapter = new SimpleDataAdapter<>();
        ulfRangeAdapter.add(new DataPoint<>(0d, maxVal));
        ulfRangeAdapter.add(new DataPoint<>(0.015d, maxVal));
        LineSeries ulfRange = new LineSeries();
        ulfRange.setDataAdapter(ulfRangeAdapter);
        ulfRange.getStyle().setFillStyle(SeriesStyle.FillStyle.FLAT);
        ulfRange.getStyle().setAreaColor(Color.argb(90, 255, 0, 0));
        ulfRange.getStyle().setAreaLineColor(Color.TRANSPARENT);
        chart.addSeries(ulfRange);

        SimpleDataAdapter<Double, Double> vlfRangeAdapter = new SimpleDataAdapter<>();
        vlfRangeAdapter.add(new DataPoint<>(0.015d, maxVal));
        vlfRangeAdapter.add(new DataPoint<>(0.04d, maxVal));
        LineSeries vlfRange = new LineSeries();
        vlfRange.setDataAdapter(vlfRangeAdapter);
        vlfRange.getStyle().setFillStyle(SeriesStyle.FillStyle.FLAT);
        vlfRange.getStyle().setAreaColor(Color.argb(90, 0, 255, 0));
        vlfRange.getStyle().setAreaLineColor(Color.TRANSPARENT);
        chart.addSeries(vlfRange);

        SimpleDataAdapter<Double, Double> lfRangeAdapter = new SimpleDataAdapter<>();
        lfRangeAdapter.add(new DataPoint<>(0.04d, maxVal));
        lfRangeAdapter.add(new DataPoint<>(0.15d, maxVal));
        LineSeries lfRange = new LineSeries();
        lfRange.setDataAdapter(lfRangeAdapter);
        lfRange.getStyle().setFillStyle(SeriesStyle.FillStyle.FLAT);
        lfRange.getStyle().setAreaColor(Color.argb(90, 255, 165, 0));
        lfRange.getStyle().setAreaLineColor(Color.TRANSPARENT);
        chart.addSeries(lfRange);

        SimpleDataAdapter<Double, Double> hfRangeAdapter = new SimpleDataAdapter<>();
        hfRangeAdapter.add(new DataPoint<>(0.15d, maxVal));
        hfRangeAdapter.add(new DataPoint<>(0.4d, maxVal));
        LineSeries hfRange = new LineSeries();
        hfRange.setDataAdapter(hfRangeAdapter);
        hfRange.getStyle().setFillStyle(SeriesStyle.FillStyle.FLAT);
        hfRange.getStyle().setAreaColor(Color.argb(80, 0, 0, 255));
        hfRange.getStyle().setAreaLineColor(Color.TRANSPARENT);
        chart.addSeries(hfRange);

        double maxFreq = sa.getMaxFrequency(), freq = 0;
        SimpleDataAdapter<Double, Double> dataAdapter = new SimpleDataAdapter<>();
        while(freq < maxFreq) {
            dataAdapter.add(new DataPoint<>(freq, spectrum.value(freq) * 1e-6));
            freq += 0.00025;
        }
        LineSeries series = new LineSeries();
        series.setDataAdapter(dataAdapter);
        chart.addSeries(series);

        SimpleDataAdapter<Double, Double> pointsAdapter = new SimpleDataAdapter<>();
        for (int i=1; i<power.length; i++) {
            pointsAdapter.add(new DataPoint<>(sa.toFrequency(i), power[i] * 1e-6));
        }
        LineSeries pointSeries = new LineSeries();
        pointSeries.setDataAdapter(pointsAdapter);
        chart.addSeries(pointSeries);

        series.getStyle().setLineColor(getResources().getColor(R.color.colorAccent));

        pointSeries.getStyle().setLineColor(Color.TRANSPARENT);
        pointSeries.getStyle().getPointStyle().setPointsShown(true);
        pointSeries.getStyle().getPointStyle().setInnerRadius(1.5f);
        pointSeries.getStyle().getPointStyle().setColor(Color.TRANSPARENT);
        pointSeries.getStyle().getPointStyle().setInnerColor(Color.BLACK);

        chart.redrawChart();
    }

    @Override
    protected int getBottomCustomLayoutId() {
        return R.layout.fragment_spectral_analysis_report_bottom;
    }
}
