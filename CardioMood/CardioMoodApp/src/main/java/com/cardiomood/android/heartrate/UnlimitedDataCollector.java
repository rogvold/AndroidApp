package com.cardiomood.android.heartrate;

import com.cardiomood.android.db.DatabaseHelper;

/**
 * Created by danon on 05.03.14.
 */
public class UnlimitedDataCollector extends AbstractDataCollector {

    private static final String TAG = UnlimitedDataCollector.class.getSimpleName();

    public UnlimitedDataCollector(CardioMoodHeartRateLeService service, DatabaseHelper helper) {
        super(service, helper);
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
