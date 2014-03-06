package com.cardiomood.math;

import com.cardiomood.math.histogram.Histogram;
import com.cardiomood.math.window.DataWindow;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;

public class HeartRateMath {

    public static final double DEFAULT_WINDOW_STEP = 1000;

    private DoubleArray time = null;
    private DoubleArray rrIntervals = null;
    private double duration = 0;
    private DataWindow window;

    public HeartRateMath() {
        rrIntervals = new ResizableDoubleArray();
        time = new ResizableDoubleArray();
        setWindow(new DataWindow.Unlimited(0, DEFAULT_WINDOW_STEP));
    }

    public HeartRateMath(double[] rrIntervals) {
        this.rrIntervals = new ResizableDoubleArray();
        time = new ResizableDoubleArray();
        setWindow(new DataWindow.Unlimited(0, DEFAULT_WINDOW_STEP));

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

    public void setWindow(DataWindow window) {
        this.window = window;
    }

    public double getDuration() {
        return duration;
    }

    public DataWindow getWindow() {
        return window;
    }

    public void addIntervals(double... rrIntervals) {
        for (double rrInterval: rrIntervals) {
            this.rrIntervals.addElement(rrInterval);
            time.addElement(this.duration);
            window.add(rrInterval);
            this.duration += rrInterval;
        }
    }

    public double getMean() {
        return StatUtils.mean(rrIntervals.getElements());
    }

    public double getTotalStressIndex() {
        Histogram h = new Histogram(rrIntervals.getElements(), 50);
        return h.getSI();
    }

    public int getCount() {
        return this.rrIntervals.getNumElements();
    }
}
