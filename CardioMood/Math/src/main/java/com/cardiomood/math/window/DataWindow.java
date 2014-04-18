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

        intervals = new LocalResizableDoubleArray();
        time = new ResizableDoubleArray();
    }

    protected abstract boolean addIntervalAndMove(double interval);

    public final void add(double... intervals) {
        for (double interval: intervals) {
            if (addIntervalAndMove(interval) && callback != null) {
                   callback.onStep(this, indexPosition + getCount(), duration+timePosition, interval);
            }
        }
    }

    public void clear() {
        timePosition = 0;
        indexPosition = 0;
        intervals.clear();
        time.clear();
        duration = 0;
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

    public int getCount() {
        return getIntervals().getNumElements();
    }

    public interface Callback<T extends DataWindow> {
        /**
         * Invoked before window is moved (i.e. before calling addElementRolling()).
         * @param window Instance, representing current window instance.
         * @param t Time (x) of the interval.
         * @param value Value (y) that is going to be added to the window.
         */
        void onMove(T window, double t, double value);

        /**
         * Always invoked before adding next element to the window.
         * @param window Instance, representing current window instance.
         * @param t Time (x) of the interval.
         * @param value Value (y) that is going to be added to the window.
         * @return Actual value that will be added to the window.
         */
        double onAdd(T window, double t, double value);

        void onStep(T window, int index, double t, double value);
    }

    public static class CallbackAdapter<T  extends DataWindow> implements Callback<T> {

        @Override
        public void onMove(T window, double t, double value) {

        }

        @Override
        public double onAdd(T window, double t, double value) {
            return value;
        }

        @Override
        public void onStep(T window, int index, double t, double value) {

        }
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
            boolean flag = getIndexPosition() == 0;

            if (duration + interval > windowSize) {
                // callback.onMove() will be invoked
                getIntervals().addElementRolling(interval);
            } else {
                getIntervals().addElement(interval);
            }

            if (flag && getIndexPosition() == 1) {
                // position was 0 and now is 1
                return true;
            }

            // notify about next step if necessary
            double stepSize = getStepSize();
            double position = getTimePosition();

            if (position - lastPosition >= stepSize) {
                // callback.onStep() will be invoked
                lastPosition = (Math.round(position) / ((long) stepSize)) * (long) stepSize;
                return true;
            }

            // onStep() will not be invoked
            return false;
        }
    }

    public static class UnlimitedWithTimeStep extends DataWindow {

        private double lastDuration = 0;

        public UnlimitedWithTimeStep(double stepSize) {
            super(0, stepSize);
        }

        @Override
        protected boolean addIntervalAndMove(double interval) {
            // add element and update window size
            getIntervals().addElement(interval);
            double duration = getDuration();
            setWindowSize(duration);

            // notify about next step if necessary
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

    public static class UnlimitedWithCountStep extends DataWindow {

        private int lastCount = 0;

        public UnlimitedWithCountStep(double stepSize) {
            super(0, stepSize);
        }

        @Override
        protected boolean addIntervalAndMove(double interval) {
            // add element and update window size
            getIntervals().addElement(interval);
            int count = getCount();
            setWindowSize(getIntervals().getNumElements());

            // notify about next step if necessary

            double stepSize = getStepSize();
            if (count - lastCount < stepSize) {
                // onStep() will not be invoked!
                return false;
            }

            // next window step
            lastCount = count;
            // onStep() will be invoked
            return true;
        }
    }

    public static class IntervalsCount extends DataWindow {

        private int lastPosition = 0;

        public IntervalsCount(double windowSize, double stepSize) {
            super(windowSize, stepSize);
        }

        @Override
        protected boolean addIntervalAndMove(double interval) {
            // add element
            int count = getCount();
            double windowSize = getWindowSize();
            if (count + 1 > windowSize) {
                // callback.onMove() will be invoked
                getIntervals().addElementRolling(interval);
            } else {
                getIntervals().addElement(interval);
                if (getCount() == (int) Math.round(getWindowSize())) {
                    return true;
                }
            }

            // notify about next step if necessary
            int stepSize = (int) Math.round(getStepSize());
            int position = getIndexPosition();
            if (position - lastPosition >= stepSize) {
                // callback.onStep() will be invoked
                lastPosition = (position / stepSize) * stepSize;
                return true;
            }

            // onStep() will not be invoked
            return false;
        }
    }

    private class LocalResizableDoubleArray extends ResizableDoubleArray {
        @Override
        public synchronized double addElementRolling(double value) {
            if (callback != null) {
                callback.onMove(DataWindow.this, duration + timePosition, value);
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
                value = callback.onAdd(DataWindow.this, duration + timePosition, value);
            }
            super.addElement(value);
            time.addElement(duration);
            duration += value;
        }
    }
}
