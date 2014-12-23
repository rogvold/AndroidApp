package com.cardiomood.android.fragments.details;

import android.graphics.Color;

import com.cardiomood.android.R;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.math.histogram.Histogram;
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


public class HistogramReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = HistogramReportFragment.class.getSimpleName();
    private Histogram histogram = new Histogram(new double[0], 50);

    public HistogramReportFragment() {
        // Required empty public constructor
    }

    @Override
    protected Axis getXAxis() {
        return new CategoryAxis();
    }

    @Override
    protected Axis getYAxis() {
        return new NumberAxis();
    }

    @Override
    protected void collectDataInBackground(SessionEntity session, double[] time, double[] rrFiltered) {
        histogram = new Histogram(rrFiltered, 50);
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
        xAxis.getStyle().getTickStyle().setLabelTextSize(10);
        xAxis.setTitle("NN-interval, ms");
        xAxis.getStyle().getTitleStyle().setTextSize(12);

        yAxis.getStyle().getTickStyle().setLabelTextSize(10);
        yAxis.setTitle("%");
        yAxis.getStyle().getTitleStyle().setTextSize(12);

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s: series)
            chart.removeSeries(s);


        List<ColumnSeries> dataSeries = getSeriesForIntervals(rr);
        for (Series<?> s: dataSeries) {
            chart.addSeries(s);
        }
        chart.redrawChart();
    }

    private List<ColumnSeries> getSeriesForIntervals(double rr[]) {
        DataAdapter<Integer, Double> dataAdapter = new SimpleDataAdapter<>();
        double maxRR = StatUtils.max(rr);
        double minRR = StatUtils.min(rr);
        if (minRR < 100)
            minRR = 100;
        for (double x=Math.floor((minRR-100)/50)*50; x<=Math.ceil((maxRR+50)/50)*50; x+=50) {
            if (x <= maxRR && 100.0*histogram.getCountFor(x)/rr.length >= 4.0) {
                dataAdapter.add(new DataPoint<>((int) x, Math.round(100 * 100.0 * histogram.getCountFor(x) / rr.length) / 100.0));
            } else
                dataAdapter.add(new DataPoint<>((int) x, 0.0));
        }

        DataAdapter<Integer, Double> dataAdapter2 = new SimpleDataAdapter<Integer, Double>();
        for (double x=Math.floor((minRR-100)/50)*50; x<=Math.ceil((maxRR+50)/50)*50; x+=50) {
            if (x >= minRR && x <= maxRR)
                continue;
            if (x <= maxRR && 100.0*histogram.getCountFor(x)/rr.length < 4.0) {
                dataAdapter2.add(new DataPoint<>((int) x, Math.round(100 * 100.0 * histogram.getCountFor(x) / rr.length) / 100.0));
            } else
                dataAdapter2.add(new DataPoint<>((int) x, 0.0));
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
