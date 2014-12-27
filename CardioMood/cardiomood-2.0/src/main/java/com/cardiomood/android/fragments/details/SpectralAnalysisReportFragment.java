package com.cardiomood.android.fragments.details;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.math.spectrum.SpectralAnalysis;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.ChartView;
import com.shinobicontrols.charts.DataAdapter;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.NumberRange;
import com.shinobicontrols.charts.PieSeries;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.SeriesStyle;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;


public class SpectralAnalysisReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = SpectralAnalysisReportFragment.class.getSimpleName();

    private volatile SpectralAnalysis sa = null;
    private volatile double power[] = null;
    private volatile UnivariateFunction spectrum = null;

    @InjectView(R.id.TP) @Optional TextView tpView;
    @InjectView(R.id.VLF) @Optional TextView vlfView;
    @InjectView(R.id.LF) @Optional TextView lfView;
    @InjectView(R.id.HF) @Optional TextView hfView;
    @InjectView(R.id.LF_over_HF) @Optional TextView lfOverHfView;
    @InjectView(R.id.VLF_over_HF) @Optional TextView vlfOverHfView;
    @InjectView(R.id.IC) @Optional TextView icView;
    @InjectView(R.id.ULF) @Optional TextView ulfView;
    @InjectView(R.id.spectrum_pie_chart) @Optional ChartView pieChartView;

    private ShinobiChart pieChart;

    private final Object lock = new Object();

    public SpectralAnalysisReportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pieChartView.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        pieChartView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        pieChartView.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.inject(this, v);

        chartView.onCreate(savedInstanceState);
        pieChart = pieChartView.getShinobiChart();
        pieChart.setLicenseKey(ConfigurationConstants.SHINOBI_CHARTS_API_KEY);

        // style the Pie chart (default)
        pieChart.getStyle().setPlotAreaBackgroundColor(Color.TRANSPARENT);
        pieChart.getStyle().setBackgroundColor(Color.TRANSPARENT);
        pieChart.getStyle().setCanvasBackgroundColor(Color.TRANSPARENT);

        return v;
    }

    @Override
    protected Axis createXAxis() {
        return new NumberAxis();
    }

    @Override
    protected Axis createYAxis() {
        return new NumberAxis();
    }

    @Override
    protected void collectDataInBackground(SessionEntity session, double[] time, double[] rrFiltered) {
        synchronized (lock) {
            sa = new SpectralAnalysis(time, rrFiltered);
            // Spectrum Chart
            power = sa.getPower();
            spectrum = sa.interpolatePower(SpectralAnalysis.LINEAR_INTERPOLATOR);
        }
    }

    @Override
    protected void displayData(double[] rr) {
        // main chart
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
        List<Series<?>> allSeries = new ArrayList<>(chart.getSeries());
        for (Series<?> s: allSeries)
            chart.removeSeries(s);

        allSeries = new ArrayList<>(pieChart.getSeries());
        for (Series<?> s: allSeries)
            pieChart.removeSeries(s);

        synchronized (lock) {
            double maxVal = StatUtils.max(power) * 1e-6 * 1.05d;

            // show ranges
            SimpleDataAdapter<Double, Double> ulfRangeAdapter = new SimpleDataAdapter<>();
            ulfRangeAdapter.add(new DataPoint<>(0d, maxVal));
            ulfRangeAdapter.add(new DataPoint<>(0.00333d, maxVal));
            LineSeries ulfRange = new LineSeries();
            ulfRange.setDataAdapter(ulfRangeAdapter);
            ulfRange.getStyle().setFillStyle(SeriesStyle.FillStyle.FLAT);
            ulfRange.getStyle().setAreaColor(Color.argb(90, 255, 0, 0));
            ulfRange.getStyle().setAreaLineColor(Color.TRANSPARENT);
            chart.addSeries(ulfRange);

            SimpleDataAdapter<Double, Double> vlfRangeAdapter = new SimpleDataAdapter<>();
            vlfRangeAdapter.add(new DataPoint<>(0.00333d, maxVal));
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
            while (freq < maxFreq) {
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

            // numbers
            double vlfPct = sa.getVLF() / (sa.getTP() - sa.getULF()) * 100;
            double lfPct = sa.getLF() / (sa.getTP() - sa.getULF()) * 100;
            double hfPct = sa.getHF() / (sa.getTP() - sa.getULF()) * 100;

            tpView.setText(Html.fromHtml(String.valueOf(
                    Math.round(sa.getTP())) + " ms<sup><font size='-1'>2</font></sup> = 100%"
            ));
            vlfView.setText(Html.fromHtml(String.valueOf(
                    Math.round(sa.getVLF())) + " ms<sup><font size='-1'>2</font></sup> = "
                    + Math.round(vlfPct*100)/100.0 + "%"
            ));
            lfView.setText(Html.fromHtml(String.valueOf(
                            Math.round(sa.getLF())) + " ms<sup><font size='-1'>2</font></sup> = "
                            + Math.round(lfPct*100)/100.0 + "%"
            ));
            hfView.setText(Html.fromHtml(String.valueOf(
                            Math.round(sa.getHF())) + " ms<sup><font size='-1'>2</font></sup> = "
                            + Math.round(hfPct*100)/100.0 + "%"
            ));
            ulfView.setText(Html.fromHtml(String.valueOf(
                            Math.round(sa.getULF())) + " ms<sup><font size='-1'>2</font></sup>"
            ));

            lfOverHfView.setText(String.valueOf(
                    Math.round(100*sa.getLF()/sa.getHF()) / 100.0
            ));
            vlfOverHfView.setText(String.valueOf(
                    Math.round(100*sa.getVLF()/sa.getHF()) / 100.0
            ));
            icView.setText(String.valueOf(
                    Math.round(100*sa.getIC()) / 100.0
            ));

            // Pie Chart
            DataAdapter<String, Double> pieAdapter = new SimpleDataAdapter<>();
            pieAdapter.add(new DataPoint<>("VLF%", vlfPct));
            pieAdapter.add(new DataPoint<>("LF%", lfPct));
            pieAdapter.add(new DataPoint<>("HF%", hfPct));
            PieSeries pieSeries = new PieSeries();
            pieSeries.getStyle().setFlavorColors(
                    new int[] {
                            Color.argb(200, 0, 160, 0),
                            Color.argb(200, 160, 50, 0),
                            Color.argb(200, 0, 0, 160)
                    }
            );
            pieSeries.getStyle().setCrustThickness(1.5f);
            pieSeries.setDataAdapter(pieAdapter);
            pieChart.addSeries(pieSeries);
        }

        chart.redrawChart();
        pieChart.redrawChart();
    }

    @Override
    protected int getBottomCustomLayoutId() {
        return R.layout.fragment_spectral_analysis_report_bottom;
    }
}
