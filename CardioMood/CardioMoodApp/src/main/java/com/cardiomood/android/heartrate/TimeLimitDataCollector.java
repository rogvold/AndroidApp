package com.cardiomood.android.heartrate;

/**
 * Created by danon on 05.03.14.
 */
public class TimeLimitDataCollector extends AbstractDataCollector {

    private static final String TAG = TimeLimitDataCollector.class.getSimpleName();

    private final double timeLimit;

    public TimeLimitDataCollector(CardioMoodHeartRateLeService service, double timeLimit) {
        super(service);

        if (timeLimit <= 0)
            throw new IllegalArgumentException("Time limit must be positive.");
        this.timeLimit = timeLimit;
    }

    @Override
    public boolean needToStopCollecting() {
        return getDuration() > timeLimit;
    }

    @Override
    public double getProgress() {
        double progress = 100 * math.getDuration() / timeLimit;
        if (progress > 100)
            progress = 100;
        return progress;
    }

    @Override
    public void addData(int bpm, short[] rrIntervals) {
        super.addData(bpm, rrIntervals);
        // todo: save it to database ?
    }

    public double getTimeLimit() {
        return timeLimit;
    }
}
