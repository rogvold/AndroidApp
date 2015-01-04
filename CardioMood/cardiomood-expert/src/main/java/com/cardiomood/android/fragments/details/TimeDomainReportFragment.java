package com.cardiomood.android.fragments.details;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.expert.R;
import com.cardiomood.math.parameters.SDANN10sValue;
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

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

/**
 * Created by Anton Danshin on 27/12/14.
 */
public class TimeDomainReportFragment extends AbstractSessionReportFragment {

    private double rr[] = null;
    private double time[] = null;
    private TextReport report = null;
    private double SDANN10s = 0;

    private final Object lock = new Object();

    @InjectView(R.id.rrCount) @Optional
    TextView rrCountView;
    @InjectView(R.id.artifacts) @Optional
    TextView artifactsView;
    @InjectView(R.id.meanHR) @Optional
    TextView meanHRView;
    @InjectView(R.id.mRR) @Optional
    TextView mRRView;
    @InjectView(R.id.SDNN) @Optional
    TextView sdnnView;
    @InjectView(R.id.RMSSD) @Optional
    TextView rmssdView;
    @InjectView(R.id.pNN50) @Optional
    TextView pnn50iew;
    @InjectView(R.id.time_domain_description) @Optional
    TextView descriptionView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.inject(this, v);
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
            this.rr = rrFiltered.clone();
            this.time = time.clone();

            report = new TextReport.Builder().setRRIntervals(this.rr)
                    .setStartDate(new Date(session.getStartTimestamp())).build();
            SDANN10s = new SDANN10sValue().evaluate(time, rrFiltered);
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

            rrCountView.setText(String.valueOf(rr.length));
            artifactsView.setText(String.valueOf(report.getArtifactsCount()));
            meanHRView.setText(String.valueOf(Math.round(report.getHeartRate()*10)/10.0d) + " bpm");
            mRRView.setText(String.valueOf(Math.round(report.getmRR()*10)/10.0d) + " ms");
            sdnnView.setText(String.valueOf(Math.round(report.getSDNN()*100)/100.0d) + " ms");
            rmssdView.setText(String.valueOf(Math.round(report.getRMSSD()*100)/100.0d) + " ms");
            pnn50iew.setText(String.valueOf(Math.round(report.getpNN50()*100)/100.0d) + "%");

            descriptionView.setText(getHRVStatus(report.getSDNN()));
            descriptionView.setVisibility(View.GONE);
        }

        chart.redrawChart();
    }

    private String getHRVStatus(double SDNN) {
        double SDNN1 = 10;
        double SDNN2 = 60;
        if (SDNN < SDNN1) {
            return "Low heart rate variability.";
        } else if (SDNN > SDNN2) {
            return "High heart rate variability.";
        } else {
            return "Normal heart rate variability.";
        }
    }

    @Override
    protected int getBottomCustomLayoutId() {
        return R.layout.fragment_time_domain_report_bottom;
    }
}

//        SDNN1:=10;
//        SDNN2:=60;
//        HF1:=300;
//        HF2:=1000;
//        LF1:=300;
//        LF2:=2000;
//        LF_HF1:=1;
//        LF_HF2:=3;
//
//
//        Res11:='Вариабельность ритма сердца снижена';
//        Res12:='Вариабельность ритма сердца в норме';
//        Res13:='Вариабельность ритма сердца увеличена';
//
//        Res21:='Парасимпатическая активность снижена';
//        Res22:='Парасимпатическая активность в норме';
//        Res23:='Парасимпатическая активность увеличена';
//
//        Res31:='Симпатическая активность снижена';
//        Res32:='Симпатическая активность в норме';
//        Res33:='Симпатическая активность увеличена';
//
//        Res41:='Барорефлекторная активность снижена';
//        Res42:='Барорефлекторная активность в норме';
//        Res43:='Барорефлекторная активность увеличена';
//
//        if SDNN<SDNN1 then Resume1:=Res11;
//        if (SDNN>=SDNN1) and (SDNN<=SDNN2) then Resume1:=Res12;
//        if SDNN>SDNN2 then Resume1:=Res13;
//
//        if HF<HF1 then Resume2:=Res21;
//        if (HF>=HF1) and (HF<=HF2) then Resume2:=Res22;
//        if HF>HF2 then Resume2:=Res23;
//
//        if LF_HF<LF_HF1 then Resume3:=Res31;
//        if (LF_HF>=LF_HF1) and (LF_HF<=LF_HF2) then Resume3:=Res32;
//        if LF_HF>LF_HF2 then Resume3:=Res33;
//
//        if LF<LF1 then Resume4:=Res41;
//        if (LF>=LF1) and (LF<=LF2) then Resume4:=Res42;
//        if LF>LF2 then Resume4:=Res43;

