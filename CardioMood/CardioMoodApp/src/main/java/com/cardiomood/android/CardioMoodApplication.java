package com.cardiomood.android;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.cardiomood.android.db.HeartRateDBHelper;
import com.parse.Parse;

/**
 * Created by danshin on 31.10.13.
 */
public class CardioMoodApplication extends Application {
    private static SQLiteDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = new HeartRateDBHelper(this).getWritableDatabase();
        Parse.initialize(this, "D00uihFFqj0K9yAoecTzR5t4VxJeSGfYOee4LciN", "vaqte7MiPMce9h4HFCnmTnkieIOarA9WPoCcxVnk");
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
