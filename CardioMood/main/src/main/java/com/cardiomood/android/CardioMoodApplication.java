package com.cardiomood.android;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.cardiomood.android.db.HeartRateDBHelper;

/**
 * Created by danshin on 31.10.13.
 */
public class CardioMoodApplication extends Application {
    private static SQLiteDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = new HeartRateDBHelper(this).getWritableDatabase();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
    }

    /**
     * @return an open database.
     */
    public static SQLiteDatabase getOpenDatabase() {
        return database;
    }
}
