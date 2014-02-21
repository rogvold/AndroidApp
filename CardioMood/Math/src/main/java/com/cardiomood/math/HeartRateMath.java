package com.cardiomood.math;

import com.cardiomood.math.window.AbstractIntervalsWindow;
import com.cardiomood.math.window.UnlimitedWindow;

import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;

public class HeartRateMath {

    public static final double DEFAULT_WINDOW_STEP = 1000;

    private DoubleArray time = null;
    private DoubleArray rrIntervals = null;
    private double duration = 0;
    private AbstractIntervalsWindow window;
    private WindowCallback windowCallback = null;

    public HeartRateMath() {
        rrIntervals = new ResizableDoubleArray();
        time = new ResizableDoubleArray();
        window = new UnlimitedWindow(0, DEFAULT_WINDOW_STEP);

        window.setCallback(new AbstractIntervalsWindow.Callback() {
            @Override
            public <T extends AbstractIntervalsWindow> void onMove(T window, double t, double value) {
                if (windowCallback != null)
                    windowCallback.onMove(window, t, value);
            }

            @Override
            public <T extends AbstractIntervalsWindow> double onAdd(T window, double t, double value) {
                if (windowCallback != null)
                    return windowCallback.onAdd(window, t, value);
                return value;
            }

            @Override
            public <T extends AbstractIntervalsWindow> void onStep(T window, double t, double value) {
                if (windowCallback != null)
                    windowCallback.onStep(window, t, value);
            }
        });
    }

    public HeartRateMath(double[] rrIntervals) {
        this();
        this.rrIntervals.addElements(rrIntervals);

        this.duration = 0;
        for (double rrInterval: rrIntervals) {
            time.addElement(this.duration);
            this.duration += rrInterval;
        }
        window.add(rrIntervals);
    }

    public double[] getRrIntervals() {
        return rrIntervals.getElements();
    }

    public double[] getTime() {
        return time.getElements();
    }

    public void setWindow(AbstractIntervalsWindow window) {
        this.window = window;
    }

    public void setWindowCallback(WindowCallback windowCallback) {
        this.windowCallback = windowCallback;
    }

    public double getDuration() {
        return duration;
    }

    public AbstractIntervalsWindow getWindow() {
        return window;
    }

    public void addIntervals(double... rrIntervals) {
        for (double rrInterval: rrIntervals) {
            time.addElement(this.duration);
            this.duration += rrInterval;
        }
    }

    public static interface WindowCallback {

        public <T extends AbstractIntervalsWindow> void onMove(T window, double t, double value);

        public <T extends AbstractIntervalsWindow> double onAdd(T window, double t, double value);

        public <T extends AbstractIntervalsWindow> void onStep(T window, double t, double value);

    }

}
