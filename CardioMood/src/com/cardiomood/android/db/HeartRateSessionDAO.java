package com.cardiomood.android.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cardiomood.android.db.model.HeartRateSession;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class HeartRateSessionDAO extends BaseDAO<HeartRateSession> implements HeartRateDBContract.Sessions {
	
	private static final String [] ALL_COLUMNS = new String[] {
		_ID, COLUMN_NAME_USER_ID, COLUMN_NAME_NAME, COLUMN_NAME_DESCRIPTION, COLUMN_NAME_DATE_STARTED, 
		COLUMN_NAME_DATE_ENDED, COLUMN_NAME_STATUS
	};

	public HeartRateSessionDAO(Context context) {
		super(context);
	}

	@Override
	public HeartRateSession insert(HeartRateSession item) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_NAME_USER_ID, item.getUserId());
		values.put(COLUMN_NAME_NAME, item.getName());
		values.put(COLUMN_NAME_DESCRIPTION, item.getDescription());
		values.put(COLUMN_NAME_DATE_STARTED, item.getDateStarted() == null ? null : item.getDateStarted().getTime());
		values.put(COLUMN_NAME_DATE_ENDED, item.getDateEnded() == null ? null : item.getDateEnded().getTime());
		values.put(COLUMN_NAME_STATUS, item.getStatus());
		long insertId = getDatabase().insert(TABLE_NAME, null, values);
		if (insertId >= 0) {
			item.setId(insertId);
			return item;
		} else return null;
	}

	@Override
	public HeartRateSession update(HeartRateSession item) {
		ContentValues values = new ContentValues();
		values.put(_ID, item.getId());
		values.put(COLUMN_NAME_USER_ID, item.getUserId());
		values.put(COLUMN_NAME_NAME, item.getName());
		values.put(COLUMN_NAME_DESCRIPTION, item.getDescription());
		values.put(COLUMN_NAME_DATE_STARTED, item.getDateStarted() == null ? null : item.getDateStarted().getTime());
		values.put(COLUMN_NAME_DATE_ENDED, item.getDateEnded() == null ? null : item.getDateEnded().getTime());
		values.put(COLUMN_NAME_STATUS, item.getStatus());
		int r = getDatabase().update(TABLE_NAME, values, _ID + " = " + item.getId(), null);
		return r > 0 ? item : null;
	}

	@Override
	public HeartRateSession findById(long id) {
		Cursor cursor = getDatabase().query(TABLE_NAME, ALL_COLUMNS, _ID + " = " + id, null, null, null, null);
		HeartRateSession session = null;
		if (cursor.moveToFirst()) {
			session = cursorToHeartRateSession(cursor);
		}
		cursor.close();
		return session;
	}

	@Override
	public void delete(long id) {
		getDatabase().delete(TABLE_NAME, _ID + " = " + id, null);
	}
	
	public List<HeartRateSession> getAllSessions() {
		Cursor cursor = getDatabase().query(TABLE_NAME, ALL_COLUMNS, null, null, null, null, null);
		List<HeartRateSession> sessions = new ArrayList<HeartRateSession>();
		while (cursor.moveToNext()) {
			HeartRateSession session = cursorToHeartRateSession(cursor);
			sessions.add(session);
		}
		cursor.close();
		return sessions;
	}
	
	private HeartRateSession cursorToHeartRateSession(Cursor cursor) {
		HeartRateSession session = new HeartRateSession();
		session.setId(cursor.getLong(0));
		if (!cursor.isNull(1)) {
			session.setUserId(cursor.getLong(1));
		}
		session.setName(cursor.getString(2));
		session.setDescription(cursor.getString(3));
		if (!cursor.isNull(4)) {
			session.setDateStarted(new Date(cursor.getLong(4)));
		}
		if (!cursor.isNull(5)) {
			session.setDateEnded(new Date(cursor.getLong(5)));
		}
		session.setStatus(cursor.getLong(6));
		return session;
	}

}
