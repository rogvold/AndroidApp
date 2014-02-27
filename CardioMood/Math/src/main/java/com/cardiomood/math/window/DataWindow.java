package com.cardiomood.math.window;

import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;

/**
 * Created by danon on 17.02.14.
 */
public abstract class DataWindow {

    private final DoubleArray intervals;
    private final DoubleArray time;
    private double duration = 0;
    private double windowSize;
    private double stepSize;
    private double timePosition = 0;
    private int indexPosition = 0;

    private Callback callback = null;

    protected DataWindow(double windowSize, double stepSize) {
        this.windowSize = windowSize;
        this.stepSize = stepSize;

        intervals = new ResizableDoubleArray() {
            @Override
            public synchronized double addElementRolling(double value) {
                if (callback != null) {
                    callback.onMove(DataWindow.this, duration, value);
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
                    value = callback.onAdd(DataWindow.this, duration, value);
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
        <T extends DataWindow> void onMove(T window, double t, double value);

        /**
         * Always invoked before adding next element to the window.
         * @param window Instance, representing current window instance.
         * @param t Time (x) of the interval.
         * @param value Value (y) that is going to be added to the window.
         * @param <T> Specific implementation (class) of abstract window.
         * @return Actual value that will be added to the window.
         */
        <T extends DataWindow> double onAdd(T window, double t, double value);

        <T extends DataWindow> void onStep(T window, double t, double value);
    }

    public static class Timed extends DataWindow {

        private double lastPosition = 0;

        public Timed(double windowSize, double stepSize) {
            super(windowSize, stepSize);
        }

        @Override
        protected boolean addIntervalAndMove(double interval) {
            // add element
            double duration = getDuration();
            double windowSize = getWindowSize();

            System.out.println("TimedWindow.addIntervalAndMove(): duration=" + duration + ", windowSize="+windowSize +", interval=" + interval);

            if (duration + interval > windowSize) {
                // callback.onMove() will be invoked
                getIntervals().addElementRolling(interval);
                System.out.println("TimedWindow.addIntervalAndMove(): window moved");

            } else {
                getIntervals().addElement(interval);
            }

            // notify about next step if necessary
            double stepSize = getStepSize();
            double position = getTimePosition();
            System.out.println("TimedWindow.addIntervalAndMove(): lastPosition=" + lastPosition + ", stepSize="+stepSize +", position=" + position);

            if (position - lastPosition >= stepSize) {
                // callback.onStep() will be invoked
                lastPosition = position;
                return true;
            }

            // onStep() will not be invoked
            return false;
        }
    }

    public static class Unlimited extends DataWindow {

        private double lastDuration = 0;

        public Unlimited(double windowSize, double stepSize) {
            super(windowSize, stepSize);
        }

        @Override
        protected boolean addIntervalAndMove(double interval) {
            // add element and update window size
            getIntervals().addElement(interval);
            setWindowSize(getDuration());

            // notify about next step if necessary
            double duration = getDuration();
            double stepSize = getStepSize();
            if (duration - lastDuration < stepSize) {
                // onStep() will not be invoked!
                return false;
            }

            // next window step
            lastDuration = duration;
            // onStep() will be invoked
            return true;
        }
    }

    public class IntervalsCount extends DataWindow {

        private int lastPosition = 0;

        public IntervalsCount(double windowSize, double stepSize) {
            super(windowSize, stepSize);
        }

        @Override
        protected boolean addIntervalAndMove(double interval) {
            // add element
            int count = getIntervals().getNumElements();
            double windowSize = getWindowSize();

            if (count + 1 > windowSize) {
                // callback.onMove() will be invoked
                getIntervals().addElementRolling(interval);
            } else {
                getIntervals().addElement(interval);
            }

            // notify about next step if necessary
            double stepSize = getStepSize();
            int position = getIndexPosition();
            if (position - lastPosition >= stepSize) {
                // callback.onStep() will be invoked
                lastPosition = position;
                return true;
            }

            // onStep() will not be invoked
            return false;
        }
    }

}
