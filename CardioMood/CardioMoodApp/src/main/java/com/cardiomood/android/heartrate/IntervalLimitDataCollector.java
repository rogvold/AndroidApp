package com.cardiomood.android.heartrate;

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
        if (needToStopCollecting()) {
            // enough data were collected
        } else {
            // remove session
        }
    }

    @Override
    public double getProgress() {
        double progress = (100.0*math.getCount()) / intervalsLimit;
        if (progress > 100)
            progress = 100;
        return progress;
    }
}
