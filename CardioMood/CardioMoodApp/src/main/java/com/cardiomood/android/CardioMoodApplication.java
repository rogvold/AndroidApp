package com.cardiomood.android;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;

import com.cardiomood.android.db.DatabaseHelper;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.parse.Parse;
import com.parse.PushService;

/**
 * Created by danshin on 31.10.13.
 */
public class CardioMoodApplication extends Application {

    private static final String PARSE_APP_ID = "D00uihFFqj0K9yAoecTzR5t4VxJeSGfYOee4LciN";
    private static final String PARSE_CLIENT_KEY = "vaqte7MiPMce9h4HFCnmTnkieIOarA9WPoCcxVnk";

    private DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        getTracker();
        database = getHelper().getWritableDatabase();

        try {
            String country = getResources().getConfiguration().locale.getCountry();
            String locale = getResources().getConfiguration().locale.toString();
            String language = getResources().getConfiguration().locale.getLanguage();

            Parse.initialize(this, PARSE_APP_ID, PARSE_CLIENT_KEY);
            PushService.setDefaultPushCallback(this, LoginActivity.class);
            // 1. Set up channels
            PushService.subscribe(this, "type-beta", LoginActivity.class);
            PushService.subscribe(this, "country-" + country, LoginActivity.class);
            PushService.subscribe(this, "locale-" + locale, LoginActivity.class);
            PushService.subscribe(this, "lang-" + language, LoginActivity.class);
            PushService.subscribe(this, "os-android", LoginActivity.class);
            PushService.subscribe(this, "sdk-" + Build.VERSION.SDK_INT, LoginActivity.class);
        } catch (Exception ex) {
            Log.e("CardioMoodApplication", "onCreate(): failed to initialize parse");
        }
    }

    public DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }

        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
    }

    public synchronized Tracker getTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    private Tracker mTracker = null;
}
