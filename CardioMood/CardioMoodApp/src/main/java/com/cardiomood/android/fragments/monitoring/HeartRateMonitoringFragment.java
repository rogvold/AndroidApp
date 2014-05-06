package com.cardiomood.android.fragments.monitoring;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.controls.gauge.BatteryIndicatorGauge;
import com.cardiomood.android.controls.gauge.SpeedometerGauge;
import com.cardiomood.android.controls.progress.CircularProgressBar;
import com.cardiomood.android.heartrate.AbstractDataCollector;
import com.cardiomood.android.heartrate.CardioMoodHeartRateLeService;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.math.filter.ArtifactFilter;
import com.cardiomood.math.filter.PisarukArtifactFilter;
import com.cardiomood.math.histogram.Histogram;
import com.cardiomood.math.window.DataWindow;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Created by danon on 04.03.14.
 */
public class HeartRateMonitoringFragment extends Fragment implements FragmentCallback {

    private ActivityCallback mCallback = null;

    private int currentBPM = -1;

    private CircularProgressBar progressBar;
    private TextView intervalsCollected;
    private TextView timeElapsed;
    private BatteryIndicatorGauge batteryIndicator;
    private SpeedometerGauge stressIndicator;
    private TextView stressIndexValue;
    private TextView energyLevel;

    private Handler handler;

    private double lastStressIndex = -1;
    private double lastEnergyLevel = -1;
    private double artifactsPercentage = 0.0;

    private static final DataWindow.IntervalsCount batteryWindow = new DataWindow.IntervalsCount(20, 5);
    private static final DataWindow.Timed stressWindow = new DataWindow.Timed(2 * 60 * 1000, 5000);

    private final DataWindow.Callback batteryWindowCallback = new DataWindow.CallbackAdapter<DataWindow.IntervalsCount>() {

        @Override
        public void onStep(final DataWindow.IntervalsCount window, int index, double t, double value) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    double[] rr = window.getIntervals().getElements();
                    artifactsPercentage = (filter.getArtifactsCount(rr) * 100.0) / rr.length;
                    rr = filter.doFilter(rr);
                    double d = Math.sqrt(StatUtils.variance(rr));
                    if (d <= 0) {
                        d = 0.0;
                    } else if (d < 15){
                        d *= 6.0;
                    } else {
                        d = (100 - 50.0 / (16.6171 * Math.log(d) - 40));
                    }
                    lastEnergyLevel = d;
                    if (batteryIndicator != null) {
                        if (d >= batteryIndicator.getMax() - 0.001)
                            batteryIndicator.setValue(batteryIndicator.getMax(), 4000, 0);
                        else
                            batteryIndicator.setValue((float) d, 4000, 0);
                        if (energyLevel != null) {
                            energyLevel.setText(String.valueOf(Math.round(d)) + "%");
                        }
                    }
                }
            });
        }
    };
    private final DataWindow.Callback stressWindowCallback = new DataWindow.CallbackAdapter<DataWindow.Timed>() {
        @Override
        public void onStep(final DataWindow.Timed window, int index, double t, double value) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    double[] rr = window.getIntervals().getElements();
                    rr = filter.doFilter(rr);
                    Histogram histogram = new Histogram(rr, 50);
                    final double SI = histogram.getSI();
                    lastStressIndex = SI;
                    if (stressIndicator != null) {
                        stressIndicator.setSpeed(SI, 4000, 0);
                    }
                    if (stressIndexValue != null) {
                        stressIndexValue.setText(String.valueOf(Math.round(SI)));
                    }
                }
            });
        }
    };

    private static final ArtifactFilter filter = new PisarukArtifactFilter();


    public static HeartRateMonitoringFragment newInstance() {
        return new HeartRateMonitoringFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_heart_rate_monitoring, container, false);

        progressBar = (CircularProgressBar) v.findViewById(R.id.measurement_progress);
        progressBar.setLabelConverter(new CircularProgressBar.LabelConverter() {
            @Override
            public String getLabelFor(float progress, float max, Paint paint) {
                if (currentBPM < 0)
                    return "N/A";
                return currentBPM + " bpm";
            }
        });

        intervalsCollected = (TextView) v.findViewById(R.id.intervalsCollected);
        timeElapsed = (TextView) v.findViewById(R.id.timeElapsed);
        batteryIndicator = (BatteryIndicatorGauge) v.findViewById(R.id.battery);
        stressIndicator = (SpeedometerGauge) v.findViewById(R.id.stressIndicator);
        stressIndicator.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });
        stressIndicator.setMaxSpeed(300);
        stressIndicator.setMajorTickStep(30);
        stressIndicator.setMinorTicks(2);
        stressIndicator.addColoredRange(30, 140, Color.GREEN);
        stressIndicator.addColoredRange(140, 180, Color.YELLOW);
        stressIndicator.addColoredRange(180, 400, Color.RED);

        stressIndexValue = (TextView) v.findViewById(R.id.stressIndexValue);
        if (lastStressIndex > 0) {
            stressIndexValue.setText(String.valueOf(Math.round(lastStressIndex)));
        }
        energyLevel = (TextView) v.findViewById(R.id.energy_level);
        if (lastEnergyLevel > 0) {
            energyLevel.setText(String.valueOf(Math.round(lastEnergyLevel)) + "%");
        }

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof ActivityCallback) {
            mCallback = (ActivityCallback) activity;
            mCallback.registerFragmentCallback(this);
        } else {
            throw new ClassCastException("Host activity must implement " + ActivityCallback.class.getName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (mCallback != null) {
            mCallback.unregisterFragmentCallback(this);
            mCallback = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        batteryWindow.setCallback(batteryWindowCallback);
        stressWindow.setCallback(stressWindowCallback);
    }

    @Override
    public void onStop() {
        super.onStop();

        stressWindow.setCallback(null);
        batteryWindow.setCallback(null);
    }

    @Override
    public void notifyBPM(CardioMoodHeartRateLeService service, int bpm) {
        currentBPM = bpm;
    }


    @Override
    public void notifyConnectionStatus(CardioMoodHeartRateLeService service, int oldStatus, int newStatus) {

    }

    @Override
    public void notifyProgress(CardioMoodHeartRateLeService service, double progress, int count, long duration) {
        AbstractDataCollector collector = (AbstractDataCollector) service.getDataCollector();
        collector.addWindow(batteryWindow);
        collector.addWindow(stressWindow);


        progressBar.setProgress((float) progress, 300);
        intervalsCollected.setText(String.valueOf(count));
        timeElapsed.setText(CommonTools.timeToHumanString(duration));
    }
}
