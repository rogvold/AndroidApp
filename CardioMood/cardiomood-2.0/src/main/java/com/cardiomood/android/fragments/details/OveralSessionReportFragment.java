package com.cardiomood.android.fragments.details;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.controls.gauge.SpeedometerGauge;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.math.HeartRateUtils;
import com.cardiomood.math.window.DataWindow;
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


public class OveralSessionReportFragment extends AbstractSessionReportFragment {

    private static final String TAG = OveralSessionReportFragment.class.getSimpleName();

    // Components in this fragment view:
    private TextView meanHeartRate;
    private TextView meanStressIndex;
    private SpeedometerGauge speedometer;

    private double meanBPM = 0;
    private double stressIndex = 0;
    private double[] bpm = new double[0];
    private double[] time = new double[0];

    private final Object lock = new Object();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        meanHeartRate = (TextView) v.findViewById(R.id.mean_hear_rate);
        meanStressIndex = (TextView) v.findViewById(R.id.mean_stress);

        speedometer = (SpeedometerGauge) v.findViewById(R.id.stress_speedometer);
        speedometer.setMaxSpeed(300);
        speedometer.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });
        speedometer.setMaxSpeed(300);
        speedometer.setMajorTickStep(30);
        speedometer.setMinorTicks(2);
        speedometer.addColoredRange(30, 140, Color.GREEN);
        speedometer.addColoredRange(140, 180, Color.YELLOW);
        speedometer.addColoredRange(180, 400, Color.RED);

        return v;
    }

    @Override
    protected int getTopCustomLayoutId() {
        return R.layout.fragment_overal_report_top;
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
            bpm = new double[rrFiltered.length];
            this.time = time;
            for (int i = 0; i < rrFiltered.length; i++) {
                bpm[i] = 1000 * 60 / rrFiltered[i];
            }
            meanBPM = StatUtils.mean(bpm);
            stressIndex = StatUtils.mean(HeartRateUtils.getSI(rrFiltered, new DataWindow.Timed(2 * 1000 * 60, 5000))[1]);
        }
    }

    @Override
    protected void displayData(double[] rr) {
        ShinobiChart chart = getChart();
        chart.setTitle("Heart Rate");
        Axis xAxis = chart.getXAxis();
        Axis yAxis = chart.getYAxis();


        synchronized (lock) {
            meanHeartRate.setText(String.valueOf(Math.round(meanBPM)));
            meanStressIndex.setText(String.valueOf(Math.round(stressIndex)));
            speedometer.setSpeed(stressIndex, 1200, 200);

            // Heart Rate Chart
            xAxis.enableGesturePanning(true);
            xAxis.enableGestureZooming(true);
            xAxis.allowPanningOutOfDefaultRange(false);
            xAxis.setDefaultRange(new NumberRange(time[0], time[time.length - 1] / 1000));

            // Clear
            List<Series<?>> series = new ArrayList<Series<?>>(chart.getSeries());
            for (Series<?> s : series)
                chart.removeSeries(s);

            SimpleDataAdapter<Double, Double> dataAdapter1 = new SimpleDataAdapter<Double, Double>();
            for (int i = 0; i < time.length; i++)
                dataAdapter1.add(new DataPoint<Double, Double>(time[i] / 1000, bpm[i]));


            LineSeries series1 = new LineSeries();
            series1.getStyle().setLineColor(getResources().getColor(R.color.colorAccent));
            series1.setDataAdapter(dataAdapter1);
            chart.addSeries(series1);

            // Add Mean Heart Rate horizontal line
            DataAdapter<Double, Double> dataAdapter2 = new SimpleDataAdapter<Double, Double>();
            dataAdapter2.add(new DataPoint<Double, Double>(-500.0, meanBPM));
            dataAdapter2.add(new DataPoint<Double, Double>(time[time.length - 1] / 1000 + 500.0, meanBPM));

            LineSeries series2 = new LineSeries();
            series2.setDataAdapter(dataAdapter2);
            chart.addSeries(series2);
        }

        chart.redrawChart();
    }

}
