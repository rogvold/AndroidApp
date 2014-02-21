package com.cardiomood.sport.android.db;

import android.provider.BaseColumns;

/**
 * A contract class for database schema
 * @author Anton Danshin
 */
public interface DBContract {
	
	String DATABASE_NAME = "cardio_sport.db";
	int DATABASE_VERSION = 1000;

    public static interface Accounts extends BaseColumns {
        String TABLE_NAME = "users";
        String COLUMN_NAME_EXTERNAL_ID = "external_id";
        String COLUMN_NAME_EMAIL = "email";
        String COLUMN_NAME_CREATED = "created";
        String COLUMN_NAME_LAST_LOGIN = "last_login";
        String COLUMN_NAME_PROPERTIES = "properties";
    }
	
	public static interface HeartRateData extends BaseColumns {
		String TABLE_NAME = "heart_rate_data";
		String COLUMN_NAME_WORKOUT_ID = "workout_id";
		String COLUMN_NAME_BPM = "bpm";
		String COLUMN_NAME_RR = "rr";
        String COLUMN_NAME_ENERGY_EXPENDED = "energy_expended";
		String COLUMN_NAME_TIMESTAMP = "time_stamp";
        String COLUMN_NAME_SYNC = "sync";
	}

    public static interface GPSInfo extends BaseColumns {
        String TABLE_NAME = "gps_info";
        String COLUMN_NAME_WORKOUT_ID = "workout_id";
        String COLUMN_NAME_ALTITUDE = "altitude";
        String COLUMN_NAME_LONGITUDE = "longitude";
        String COLUMN_NAME_LATITUDE = "latitude";
        String COLUMN_NAME_ACCURACY = "accuracy";
        String COLUMN_NAME_SPEED = "speed";
        String COLUMN_NAME_TIMESTAMP = "time_stamp";
        String COLUMN_NAME_SYNC = "sync";
    }

    public static interface ActivityInfoData extends BaseColumns {
        String TABLE_NAME = "activity_info";
        String COLUMN_NAME_WORKOUT_ID = "workout_id";
        String COLUMN_NAME_SYNC = "sync";
        String COLUMN_NAME_NAME = "name";
        String COLUMN_NAME_DESCRIPTION = "description";
        String COLUMN_NAME_START_DATE = "start_date";
        String COLUMN_NAME_END_DATE = "end_date";
        String COLUMN_NAME_MAX_HR = "max_hr";
        String COLUMN_NAME_MIN_HR = "min_hr";
        String COLUMN_NAME_MAX_TENSION = "max_tension";
        String COLUMN_NAME_MIN_TENSION = "min_tension";
        String COLUMN_NAME_MAX_SPEED = "max_speed";
        String COLUMN_NAME_MIN_SPEED = "min_speed";
        String COLUMN_NAME_TEMPLATE_ID = "external_id";
        String COLUMN_NAME_ORDER_NUMBER = "order_number";
        String COLUMN_NAME_DURATION = "duration";
        String COLUMN_NAME_STATUS = "status";
    }

    public static interface AudioTracks extends BaseColumns {
        String TABLE_NAME = "audio_tracks";
        String COLUMN_NAME_NAME = "name";
        String COLUMN_NAME_DESCRIPTION = "description";
        String COLUMN_NAME_FILE_NAME = "file_name";
        String COLUMN_NAME_HASH = "hash";
        String COLUMN_NAME_EXTERNAL_ID = "external_id";
        String COLUMN_NAME_BPM = "bpm";
    }

    public static interface Workouts extends BaseColumns {
		String TABLE_NAME = "workouts";
        String COLUMN_NAME_COACH_ID = "coach_id";
		String COLUMN_NAME_USER_ID = "user_id";
		String COLUMN_NAME_NAME = "name";
		String COLUMN_NAME_DESCRIPTION = "description";
		String COLUMN_NAME_START_DATE = "start_date";
        String COLUMN_NAME_PLANNED_START_DATE = "planned_start_date";
        String COLUMN_NAME_EXTERNAL_ID = "external_id";
        String COLUMN_NAME_STOP_DATE = "stop_date";
        String COLUMN_NAME_STATUS = "status";
	}
	
	public static abstract class SQL {
		private static final String TEXT_TYPE = " TEXT";
		private static final String COMMA_SEP = ",";

        public static final String CREATE_TABLE_ACCOUNTS =
                "CREATE TABLE " + Accounts.TABLE_NAME + " (" +
                        Accounts._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        Accounts.COLUMN_NAME_EXTERNAL_ID + " INTEGER" + COMMA_SEP +
                        Accounts.COLUMN_NAME_EMAIL + TEXT_TYPE + COMMA_SEP +
                        Accounts.COLUMN_NAME_CREATED + " INTEGER" + COMMA_SEP +
                        Accounts.COLUMN_NAME_LAST_LOGIN + " INTEGER" + COMMA_SEP +
                        Accounts.COLUMN_NAME_PROPERTIES + TEXT_TYPE + COMMA_SEP +
                        " )";
		
		public static final String CREATE_TABLE_HR_DATA =
			    "CREATE TABLE " + HeartRateData.TABLE_NAME + " (" +
                        HeartRateData._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        HeartRateData.COLUMN_NAME_WORKOUT_ID + " INTEGER" + COMMA_SEP +
                        HeartRateData.COLUMN_NAME_BPM + " INTEGER" + COMMA_SEP +
                        HeartRateData.COLUMN_NAME_ENERGY_EXPENDED + " INTEGER" + COMMA_SEP +
                        HeartRateData.COLUMN_NAME_RR + TEXT_TYPE + COMMA_SEP +
                        HeartRateData.COLUMN_NAME_TIMESTAMP + " INTEGER" + COMMA_SEP +
                        HeartRateData.COLUMN_NAME_SYNC + " INTEGER" +
			    " )";
		public static final String CREATE_TABLE_WORKOUTS =
				"CREATE TABLE " + Workouts.TABLE_NAME + " (" +
                        Workouts._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        Workouts.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                        Workouts.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                        Workouts.COLUMN_NAME_USER_ID + " INTEGER" + COMMA_SEP +
                        Workouts.COLUMN_NAME_COACH_ID + " INTEGER" + COMMA_SEP +
                        Workouts.COLUMN_NAME_PLANNED_START_DATE + " INTEGER" + COMMA_SEP +
                        Workouts.COLUMN_NAME_START_DATE + " INTEGER" + COMMA_SEP +
                        Workouts.COLUMN_NAME_STOP_DATE + " INTEGER" + COMMA_SEP +
                        Workouts.COLUMN_NAME_EXTERNAL_ID+ " INTEGER" + COMMA_SEP +
                        Workouts.COLUMN_NAME_STATUS + TEXT_TYPE +
			    " )";
        public static final String CREATE_TABLE_GPS_INFO =
                "CREATE TABLE " + GPSInfo.TABLE_NAME + " (" +
                        GPSInfo._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        GPSInfo.COLUMN_NAME_WORKOUT_ID + " INTEGER" + COMMA_SEP +
                        GPSInfo.COLUMN_NAME_TIMESTAMP + " INTEGER" + COMMA_SEP +
                        GPSInfo.COLUMN_NAME_LONGITUDE + " REAL" + COMMA_SEP +
                        GPSInfo.COLUMN_NAME_ALTITUDE + " REAL" + COMMA_SEP +
                        GPSInfo.COLUMN_NAME_LATITUDE + " REAL" + COMMA_SEP +
                        GPSInfo.COLUMN_NAME_SPEED + " REAL" + COMMA_SEP +
                        GPSInfo.COLUMN_NAME_SYNC + " INTEGER" + COMMA_SEP +
                        GPSInfo.COLUMN_NAME_ACCURACY + " REAL" +
                " )";
        public static final String CREATE_TABLE_ACTIVITY_INFO_DATA =
                "CREATE TABLE " + ActivityInfoData.TABLE_NAME + " (" +
                        ActivityInfoData._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_WORKOUT_ID + " INTEGER" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_SYNC + " INTEGER" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_TEMPLATE_ID + " INTEGER" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_START_DATE + " INTEGER" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_END_DATE + " INTEGER" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_DURATION + " INTEGER" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_STATUS + TEXT_TYPE + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_MAX_HR + " INTEGER" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_MIN_HR + " INTEGER" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_MAX_SPEED + " REAL" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_MIN_SPEED + " REAL" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_MAX_TENSION + " REAL" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_MIN_TENSION + " REAL" + COMMA_SEP +
                        ActivityInfoData.COLUMN_NAME_ORDER_NUMBER + " INTEGER" +
                " )";

	}
}
