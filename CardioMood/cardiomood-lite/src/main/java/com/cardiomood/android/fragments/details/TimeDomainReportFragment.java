package com.cardiomood.android.fragments.details;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.lite.R;
import com.cardiomood.math.filter.ArtifactFilter;
import com.cardiomood.math.filter.PisarukArtifactFilter;
import com.cardiomood.math.parameters.PNN50Value;
import com.cardiomood.math.parameters.RMSSDValue;
import com.cardiomood.math.parameters.SDNNValue;
import com.shinobicontrols.charts.Axis;
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

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

/**
 * Created by Anton Danshin on 27/12/14.
 */
public class TimeDomainReportFragment extends AbstractSessionReportFragment {

    private final ArtifactFilter FILTER = new PisarukArtifactFilter();

    private double rr[] = null;
    private double time[] = null;

    private double meanHR;
    private double mRR;
    private int artifactsCount;
    private double SDNN;
    private double RMSSD;
    private double pNN50;

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
    protected void collectDataInBackground(SessionEntity session, double[] t, double[] rrFiltered) {
        synchronized (lock) {
            this.rr = rrFiltered.clone();
            this.time = t.clone();
        }
        mRR = StatUtils.mean(rr);
        meanHR = 60 * 1000.0d / mRR;
        artifactsCount = FILTER.getArtifactsCount(rr);
        SDNN = new SDNNValue().evaluate(time, rr);
        RMSSD = new RMSSDValue().evaluate(time, rr);
        pNN50 = new PNN50Value().evaluate(time, rr);
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
            artifactsView.setText(String.valueOf(artifactsCount));
            meanHRView.setText(String.valueOf(Math.round(meanHR*10)/10.0d) + " bpm");
            mRRView.setText(String.valueOf(Math.round(mRR*10)/10.0d) + " ms");
            sdnnView.setText(String.valueOf(Math.round(SDNN*100)/100.0d) + " ms");
            rmssdView.setText(String.valueOf(Math.round(RMSSD*100)/100.0d) + " ms");
            pnn50iew.setText(String.valueOf(Math.round(pNN50*100)/100.0d) + "%");

            descriptionView.setText(getHRVStatus(SDNN));
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

