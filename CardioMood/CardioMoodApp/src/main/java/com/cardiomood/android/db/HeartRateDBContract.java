package com.cardiomood.android.db;

import android.provider.BaseColumns;

/**
 * A contract class for database schema
 * @author Anton Danshin
 */
public interface HeartRateDBContract {
	
	String DATABASE_NAME = "heart_rate.db";
	int DATABASE_VERSION = 21;
	
	public static interface HeartRateData extends BaseColumns {
		String TABLE_NAME = "heart_rate_data";
		String COLUMN_NAME_SESSION_ID = "session_id";
		String COLUMN_NAME_BPM = "bpm";
		String COLUMN_NAME_RR_TIME = "rr_time";
		String COLUMN_NAME_TIME_STAMP = "time_stamp";
	}	
	
	public static interface Sessions extends BaseColumns {
		String TABLE_NAME = "heart_rate_sessions";
        String COLUMN_NAME_EXTERNAL_ID = "external_id";
		String COLUMN_NAME_USER_ID = "user_id";
		String COLUMN_NAME_NAME = "name";
		String COLUMN_NAME_DESCRIPTION = "description";
		String COLUMN_NAME_DATE_STARTED = "date_started";
		String COLUMN_NAME_DATE_ENDED = "date_ended";
		String COLUMN_NAME_STATUS = "status";
	}

    public static interface Users extends BaseColumns {
        String TABLE_NAME = "users";
        String COLUMN_NAME_EXTERNAL_ID = "external_id";
        String COLUMN_NAME_EMAIL = "email";
        String COLUMN_NAME_STATUS = "status";
    }
	
	public static abstract class SQL {
		private static final String TEXT_TYPE = " TEXT";
		private static final String COMMA_SEP = ",";
		
		public static final String CREATE_TABLE_HR_DATA =
			    "CREATE TABLE " + HeartRateData.TABLE_NAME + " (" +
			    HeartRateData._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
			    HeartRateData.COLUMN_NAME_SESSION_ID + " INTEGER" + COMMA_SEP +
			    HeartRateData.COLUMN_NAME_BPM + " INTEGER" + COMMA_SEP +
			    HeartRateData.COLUMN_NAME_RR_TIME + " REAL" + COMMA_SEP +
			    HeartRateData.COLUMN_NAME_TIME_STAMP + " INTEGER" + COMMA_SEP +
			    "FOREIGN KEY(" + HeartRateData.COLUMN_NAME_SESSION_ID + ") REFERENCES " +
			    Sessions.TABLE_NAME + "(" + Sessions._ID + ")" +
			    " )";
		public static final String CREATE_TABLE_SESSIONS = 
				"CREATE TABLE " + Sessions.TABLE_NAME + " (" +
				Sessions._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                Sessions.COLUMN_NAME_EXTERNAL_ID + " INTEGER" + COMMA_SEP +
			    Sessions.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
			    Sessions.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
			    Sessions.COLUMN_NAME_USER_ID + " INTEGER" + COMMA_SEP +
			    Sessions.COLUMN_NAME_DATE_STARTED + " INTEGER" + COMMA_SEP +
			    Sessions.COLUMN_NAME_DATE_ENDED + " INTEGER" + COMMA_SEP +
			    Sessions.COLUMN_NAME_STATUS + " INTEGER" + COMMA_SEP +
                "FOREIGN KEY(" + Sessions.COLUMN_NAME_USER_ID+ ") REFERENCES " +
                Users.TABLE_NAME + "(" + Users._ID + ")" +
			    " )";
        public static final String CREATE_TABLE_USERS =
                "CREATE TABLE " + Users.TABLE_NAME + " (" +
                 Users._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                 Users.COLUMN_NAME_EMAIL + TEXT_TYPE + " UNIQUE" + COMMA_SEP +
                 Users.COLUMN_NAME_EXTERNAL_ID + " INTEGER UNIQUE" + COMMA_SEP +
                 Users.COLUMN_NAME_STATUS + TEXT_TYPE +
                " )";
    }
}
