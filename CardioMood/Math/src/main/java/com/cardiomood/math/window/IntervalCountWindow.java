package com.cardiomood.math.window;

/**
 * Created by danon on 17.02.14.
 */
public class IntervalCountWindow extends AbstractIntervalsWindow {

    private int lastPosition = 0;

    public IntervalCountWindow(double windowSize, double stepSize) {
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
