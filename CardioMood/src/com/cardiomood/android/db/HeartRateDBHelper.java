package com.cardiomood.android.db;

import java.util.concurrent.atomic.AtomicInteger;

import com.cardiomood.android.db.model.HeartRateSession;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HeartRateDBHelper extends SQLiteOpenHelper implements HeartRateDBContract {

	private static class HOLDER {
		private static HeartRateDBHelper instance;
		private static AtomicInteger counter = new AtomicInteger(0);
	}
	
	public static HeartRateDBHelper getInstance(Context ctx) {
		if (HOLDER.instance == null) {
			HOLDER.instance = new HeartRateDBHelper(ctx);
		}
		return HOLDER.instance;
	}
	
	private HeartRateDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public synchronized SQLiteDatabase getWritableDatabase() {
		HOLDER.counter.incrementAndGet();
		return super.getWritableDatabase();
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
			db.execSQL(SQL.CREATE_TABLE_SESSIONS);
			db.execSQL(SQL.CREATE_TABLE_HR_DATA);
		} catch (Exception e) {
			// tables already exist
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Upgrade or downgrade the database schema
//		db.execSQL("DROP TABLE IF EXISTS " + HeartRateData.TABLE_NAME);
//		db.execSQL("DROP TABLE IF EXISTS " + Sessions.TABLE_NAME);
		db.execSQL("UPDATE " + Sessions.TABLE_NAME + " SET " + Sessions.COLUMN_NAME_STATUS + " = " + HeartRateSession.SYNCRONIZED_STATUS + 
				" WHERE " + Sessions.COLUMN_NAME_STATUS + " = " + HeartRateSession.SYNCHRONIZING_STATUS);
		onCreate(db);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

}
