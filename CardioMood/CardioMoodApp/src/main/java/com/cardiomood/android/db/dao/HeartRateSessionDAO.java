package com.cardiomood.android.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cardiomood.android.db.HeartRateDBContract;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;

import java.util.ArrayList;
import java.util.List;

public class HeartRateSessionDAO extends BaseDAO<HeartRateSession> implements HeartRateDBContract.Sessions {
	
	private static final String [] ALL_COLUMNS = new String[] {
		_ID, COLUMN_NAME_USER_ID, COLUMN_NAME_EXTERNAL_ID, COLUMN_NAME_ORIGINAL_SESSION_ID, COLUMN_NAME_NAME, COLUMN_NAME_DESCRIPTION, COLUMN_NAME_DATE_STARTED,
		COLUMN_NAME_DATE_ENDED, COLUMN_NAME_STATUS
	};


    public HeartRateSessionDAO(SQLiteDatabase database) {
        super(database);
    }

    public HeartRateSessionDAO() {
    }

    @Override
    public ContentValues getContentValues(HeartRateSession item) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_USER_ID, item.getUserId());
        values.put(COLUMN_NAME_EXTERNAL_ID, item.getExternalId());
        values.put(COLUMN_NAME_ORIGINAL_SESSION_ID, item.getOriginalSessionId());
        values.put(COLUMN_NAME_NAME, item.getName());
        values.put(COLUMN_NAME_DESCRIPTION, item.getDescription());
        values.put(COLUMN_NAME_DATE_STARTED, item.getDateStarted() == null ? null : item.getDateStarted().getTime());
        values.put(COLUMN_NAME_DATE_ENDED, item.getDateEnded() == null ? null : item.getDateEnded().getTime());
        values.put(COLUMN_NAME_STATUS, String.valueOf(item.getStatus()));
        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String[] getColumnNames() {
        return ALL_COLUMNS;
    }

    @Override
    public HeartRateSession loadFromCursor(Cursor cursor) {
        HeartRateSession session = new HeartRateSession(cursor);
        return session;
    }

    public List<HeartRateSession> getAllSessions() {
        List<HeartRateSession> sessions = new ArrayList<HeartRateSession>();
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            Cursor cursor = db.query(getTableName(), getColumnNames(), null, null, null, null, COLUMN_NAME_DATE_STARTED + " desc");
            if (cursor.moveToFirst()) {
                do {
                    sessions.add(new HeartRateSession(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return sessions;
    }

    public List<HeartRateSession> getAllSessions(int step, int from) {
        List<HeartRateSession> sessions = new ArrayList<HeartRateSession>();
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            Cursor cursor = db.query(getTableName(), getColumnNames(), null, null, null, null, _ID + " desc limit "+step+" offset " + from);
            if (cursor.moveToFirst()) {
                do {
                    sessions.add(new HeartRateSession(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return sessions;
    }

    @Override
    public void delete(long id) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                super.delete(id);
                int k = db.delete(HeartRateDataItem.TABLE_NAME, HeartRateDataItem.COLUMN_NAME_SESSION_ID + " = ?", new String[]{String.valueOf(id)});
                db.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.e("HeartRateSessionDAO", "Failed to delete session", ex);
            } finally {
               db.endTransaction();
            }
        }
    }

    public HeartRateSession insert(HeartRateSession session, List<HeartRateDataItem> items) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                session = insert(session);
                for (HeartRateDataItem item: items) {
                    item.setSessionId(session.getId());
                }
                new HeartRateDataItemDAO(getDatabase()).bulkInsert(items);
                db.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.e("HeartRateSessionDAO", "insert() failed", ex);
            } finally {
                db.endTransaction();
            }
        }
        return session;
    }

    public List<HeartRateSession> getAllSessionsOfUser(Long userId, int step, int from) {
        List<HeartRateSession> sessions = new ArrayList<HeartRateSession>();
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            Cursor cursor = db.query(
                    getTableName(),
                    getColumnNames(),
                    COLUMN_NAME_USER_ID +"=? and " + COLUMN_NAME_STATUS + "<>?",
                    new String[]{String.valueOf(userId), String.valueOf(SessionStatus.IN_PROGRESS)},
                    null,
                    null,
                    _ID + " desc limit "+step+" offset " + from);
            if (cursor.moveToFirst()) {
                do {
                    sessions.add(new HeartRateSession(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return sessions;
    }

    public List<HeartRateSession> getSessions(String selection, String[] selectionArgs) {
        List<HeartRateSession> sessions = new ArrayList<HeartRateSession>();
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            Cursor cursor = db.query(getTableName(), getColumnNames(), selection, selectionArgs, null, null, COLUMN_NAME_DATE_STARTED + " desc");
            if (cursor.moveToFirst()) {
                do {
                    sessions.add(new HeartRateSession(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return sessions;
    }
}
