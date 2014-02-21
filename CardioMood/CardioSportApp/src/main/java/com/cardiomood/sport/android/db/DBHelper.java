package com.cardiomood.sport.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

public class DBHelper extends SQLiteOpenHelper implements DBContract {

	private static class HOLDER {
		private static DBHelper instance;
		private static AtomicInteger counter = new AtomicInteger(0);
	}
	
	public static DBHelper getInstance(Context ctx) {
		if (HOLDER.instance == null) {
			HOLDER.instance = new DBHelper(ctx);
		}
		return HOLDER.instance;
	}
	
	private DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public synchronized SQLiteDatabase getWritableDatabase() {
		HOLDER.counter.incrementAndGet();
		return super.getWritableDatabase();
	}

    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        HOLDER.counter.incrementAndGet();
        return super.getReadableDatabase();
    }
	
	@Override
	public synchronized void close() {
		int val = HOLDER.counter.decrementAndGet();
		if (val <= 0) {
			super.close();
			HOLDER.counter = new AtomicInteger(0);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create database entities                                 
		try {
			db.execSQL(SQL.CREATE_TABLE_WORKOUTS);
			db.execSQL(SQL.CREATE_TABLE_HR_DATA);
            db.execSQL(SQL.CREATE_TABLE_GPS_INFO);
            db.execSQL(SQL.CREATE_TABLE_ACTIVITY_INFO_DATA);
		} catch (Exception e) {
			// tables already exist
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Upgrade or downgrade the database schema
		db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + HeartRateData.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + GPSInfo.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ActivityInfoData.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Workouts.TABLE_NAME);
            onCreate(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

}
