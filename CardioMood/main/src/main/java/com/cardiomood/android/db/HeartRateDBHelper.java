package com.cardiomood.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HeartRateDBHelper extends SQLiteOpenHelper implements HeartRateDBContract {

	public HeartRateDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
		db.execSQL("DROP TABLE IF EXISTS " + HeartRateData.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + Sessions.TABLE_NAME);
		onCreate(db);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

}
