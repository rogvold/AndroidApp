package com.cardiomood.sport.android.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cardiomood.sport.android.db.DBContract;
import com.cardiomood.sport.android.db.entity.GPSInfoEntity;

import org.apache.log4j.Logger;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 19:05
 */
public class GPSInfoDAO extends BaseDAO<GPSInfoEntity> implements DBContract.GPSInfo {
    private static final Logger log = Logger.getLogger(GPSInfoDAO.class);

    private static final String[] ALL_COLUMNS = {
            _ID,
            COLUMN_NAME_ACCURACY,
            COLUMN_NAME_ALTITUDE,
            COLUMN_NAME_LATITUDE,
            COLUMN_NAME_LONGITUDE,
            COLUMN_NAME_SPEED,
            COLUMN_NAME_TIMESTAMP,
            COLUMN_NAME_WORKOUT_ID,
            COLUMN_NAME_SYNC
    };

    public GPSInfoDAO(Context context) {
        super(context);
    }

    @Override
    public ContentValues getContentValues(GPSInfoEntity item) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, item.getId());
        cv.put(COLUMN_NAME_ACCURACY, item.getAccuracy());
        cv.put(COLUMN_NAME_ALTITUDE, item.getAltitude());
        cv.put(COLUMN_NAME_LATITUDE, item.getLatitude());
        cv.put(COLUMN_NAME_LONGITUDE, item.getLongitude());
        cv.put(COLUMN_NAME_SPEED, item.getSpeed());
        cv.put(COLUMN_NAME_SYNC, item.isSync());
        cv.put(COLUMN_NAME_TIMESTAMP, item.getTimestamp());
        cv.put(COLUMN_NAME_WORKOUT_ID, item.getWorkoutId());
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
    public GPSInfoEntity loadFromCursor(Cursor cursor) {
        return new GPSInfoEntity(cursor);
    }

    @Override
    public void delete(long id) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                int k = db.delete(getTableName(), _ID + " = ?", new String[]{String.valueOf(id)});
                if (log.isDebugEnabled()) {
                    log.debug("delete(id): id=" + id + " >> " + k + " rows deleted");
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
}
