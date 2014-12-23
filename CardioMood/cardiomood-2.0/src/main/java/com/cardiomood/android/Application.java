package com.cardiomood.android;

import android.support.multidex.MultiDexApplication;

import com.cardiomood.android.db.DatabaseHelperFactory;
import com.parse.Parse;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by Anton Danshin on 19/12/14.
 */
public class Application extends MultiDexApplication {

    private static final String PARSE_APP_ID = "SSzU4YxI6Z6SwvfNc2vkZhYQYl86CvBpd3P2wHF1";
    private static final String PARSE_CLIENT_KEY = "erjFId3717uDBia7ZOUObyaYudbIB8TYUXIW3Pnt";

    @Override @DebugLog
    public void onCreate() {
        super.onCreate();

        // initialize Timber for logging
        Timber.plant(new Timber.DebugTree());

        // initialize Parse
        Parse.initialize(this.getApplicationContext(), PARSE_APP_ID, PARSE_CLIENT_KEY);

        // initialize DB
        DatabaseHelperFactory.initialize(this);
    }

    @Override @DebugLog
    public void onTerminate() {
        super.onTerminate();
        DatabaseHelperFactory.releaseHelper();
    }
}
