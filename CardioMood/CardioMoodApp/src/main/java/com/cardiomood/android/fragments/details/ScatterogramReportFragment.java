package com.cardiomood.android.fragments.details;

import android.graphics.Color;
import android.os.Bundle;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.math.HeartRateMath;
import com.shinobicontrols.charts.Axis;
import com.shinobicontrols.charts.DataAdapter;
import com.shinobicontrols.charts.DataPoint;
import com.shinobicontrols.charts.LineSeries;
import com.shinobicontrols.charts.NumberAxis;
import com.shinobicontrols.charts.NumberRange;
import com.shinobicontrols.charts.Series;
import com.shinobicontrols.charts.ShinobiChart;
import com.shinobicontrols.charts.SimpleDataAdapter;

import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;


public class ScatterogramReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = ScatterogramReportFragment.class.getSimpleName();

    private HeartRateDataItemDAO hrDAO;

    public ScatterogramReportFragment() {
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
    protected HeartRateMath collectDataInBackground(HeartRateSession session) {
        List<HeartRateDataItem> items = hrDAO.getItemsBySessionId(session.getId());
        double[] rr = new double[items.size()];
        for (int i=0; i<items.size(); i++) {
            rr[i] = items.get(i).getRrTime();
        }
        return new HeartRateMath(rr);
    }

    @Override
    protected void displayData(HeartRateMath hrm) {
        ShinobiChart chart = getChart();
        chart.setTitle("Scatterogram");
        Axis xAxis = chart.getXAxis();
        xAxis.setTitle("RR[i-1], ms");
        xAxis.getStyle().getTitleStyle().setTextSize(12);
        xAxis.getStyle().getTickStyle().setLabelTextSize(10);
        Axis yAxis = chart.getYAxis();
        yAxis.setTitle("RR[i], ms");
        yAxis.getStyle().getTitleStyle().setTextSize(12);
        yAxis.getStyle().getTickStyle().setLabelTextSize(10);

        double rr[] = hrm.getRrIntervals();

        xAxis.enableGesturePanning(true);
        xAxis.enableGestureZooming(true);
        xAxis.setDefaultRange(new NumberRange(StatUtils.min(rr)-100, StatUtils.max(rr)+100));

        yAxis.setDefaultRange(new NumberRange(StatUtils.min(rr)-100, StatUtils.max(rr)+100));
        yAxis.enableGesturePanning(true);
        yAxis.enableGestureZooming(true);

        // Clear
        List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
        for (Series<?> s: series)
            chart.removeSeries(s);

        DataAdapter<Double, Double> dataAdapter1 = new SimpleDataAdapter<Double, Double>();
        for (double i=0; i<1500; i+=50)
            dataAdapter1.add(new DataPoint<Double, Double>(i, i));
        LineSeries series1 = new LineSeries();
        series1.setDataAdapter(dataAdapter1);
        chart.addSeries(series1);

        DataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
        for (int i=1; i<rr.length; i++) {
            dataAdapter2.add(new DataPoint<Double, Double>(rr[i-1], rr[i]));
        }

        LineSeries series2 = new LineSeries();
        series2.setDataAdapter(dataAdapter2);
        series2.getStyle().setLineShown(false);
        series2.getStyle().getPointStyle().setPointsShown(true);
        series2.getStyle().getPointStyle().setInnerColor(Color.RED);
        series2.getStyle().getPointStyle().setColor(Color.BLACK);
        series2.getStyle().getPointStyle().setRadius(1.0f);
        chart.addSeries(series2);

        chart.redrawChart();
    }

    @Override
    protected int getBottomCustomLayoutId() {
        return R.layout.fragment_scatterogram_report_bottom;
    }
}
