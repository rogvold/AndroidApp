package com.cardiomood.heartrate.android;

import com.cardiomood.heartrate.android.db.DatabaseHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager;

/**
 * Created by danon on 19.04.2014.
 */
public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        OpenHelperManager.getHelper(this, DatabaseHelper.class).getWritableDatabase();
    }
}
