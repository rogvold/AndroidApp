package com.cardiomood.android.fragments.details;

import com.cardiomood.android.R;
import com.cardiomood.android.db.entity.SessionEntity;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.NumberRange;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Anton Danshin on 27/12/14.
 */
public class TextReportFragment extends AbstractSessionReportFragment {

    private double rr[] = null;
    private double time[] = null;
    private TextReport report = null;

    private final Object lock = new Object();

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
            this.rr = rrFiltered.clone();
            this.time = time.clone();

            report = new TextReport.Builder().setRRIntervals(this.rr)
                    .setStartDate(new Date(session.getStartTimestamp())).build();
        }
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("RR-intervals");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("Time, s");
        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("RR, ms");

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s : series)
            chart.removeSeries(s);

        synchronized (lock) {
            xAxis.enableGesturePanning(true);
            xAxis.enableGestureZooming(true);
            xAxis.allowPanningOutOfDefaultRange(false);
            xAxis.setDefaultRange(new NumberRange(time[0], time[time.length - 1] / 1000));

            SimpleDataAdapter<Double, Double> dataAdapter1 = new SimpleDataAdapter<Double, Double>();
            for (int i = 0; i < time.length; i++)
                dataAdapter1.add(new DataPoint<>(time[i] / 1000, this.rr[i]));

            LineSeries series1 = new LineSeries();
            series1.getStyle().setLineColor(getResources().getColor(R.color.colorAccent));
            series1.setDataAdapter(dataAdapter1);
            chart.addSeries(series1);
        }

        chart.redrawChart();
    }
}
