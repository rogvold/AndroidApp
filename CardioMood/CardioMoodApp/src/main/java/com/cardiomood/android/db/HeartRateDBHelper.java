package com.cardiomood.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HeartRateDBHelper extends SQLiteOpenHelper implements HeartRateDBContract {

    private static final String TAG = HeartRateDBHelper.class.getSimpleName();

    private static final short[] GOOD_SESSION_RR = new short[] {
            695,710,710,703,710,679,695,664,687,656,671,679,710,703,726,718,710,718,734,695,710,726,718,710,703,710,703,726,703,710,703,726,695,718,710,718,726,710,710,718,695,695,703,695,718,726,734,718,750,734,734,710,718,726,710,718,703,710,687,687,679,679,679,671,664,640,648,632,640,656,656,664,703,703,726,757,757,750,726,734,687,695,679,679,664,679,695,695,679,671,656,648,664,671,679,718,710,742,726,710,679,664,664,640,617,617,617,625,625,664,687,695,695,718,718,718,718,742,726,710,679,687,687,726,718,710,703,710,695,679,671,671,656,632,640,617,632,656,687,718,757,773,789,765,757,742,757,734,742,742,710,726,687,671,640,632,640,664,664,656,671,679,656,656,625,640,625,609,609,601,609,585,585,562,578,570,539,554,539,562,554,546,554,554,585,585,617,617,640,656,671,656,664,664,640,648,671,664,648,656,625,632,617,617,632,617,625,601,609,593,593,570,593,570,578,585,585,609,648,664,671,679,679,687,664,679,656,671,664,679,656,656,640,640,609,617,601,593,570,593,609,625,679,687,718,726,742,734,726,726,718,726,679,703,718,710,710,687,664,664,648,648,648,656,671,687,687,671,664,679,664,671,671,687,671,656,656
    };

    private static final short[] STRESSED_SESSION_RR = new short[] {
            746,750,758,768,759,754,748,749,750,769,754,755,762,760,751,760,771,761,768,776,782,786,781,788,793,791,762,755,768,763,761,747,756,762,767,766,762,775,781,800,795,796,779,778,787,791,788,785,798,811,805,797,804,802,783,771,770,772,785,790,779,774,791,791,778,761,775,768,763,764,779,784,786,774,779,782,776,775,785,778,760,759,767,776,791,788,788,798,797,787,766,779,781,791,796,790,805,822,800,802,805,798,785,779,783,791,808,805,782,768,767,777,770,765,771,791,792,784,792,793,796,784,786,798,803,794,779,785,787,789,801,809,791,789,798,795,784,775,788,795,806,791,787,807,811,810,791,801,807,788,796,779,778,787,791,788,785,798,811,805,797,804,802,783,771,770,772,785,790,779,774,791,791,778,761,775,768,763,764, 762,755,768,763,761,747,756,762
    };

    private static final short[] BAD_SESSION_RR = new short[] {
            250,23,312,164,640,625,500,523,656,476,421,609,601,445,578,562,429,679,625,437,765,23,312,773,23,312,484,523,468,359,757,23,312,765,23,312,476,437,539,460,585,640,601,742,23,312,750,23,312,601,484,492,664,593,750,664,523,468,578,554,484,492,570,710,632,507,562,843,23,312,851,23,312,625,437,515,562,492,523,609,562,562,640,679,562,726,648,578,687,562,539,703,546,515,656,539,484,546,468,554,539,546,578,562,593,515,445,492,539,429,476,656,562,507,601,484,750,23,312,757,23,312,671,476,554,562,554,492,445,695,554,468,585,507,492,593,507,484,617,453,492,492,445,414,562,562,492,765,23,312,765,539,773,632,609,23,312,578,742,23,312,742,23,312,640,523,515,687,570,593,492,773,23,312,781,23,312,625,453,617,609,460,609,648,640,648,703,718,679,492,570,382,757,23,312,765,460,492,617,23,312,132,476,718,515,437,671,687,570,554,507,632,507,460,531,625,632,507,578,460,507,492,750,23,312,750,23,312,484,453,460,867,23,312,867,23,312,617,750,23,312,750,531,500,546,539,23,312,414,359,460,375,492,570,429,539,421,757,23,312,765,476,460,500,554,476,23,312,554,492,453,500,406,421,476,406,468,500,406,484,617,445,484,601,445,546,398,750
    };

    private Context context;
    private PreferenceHelper pHelper;

	public HeartRateDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        pHelper = new PreferenceHelper(context);
        pHelper.setPersistent(true);

        try {
            backupDB();
        } catch (Exception ex) {
            Log.w(TAG, "onCreate(): failed to backup DB", ex);
        }
	}

    private void backupDB() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dbFile = context.getDatabasePath(DATABASE_NAME);
            File outFile = new File(Environment.getExternalStorageDirectory(), "cardiomood.sqlite.db");
            try {
                FileUtils.copyFile(dbFile, outFile);
            } catch (Exception ex) {
                Log.e("HeartRateDBHelper", "backupDB() failed", ex);
            }
        }
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create database entities                                 
		try {
			// create
            db.execSQL(SQL.CREATE_TABLE_USERS);
            db.execSQL(SQL.CREATE_TABLE_SESSIONS);
			db.execSQL(SQL.CREATE_TABLE_HR_DATA);

            // populate with sample sessions
            createSampleSessions(db);
		} catch (Exception e) {
			// tables already exist
            Log.d(TAG, "onCreate() exception", e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Upgrade or downgrade the database schema
		if (oldVersion <= 20) {
            try {
                db.execSQL(SQL.CREATE_TABLE_USERS);

                String ADD_COLUMN_EXTERNAL_ID = "ALTER TABLE " + Sessions.TABLE_NAME
                        + " ADD COLUMN " + Sessions.COLUMN_NAME_EXTERNAL_ID + " INTEGER";
                db.execSQL(ADD_COLUMN_EXTERNAL_ID);
            } catch (Exception e) {
                Log.d(TAG, "onUpgrade() exception", e);
            }

        }

        createSampleSessions(db);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

    private void createSampleSessions(SQLiteDatabase db) {
        HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO(db);
        // bad session
        createSessionIfNotExists(
                sessionDAO,
                ConfigurationConstants.DB_BAD_SESSION_ID,
                BAD_SESSION_RR,
                context.getString(R.string.bad_session_name)
        );
        // stressed session
        createSessionIfNotExists(
                sessionDAO,
                ConfigurationConstants.DB_STRESSED_SESSION_ID,
                STRESSED_SESSION_RR,
                context.getString(R.string.stressed_session_name)
        );
        // athlete session
        createSessionIfNotExists(
                sessionDAO,
                ConfigurationConstants.DB_ATHLETE_SESSION_ID,
                ATHLETE_SESSION_RR,
                context.getString(R.string.athlete_session_name)
        );
        // good session
        createSessionIfNotExists(
                sessionDAO,
                ConfigurationConstants.DB_GOOD_SESSION_ID,
                GOOD_SESSION_RR,
                context.getString(R.string.good_session_name)
        );
    }

    private void createSessionIfNotExists(HeartRateSessionDAO sessionDAO, String configId, short[] intervals, String name) {
        if (pHelper.getLong(configId, -1L) == -1L || !sessionDAO.exists(pHelper.getLong(configId, -1L))) {
            HeartRateSession session = createSession(sessionDAO, intervals, name);
            pHelper.putLong(configId, session.getId());
        }
    }

    private HeartRateSession createSession(HeartRateSessionDAO sessionDAO, short[] intervals, String title) {
        List<HeartRateDataItem> sampleItems = new ArrayList<HeartRateDataItem>();
        long length = 0;
        final long ts = System.currentTimeMillis();
        for (short rr: intervals) {
            HeartRateDataItem item = new HeartRateDataItem();
            item.setTimeStamp(new Date(ts + length));
            item.setRrTime(rr);
            item.setHeartBeatsPerMinute((int) (1 / rr * 60000));
            length += rr;
            sampleItems.add(item);
        }

        HeartRateSession session = new HeartRateSession();
        session.setDateStarted(new Date(ts));
        session.setDateEnded(new Date(ts + length));
        session.setStatus(SessionStatus.SYNCHRONIZED);
        session.setName(title);

        return sessionDAO.insert(session, sampleItems);
    }


    private static final short[] ATHLETE_SESSION_RR = new short[] {
            1398,
            1399,
            1421,
            1336,
            1368,
            1375,
            1367,
            1343,
            1336,
            1336,
            1336,
            1414,
            1383,
            1430,
            1391,
            1406,
            1336,
            1336,
            1367,
            1336,
            1422,
            1375,
            1343,
            1360,
            1328,
            1359,
            1328,
            1313,
            1320,
            1305,
            1344,
            1359,
            1398,
            1422,
            1383,
            1367,
            1258,
            1242,
            1172,
            1141,
            1117,
            1172,
            1242,
            1273,
            1360,
            1359,
            1352,
            1390,
            1383,
            1383,
            1352,
            1289,
            1367,
            1344,
            1382,
            1352,
            1359,
            1368,
            1343,
            1391,
            1406,
            1438,
            1508,
            1406,
            1375,
            1328,
            1383,
            1367,
            1359,
            1367,
            1360,
            1390,
            1375,
            1360,
            1336,
            1320,
            1320,
            1211,
            1219,
            1227,
            1211,
            1265,
            1235,
            1250,
            1265,
            1235,
            1265,
            1235,
            1203,
            1234,
            1195,
            1227,
            1227,
            1234,
            1289
    };
}
