package com.cardiomood.math.window;

import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;

/**
 * Created by danon on 17.02.14.
 */
public abstract class AbstractIntervalsWindow {

    private final DoubleArray intervals;
    private final DoubleArray time;
    private double duration = 0;
    private double windowSize;
    private double stepSize;
    private double timePosition = 0;
    private int indexPosition = 0;

    private Callback callback = null;

    public AbstractIntervalsWindow(double windowSize, double stepSize) {
        this.windowSize = windowSize;
        this.stepSize = stepSize;

        intervals = new ResizableDoubleArray() {
            @Override
            public synchronized double addElementRolling(double value) {
                if (callback != null) {
                    callback.onMove(AbstractIntervalsWindow.this, duration, value);
                }

                double discarded = super.addElementRolling(value);
                time.addElementRolling(duration);
                duration += value - discarded;
                indexPosition++;
                timePosition += discarded;

                return discarded;
            }

            @Override
            public synchronized void addElement(double value) {
                if (callback != null) {
                    value = callback.onAdd(AbstractIntervalsWindow.this, duration, value);
                }
                super.addElement(value);
                time.addElement(duration);
                duration += value;
            }

        };
        time = new ResizableDoubleArray();
    }

    protected abstract boolean addIntervalAndMove(double interval);

    public final void add(double... intervals) {
        for (double interval: intervals) {
            if (addIntervalAndMove(interval) && callback != null) {
                   callback.onStep(this, duration, interval);
            }
        }
    }

    public DoubleArray getIntervals() {
        return intervals;
    }

    public double getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(double windowSize) {
        this.windowSize = windowSize;
    }

    public double getStepSize() {
        return stepSize;
    }

    public void setStepSize(double stepSize) {
        this.stepSize = stepSize;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public double getDuration() {
        return duration;
    }

    public double getTimePosition() {
        return timePosition;
    }

    public int getIndexPosition() {
        return indexPosition;
    }

    public DoubleArray getTime() {
        return time;
    }

    public interface Callback {
        /**
         * Invoked before window is moved (i.e. before calling addElementRolling()).
         * @param window Instance, representing current window instance.
         * @param t Time (x) of the interval.
         * @param value Value (y) that is going to be added to the window.
         * @param <T> Specific implementation (class) of abstract window
         */
        <T extends AbstractIntervalsWindow> void onMove(T window, double t, double value);

        /**
         * Always invoked before adding next element to the window.
         * @param window Instance, representing current window instance.
         * @param t Time (x) of the interval.
         * @param value Value (y) that is going to be added to the window.
         * @param <T> Specific implementation (class) of abstract window.
         * @return Actual value that will be added to the window.
         */
        <T extends AbstractIntervalsWindow> double onAdd(T window, double t, double value);

        <T extends AbstractIntervalsWindow> void onStep(T window, double t, double value);
    }
}
