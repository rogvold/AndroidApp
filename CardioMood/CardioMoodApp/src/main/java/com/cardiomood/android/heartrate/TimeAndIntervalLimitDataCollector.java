package com.cardiomood.android.heartrate;

import com.cardiomood.android.db.DatabaseHelper;

/**
 * Created by danon on 06.03.14.
 */
public class TimeAndIntervalLimitDataCollector extends TimeLimitDataCollector {

    private int intervalsLimit = 0;

    public TimeAndIntervalLimitDataCollector(CardioMoodHeartRateLeService service, DatabaseHelper helper, double timeLimit, int intervalsLimit) {
        super(service, helper, timeLimit);
        this.intervalsLimit = intervalsLimit;
    }

    @Override
    public boolean needToStopCollecting() {
        return super.needToStopCollecting() || (getIntervalsCount() >= intervalsLimit);
    }

    @Override
    public double getProgress() {
        double progress = (100.0*getIntervalsCount()) / intervalsLimit;
        progress = Math.max(progress, super.getProgress());
        if (progress > 100)
            progress = 100;
        return progress;
    }
}
