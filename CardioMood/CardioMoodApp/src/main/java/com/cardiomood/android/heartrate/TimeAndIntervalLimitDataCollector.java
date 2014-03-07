package com.cardiomood.android.heartrate;

/**
 * Created by danon on 06.03.14.
 */
public class TimeAndIntervalLimitDataCollector extends TimeLimitDataCollector {

    private int intervalsLimit = 0;

    public TimeAndIntervalLimitDataCollector(CardioMoodHeartRateLeService service, double timeLimit, int intervalsLimit) {
        super(service, timeLimit);
        this.intervalsLimit = intervalsLimit;
    }

    @Override
    public boolean needToStopCollecting() {
        return super.needToStopCollecting() || (math.getCount() >= intervalsLimit);
    }

    @Override
    public double getProgress() {
        double progress = (100.0*math.getCount()) / intervalsLimit;
        progress = Math.max(progress, super.getProgress());
        if (progress > 100)
            progress = 100;
        return progress;
    }
}
