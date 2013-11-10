package com.cardiomood.android.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cardiomood.android.db.HeartRateDBContract;
import com.cardiomood.android.db.model.HeartRateDataItem;

import java.util.ArrayList;
import java.util.List;

public class HeartRateDataItemDAO extends BaseDAO<HeartRateDataItem> implements HeartRateDBContract.HeartRateData {

    public HeartRateDataItemDAO() {
    }

    public HeartRateDataItemDAO(SQLiteDatabase database) {
        super(database);
    }

    private static final String[] ALL_COLUMNS = new String[] {
		_ID, COLUMN_NAME_SESSION_ID, COLUMN_NAME_BPM, COLUMN_NAME_RR_TIME, COLUMN_NAME_TIME_STAMP
	};


    @Override
    public ContentValues getContentValues(HeartRateDataItem item) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, item.getId());
        cv.put(COLUMN_NAME_SESSION_ID, item.getSessionId());
        cv.put(COLUMN_NAME_BPM, item.getHeartBeatsPerMinute());
        cv.put(COLUMN_NAME_RR_TIME, item.getRrTime());
        cv.put(COLUMN_NAME_TIME_STAMP, item.getTimeStamp() == null ? null : item.getTimeStamp().getTime());
        return cv;
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
    public HeartRateDataItem loadFromCursor(Cursor cursor) {
        HeartRateDataItem item = new HeartRateDataItem(cursor);
        cursor.close();
        return item;
    }

    public List<HeartRateDataItem> getItemsBySessionId(long sessionId) {
        List<HeartRateDataItem> result = new ArrayList<HeartRateDataItem>();
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            Cursor cursor = db.query(
                        getTableName(),
                        getColumnNames(),
                        COLUMN_NAME_SESSION_ID + " = ?",
                        new String[]{String.valueOf(sessionId)},
                        null,
                        null,
                        _ID
                    );
            if (cursor.moveToFirst()) {
                do {
                    result.add(new HeartRateDataItem(cursor));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return result;
    }


}
