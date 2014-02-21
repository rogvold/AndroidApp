package com.cardiomood.math.window;

/**
 * Created by danon on 17.02.14.
 */
public class UnlimitedWindow extends AbstractIntervalsWindow {

    private double lastDuration = 0;

    public UnlimitedWindow(double windowSize, double stepSize) {
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
