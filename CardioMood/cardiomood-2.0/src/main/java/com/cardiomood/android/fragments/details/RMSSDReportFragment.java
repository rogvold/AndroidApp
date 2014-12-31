package com.cardiomood.android.fragments.details;

import android.os.Bundle;

import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.expert.R;
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.parameters.RMSSDValue;
import com.cardiomood.math.window.DataWindow;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.List;


public class RMSSDReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = RMSSDReportFragment.class.getSimpleName();

    private double[][] RMSSD = new double[0][0];
    private final Object lock = new Object();

    public RMSSDReportFragment() {
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
            RMSSD = HeartRateUtils.getWindowedParameter(
                    new RMSSDValue(),
                    rrFiltered,
                    0,
                    rrFiltered.length,
                    new DataWindow.Timed(2 * 60 * 1000, 5000)
            );
        }
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("RMSSD (2 min window)");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("Time, s");

        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("RMSSD, ms");

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s : series)
            chart.removeSeries(s);

        synchronized (lock) {
            if (RMSSD[0].length > 2) {
                PolynomialSplineFunction stress = new SplineInterpolator().interpolate(RMSSD[0], RMSSD[1]);

                xAxis.enableGesturePanning(true);
                xAxis.enableGestureZooming(true);

                SimpleDataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
                double t = RMSSD[0][0];
                while (t <= RMSSD[0][RMSSD[0].length - 1]) {
                    dataAdapter2.add(new DataPoint<>(t / 1000, stress.value(t)));
                    t += 200;
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
