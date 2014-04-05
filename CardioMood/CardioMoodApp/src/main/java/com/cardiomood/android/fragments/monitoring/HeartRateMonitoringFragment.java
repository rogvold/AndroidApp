package com.cardiomood.android.fragments.monitoring;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.progress.BatteryIndicatorGauge;
import com.cardiomood.android.progress.CircularProgressBar;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.math.HeartRateMath;
import com.cardiomood.math.window.DataWindow;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Created by danon on 04.03.14.
 */
public class HeartRateMonitoringFragment extends Fragment implements FragmentCallback {

    private ActivityCallback mCallback = null;

    private int currentBPM = -1;
    private double currentProgress = 0;

    private CircularProgressBar progressBar;
    private TextView intervalsCollected;
    private TextView timeElapsed;
    private BatteryIndicatorGauge batteryIndicator;

    private HeartRateMath hrm;

    private Handler handler;


    public static HeartRateMonitoringFragment newInstance() {
        return new HeartRateMonitoringFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();

        hrm = new HeartRateMath();
        hrm.setWindow(new DataWindow.IntervalsCount(30, 5));
        hrm.getWindow().setCallback(new DataWindow.Callback() {
            @Override
            public <T extends DataWindow> void onMove(T window, double t, double value) {
            }

            @Override
            public <T extends DataWindow> double onAdd(T window, double t, double value) {
                return value;
            }

            @Override
            public <T extends DataWindow> void onStep(T window, double t, double value) {
                double[] rr = window.getIntervals().getElements();
                final double d = Math.sqrt(StatUtils.variance(rr)) / StatUtils.mean(rr) * 100;
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (d > batteryIndicator.getMax())
                            batteryIndicator.setValue(batteryIndicator.getMax(), 1000, 0);
                        else
                            batteryIndicator.setValue((float) d, 1000, 0);
                    }
                });
            }
        });
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
        mCallback.unregisterFragmentCallback(this);
        mCallback = null;
    }

    @Override
    public void notifyBPM(int bpm) {
        currentBPM = bpm;
    }

    @Override
    public void notifyRRIntervals(short[] rr) {
        for (short r: rr) {
            hrm.addIntervals((double) r);
        }
    }

    @Override
    public void notifyConnectionStatus(int oldStatus, int newStatus) {

    }

    @Override
    public void notifyProgress(double progress, int count, long duration) {
        currentProgress = progress;
        progressBar.setProgress((float) progress, 300);
        intervalsCollected.setText(String.valueOf(count));
        timeElapsed.setText(CommonTools.timeToHumanString(duration));
    }
}
