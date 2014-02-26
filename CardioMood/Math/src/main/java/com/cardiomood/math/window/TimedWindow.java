package com.cardiomood.math.window;

/**
 * Created by danon on 17.02.14.
 */
public class TimedWindow extends AbstractIntervalsWindow {

    private double lastPosition = 0;

    public TimedWindow(double windowSize, double stepSize) {
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
