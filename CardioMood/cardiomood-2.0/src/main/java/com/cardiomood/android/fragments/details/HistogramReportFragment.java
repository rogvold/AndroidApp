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
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.histogram.Histogram;
import com.cardiomood.math.histogram.Histogram128Ext;
import com.cardiomood.math.window.DataWindow;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.CategoryAxis;
import com.shinobicontrols.charts.ColumnSeries;
import com.shinobicontrols.charts.DataAdapter;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.SeriesStyle;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;


public class HistogramReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = HistogramReportFragment.class.getSimpleName();
    private Histogram histogram = new Histogram(new double[0], 50);
    private Histogram128Ext histogram128Ext = null;
    private double[] time;
    private double stressIndex;
    private double gorgoA;

    private double maxNN, minNN;

    @InjectView(R.id.Mo) @Optional
    TextView moView;
    @InjectView(R.id.AMo) @Optional
    TextView aMoView;
    @InjectView(R.id.Bayevsky) @Optional
    TextView bayevskyView;
    @InjectView(R.id.Gorgo) @Optional
    TextView gorgoView;
    @InjectView(R.id.MxDMn) @Optional
    TextView mxdmnView;
    @InjectView(R.id.WN1) @Optional
    TextView wn1View;
    @InjectView(R.id.WN4) @Optional
    TextView wn4View;
    @InjectView(R.id.WN5) @Optional
    TextView wn5View;
    @InjectView(R.id.HRVTi) @Optional
    TextView hrvtiView;

    public HistogramReportFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.inject(this, v);
        return v;
    }

    @Override
    protected Axis createXAxis() {
        return new CategoryAxis();
    }

    @Override
    protected Axis createYAxis() {
        return new NumberAxis();
    }

    @Override
    protected void collectDataInBackground(SessionEntity session, double[] time, double[] rrFiltered) {
        this.time = time;
        histogram = new Histogram(rrFiltered, 50);
        histogram128Ext = new Histogram128Ext(rrFiltered);

        double[] SI = HeartRateUtils.getSI(rrFiltered, new DataWindow.Timed(2 * 1000 * 60, 5000))[1];
        stressIndex = SI.length > 0 ? StatUtils.mean(SI) : HeartRateUtils.getSI(rrFiltered);

        double[] A = HeartRateUtils.getA(rrFiltered, new DataWindow.IntervalsCount(100, 5))[1];
        gorgoA = A.length > 0 ? StatUtils.mean(A) : HeartRateUtils.getA(rrFiltered);
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("Histogram");
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();

        // Histogram Chart
        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        xAxis.setMajorTickFrequency(100.0);
        xAxis.getStyle().getTickStyle().setMinorTicksShown(false);
        xAxis.getStyle().getTickStyle().setMajorTicksShown(true);
        xAxis.setTitle("NN-interval count");

        yAxis.setTitle("%");

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s: series)
            chart.removeSeries(s);


        List<ColumnSeries> dataSeries = getSeriesForIntervals(rr);
        for (Series<?> s: dataSeries) {
            chart.addSeries(s);
        }

        moView.setText(String.valueOf(Math.round(histogram.getMo())) + " ms");
        aMoView.setText(String.valueOf(Math.round(histogram.getAMo()*10)/10.0) + "%");
        bayevskyView.setText(Html.fromHtml(String.valueOf(Math.round(stressIndex)) + " s<sup>-2</sup>"));
        gorgoView.setText(String.valueOf(Math.round(gorgoA * 1000)/1000.0));
        mxdmnView.setText(String.valueOf(Math.round(histogram.getMxDMn())) + " ms");
        wn1View.setText(String.valueOf(Math.round(histogram128Ext.getWN1())) + " ms");
        wn4View.setText(String.valueOf(Math.round(histogram128Ext.getWN4())) + " ms");
        wn5View.setText(String.valueOf(Math.round(histogram128Ext.getWN5())) + " ms");
        hrvtiView.setText(String.valueOf(Math.round(histogram128Ext.getHRVTi()*100)/100.0));

//        xAxis.setDefaultRange(new NumberRange(minNN - 100, maxNN + 100));
        chart.redrawChart();
    }

    private List<ColumnSeries> getSeriesForIntervals(double rr[]) {
        DataAdapter<Double, Double> dataAdapter = new SimpleDataAdapter<>();
        double maxRR = StatUtils.max(rr);
        double minRR = StatUtils.min(rr);
        if (minRR < 100)
            minRR = 100;
        if (maxRR > 2000)
            maxRR = 2000;
        for (double x=Math.floor((minRR-100)/50)*50; x<=Math.ceil((maxRR+50)/50)*50; x+=50) {
            if (x <= maxRR && 100.0*histogram.getCountFor(x)/rr.length >= 4.0) {
                dataAdapter.add(new DataPoint<>(x/1000, Math.round(100 * 100.0 * histogram.getCountFor(x) / rr.length) / 100.0));
            } else
                dataAdapter.add(new DataPoint<>(x/1000, 0.0));
        }

        DataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<>();
        minNN = maxRR;
        maxNN = minRR;
        for (double x=Math.floor((minRR-100)/50)*50; x<=Math.ceil((maxRR+50)/50)*50; x+=50) {
            if (x >= minRR && x <= maxRR)
                continue;
            if (x <= maxRR && 100.0*histogram.getCountFor(x)/rr.length < 4.0) {
                dataAdapter2.add(new DataPoint<>(x/1000, Math.round(100 * 100.0 * histogram.getCountFor(x) / rr.length) / 100.0));
            } else {
                dataAdapter2.add(new DataPoint<>(x/1000, 0.0));
                if (x < minNN)
                    minNN = x;
                if (x > maxNN)
                    maxNN = x;
            }
        }

        ColumnSeries series = new ColumnSeries();
        series.getStyle().setLineShown(true);
        series.setDataAdapter(dataAdapter);

        ColumnSeries series2 = new ColumnSeries();
        series2.setDataAdapter(dataAdapter2);
        series2.getStyle().setAreaColor(Color.LTGRAY);
        series2.getStyle().setLineColor(Color.GRAY);
        series2.getStyle().setFillStyle(SeriesStyle.FillStyle.FLAT);
        series2.getStyle().setLineShown(true);

        return Arrays.asList(series, series2);
    }

    @Override
    protected int getBottomCustomLayoutId() {
        return R.layout.fragment_histogram_report_bottom;
    }
}
