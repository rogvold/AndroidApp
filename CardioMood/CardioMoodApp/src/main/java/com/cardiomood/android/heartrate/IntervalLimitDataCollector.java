package com.cardiomood.android.heartrate;

import android.util.Log;

/**
 * Created by danon on 05.03.14.
 */
public class IntervalLimitDataCollector extends AbstractDataCollector {

    private static final String TAG = IntervalLimitDataCollector.class.getSimpleName();

    private int intervalsLimit;

    public IntervalLimitDataCollector(CardioMoodHeartRateLeService service, int intervalsLimit) {
        super(service);
        if (intervalsLimit <= 0)
            throw new IllegalArgumentException("intervalsLimit must be positive.");

        this.intervalsLimit = intervalsLimit;
    }


    @Override
    public boolean needToStopCollecting() {
        return math.getCount() >= intervalsLimit;
    }

    @Override
    public void onCompleteCollecting() {
        super.onCompleteCollecting();

        // save data :)
        StringBuilder sb = new StringBuilder("rrIntervals:");
        double rr[] = math.getRrIntervals();
        for (double r: rr)
            sb.append(" ").append(r);
        Log.d(TAG, sb.toString());
    }

    @Override
    public double getProgress() {
        return (100.0*math.getCount()) / intervalsLimit;
    }
}
