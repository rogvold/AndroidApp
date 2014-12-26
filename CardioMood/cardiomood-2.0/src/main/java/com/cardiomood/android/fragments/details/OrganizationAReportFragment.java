package com.cardiomood.android.fragments.details;

import android.os.Bundle;

import com.cardiomood.android.R;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.interpolation.ConstrainedSplineInterpolator;
import com.cardiomood.math.window.DataWindow;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.List;


public class OrganizationAReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = OrganizationAReportFragment.class.getSimpleName();

    private double[][] A = new double[0][0];

    public OrganizationAReportFragment() {
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
        A = HeartRateUtils.getA(rrFiltered, new DataWindow.IntervalsCount(100, 5));
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("Gorgo \"A\" Organization");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("Time, s");

        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("\"A\" Organization");

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s : series)
            chart.removeSeries(s);

        if (A[0].length > 2) {
            PolynomialSplineFunction stress = new ConstrainedSplineInterpolator().interpolate(A[0], A[1]);

            xAxis.enableGesturePanning(true);
            xAxis.enableGestureZooming(true);

            SimpleDataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
            double t = A[0][0];
            while (t <= A[0][A[0].length-1]) {
                dataAdapter2.add(new DataPoint<>(t/1000, stress.value(t)));
                t += 200;
            }

            LineSeries series2 = new LineSeries();
            series2.getStyle().setLineColor(getResources().getColor(R.color.colorAccent));
            series2.setDataAdapter(dataAdapter2);
            chart.addSeries(series2);
            chart.redrawChart();
        }
    }

}
