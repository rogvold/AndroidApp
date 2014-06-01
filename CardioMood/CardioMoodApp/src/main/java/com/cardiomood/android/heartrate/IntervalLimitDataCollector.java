package com.cardiomood.android.heartrate;

import com.cardiomood.android.db.DatabaseHelper;

/**
 * Created by danon on 05.03.14.
 */
public class IntervalLimitDataCollector extends AbstractDataCollector {

    private static final String TAG = IntervalLimitDataCollector.class.getSimpleName();

    private int intervalsLimit;

    public IntervalLimitDataCollector(CardioMoodHeartRateLeService service, DatabaseHelper helper, int intervalsLimit) {
        super(service, helper);
        if (intervalsLimit <= 0)
            throw new IllegalArgumentException("intervalsLimit must be positive.");

        this.intervalsLimit = intervalsLimit;
    }


    @Override
    public boolean needToStopCollecting() {
        return getIntervalsCount() >= intervalsLimit;
    }


    @Override
    public double getProgress() {
        double progress = (100.0*getIntervalsCount()) / intervalsLimit;
        if (progress > 100)
            progress = 100;
        return progress;
    }
}
