package com.cardiomood.sport.android.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.cardiomood.sport.android.db.DBContract;
import com.cardiomood.sport.android.db.entity.HeartRateEntity;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 19:04
 */
public class HeartRateDAO extends BaseDAO<HeartRateEntity> implements DBContract.HeartRateData {

    public static final String[] ALL_COLUMNS = new String[]{
            _ID,
            COLUMN_NAME_WORKOUT_ID,
            COLUMN_NAME_BPM,
            COLUMN_NAME_RR,
            COLUMN_NAME_ENERGY_EXPENDED,
            COLUMN_NAME_TIMESTAMP,
            COLUMN_NAME_SYNC,
    };

    public HeartRateDAO(Context context) {
        super(context);
    }

    @Override
    public ContentValues getContentValues(HeartRateEntity item) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, item.getId());
        cv.put(COLUMN_NAME_WORKOUT_ID, item.getWorkoutId());
        cv.put(COLUMN_NAME_BPM, item.getWorkoutId());
        cv.put(COLUMN_NAME_RR, item.getWorkoutId());
        cv.put(COLUMN_NAME_ENERGY_EXPENDED, item.getWorkoutId());
        cv.put(COLUMN_NAME_TIMESTAMP, item.getWorkoutId());
        cv.put(COLUMN_NAME_SYNC, item.getWorkoutId());
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
    public HeartRateEntity loadFromCursor(Cursor cursor) {
        return new HeartRateEntity(cursor);
    }


}
