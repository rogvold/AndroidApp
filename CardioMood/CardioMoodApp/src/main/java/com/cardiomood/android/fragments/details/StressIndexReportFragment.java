package com.cardiomood.android.fragments.details;

import android.os.Bundle;

import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.math.HeartRateUtils;
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


public class StressIndexReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = StressIndexReportFragment.class.getSimpleName();

    private HeartRateDataItemDAO hrDAO;

    private double[][] SI = new double[0][0];

    public StressIndexReportFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hrDAO = new HeartRateDataItemDAO();
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
    protected double[] collectDataInBackground(HeartRateSession session) {
        long sessionId = session.getId();
        List<HeartRateDataItem> items = hrDAO.getItemsBySessionId(sessionId);

        double[] rr = new double[items.size()];
        for (int i=0; i<items.size(); i++) {
            rr[i] = items.get(i).getRrTime();
        }

        SI = HeartRateUtils.getSI(rr, new DataWindow.Timed(2*60*1000, 5000));
        return rr;
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("Bayevsky Stress Index");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("Time, s");
        xAxis.getStyle().getTickStyle().setLabelTextSize(10);
        xAxis.getStyle().getTitleStyle().setTextSize(12);

        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("Stress Index");
        yAxis.getStyle().getTickStyle().setLabelTextSize(10);
        yAxis.getStyle().getTitleStyle().setTextSize(12);


        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s : series)
            chart.removeSeries(s);

        if (SI[0].length > 2) {
            PolynomialSplineFunction stress = new SplineInterpolator().interpolate(SI[0], SI[1]);

            xAxis.enableGesturePanning(true);
            xAxis.enableGestureZooming(true);

            SimpleDataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
            double t = SI[0][0];
            while (t <= SI[0][SI[0].length-1]) {
                dataAdapter2.add(new DataPoint<Double, Double>(t/1000, stress.value(t)));
                t += 200;
            }

            LineSeries series2 = new LineSeries();
            series2.setDataAdapter(dataAdapter2);
            chart.addSeries(series2);
            chart.redrawChart();
        }
    }

}
