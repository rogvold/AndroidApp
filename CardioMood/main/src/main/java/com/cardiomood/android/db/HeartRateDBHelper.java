package com.cardiomood.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.tools.config.PreferenceHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HeartRateDBHelper extends SQLiteOpenHelper implements HeartRateDBContract {

    private static final short[] GOOD_SESSION_RR = new short[] {
            695,710,710,703,710,679,695,664,687,656,671,679,710,703,726,718,710,718,734,695,710,726,718,710,703,710,703,726,703,710,703,726,695,718,710,718,726,710,710,718,695,695,703,695,718,726,734,718,750,734,734,710,718,726,710,718,703,710,687,687,679,679,679,671,664,640,648,632,640,656,656,664,703,703,726,757,757,750,726,734,687,695,679,679,664,679,695,695,679,671,656,648,664,671,679,718,710,742,726,710,679,664,664,640,617,617,617,625,625,664,687,695,695,718,718,718,718,742,726,710,679,687,687,726,718,710,703,710,695,679,671,671,656,632,640,617,632,656,687,718,757,773,789,765,757,742,757,734,742,742,710,726,687,671,640,632,640,664,664,656,671,679,656,656,625,640,625,609,609,601,609,585,585,562,578,570,539,554,539,562,554,546,554,554,585,585,617,617,640,656,671,656,664,664,640,648,671,664,648,656,625,632,617,617,632,617,625,601,609,593,593,570,593,570,578,585,585,609,648,664,671,679,679,687,664,679,656,671,664,679,656,656,640,640,609,617,601,593,570,593,609,625,679,687,718,726,742,734,726,726,718,726,679,703,718,710,710,687,664,664,648,648,648,656,671,687,687,671,664,679,664,671,671,687,671,656,656,648,648,632,632,609,640,648,648,640,679,687,710,695,718,734,734,710,718,718,718,703,664,664,648,671,664,710,734,750,742,734,710,710,679,679,648,656,625,625,617,632,625,617,625,601,593,593,593,601,632,664,687,703,703,695,710,695,687,656,664,648,656,648,664,640,656,640,640,632,617,625,617,640,640,648,656,851,828,781,757,710,710,679,687,664,695,671,671,664,687,656,656,648,640,632,632,609,625,601,601,601,601,617,609,632,632,632,632,640,625,632,625,632,609,609,601,593,609,585,609,617,617,648,632,640,609,609,593,585,578,437,140,578,578,570,578,562,554,554,554,539,546,531,546,539,539,562,539,554,562,562,562,570,585,562,578,578,570,562,578,570,570,585,570,578,578,562,570,562,578,570,570,570,570,578,578,562,593,562,570,570
    };

    private static final short[] STRESSED_SESSION_RR = new short[] {
            843,992,929,921,960,960,984,898,968,843,968,875,882,875,953,945,960,945,859,929,914,968,960,984,898,968,976,656,976,937,968,945,921,921,875,921,906,914,828,867,875,890,937,953,929,929,921,851,992,953,937,859,875,875,890,906,804,843,960,851,851,750,820,812,906,937,968,953,875,921,906,937,945,984,937,945,843,921,890,937,921,953,953,859,898,859,914,898,906,906,789,828,789,914,859,859,742,742,789,765,757,750,789,828,828,781,835,820,875,890,953,921,875,812,882,867,929,992,929,937,875,828,914,914,968,992,796,851,914,914,914,937,875,765,781,796,835,796,867,828,890,898,875,812,789,882,828,906,937,851,898,851,968,898,968,945,882,960,875,992,851,859,914,875,859,781,773,843,851,875,898,960,906,890,828,812,882,835,937,843,695,695,726,812,796,882,812,789,804,890,804,953,843,953,953,937,921,835,796,851,812,867,835,757,812,781,914,851,960,867,984,968,929,859,757,757,789,726,742,914,820,796,726,812,843,898,937,929,843,906,828,968,875,898,937,960,859,906,828,984,890,929,960,914,875,953,867,968,921,898,976,906,984,984,968,914,960,898,929,937,875,820,921,835,976,945,882,953,875,921,921,921,820,828,914,835,898,835,820,828,828,945,835,648,593,609,679,562,507,531,570,515,500,578,523,460,468,500,515,578,632,539,585,554,585,515,554,593,617,648,671,703,640,640,734,625,710,664,601,617,671,687,687,664,742,664,703,718,687,734,765,671,671,703,671,695,601,570,554,593,625,726,703,687,710,757,757,882,765,804,742,757,859,750,726,750,750,812,750
    };

    private Context context;
    private PreferenceHelper pHelper;

	public HeartRateDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        pHelper = new PreferenceHelper(context);
        pHelper.setPersistent(true);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create database entities                                 
		try {
			// create
            db.execSQL(SQL.CREATE_TABLE_SESSIONS);
			db.execSQL(SQL.CREATE_TABLE_HR_DATA);

            // populate with sample sessions
            createSampleSessions(db);
		} catch (Exception e) {
			// tables already exist
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Upgrade or downgrade the database schema
		createSampleSessions(db);
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

    private void createSampleSessions(SQLiteDatabase db) {
        HeartRateSessionDAO sessionDAO = new HeartRateSessionDAO(db);
        if (pHelper.getLong(ConfigurationConstants.DB_GOOD_SESSION_ID, -1L) == -1L) {
            HeartRateSession session = createSession(sessionDAO, GOOD_SESSION_RR, context.getString(R.string.good_session_name));
            pHelper.putLong(ConfigurationConstants.DB_GOOD_SESSION_ID, session.getId());
        }
        if (pHelper.getLong(ConfigurationConstants.DB_STRESSED_SESSION_ID, -1L) == -1L) {
            HeartRateSession session = createSession(sessionDAO, STRESSED_SESSION_RR, context.getString(R.string.stressed_session_name));
            pHelper.putLong(ConfigurationConstants.DB_STRESSED_SESSION_ID, session.getId());
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
            item.setHeartBeatsPerMinute((int) (1.0 / rr * 60000));
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

}
