package com.cardiomood.android.fragments.details;

import android.os.Bundle;

import com.cardiomood.android.R;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.parameters.SeluyanovIndexValue;
import com.cardiomood.math.window.DataWindow;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;


public class SeluyanovReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = SeluyanovReportFragment.class.getSimpleName();

    private double[][] Seluyanov = new double[0][0];
    private final Object lock = new Object();

    public SeluyanovReportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            Seluyanov = HeartRateUtils.getWindowedParameter(
                    new SeluyanovIndexValue(),
                    rrFiltered,
                    0,
                    rrFiltered.length,
                    new DataWindow.IntervalsCount(30, 5)
            );
        }
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("Seluyanov Index");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("Time, s");

        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("Seluyanov Index, ms");

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s : series)
            chart.removeSeries(s);

        synchronized (lock) {
            if (Seluyanov[0].length > 2) {
                UnivariateFunction stress = new LinearInterpolator().interpolate(Seluyanov[0], Seluyanov[1]);

                xAxis.enableGesturePanning(true);
                xAxis.enableGestureZooming(true);

                SimpleDataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
                double t = Seluyanov[0][0];
                while (t <= Seluyanov[0][Seluyanov[0].length - 1]) {
                    dataAdapter2.add(new DataPoint<>(t / 1000, stress.value(t)));
                    t += 250;
                }

                LineSeries series2 = new LineSeries();
                series2.getStyle().setLineColor(getResources().getColor(R.color.colorAccent));
                series2.setDataAdapter(dataAdapter2);
                chart.addSeries(series2);
            }
        }
        chart.redrawChart();
    }

}
