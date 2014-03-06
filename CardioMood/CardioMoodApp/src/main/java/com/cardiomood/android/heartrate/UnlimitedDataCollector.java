package com.cardiomood.android.heartrate;

/**
 * Created by danon on 05.03.14.
 */
public class UnlimitedDataCollector extends AbstractDataCollector {

    private static final String TAG = UnlimitedDataCollector.class.getSimpleName();

    public UnlimitedDataCollector(CardioMoodHeartRateLeService service) {
        super(service);
    }

    @Override
    protected boolean needToStopCollecting() {
        return false;
    }

    @Override
    public double getProgress() {
        return 0;
    }

}
