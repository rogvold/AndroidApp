package com.cardiomood.sport.android.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.cardiomood.sport.android.db.DBContract;
import com.cardiomood.sport.android.db.entity.ActivityInfoEntity;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 19:12
 */
public class ActivityInfoDAO extends BaseDAO<ActivityInfoEntity> implements DBContract.ActivityInfoData {

    public static final String[] ALL_COLUMNS = new String[]{
            _ID,
            COLUMN_NAME_WORKOUT_ID,
            COLUMN_NAME_SYNC,
            COLUMN_NAME_NAME,
            COLUMN_NAME_DESCRIPTION,
            COLUMN_NAME_START_DATE,
            COLUMN_NAME_END_DATE,
            COLUMN_NAME_MAX_HR,
            COLUMN_NAME_MIN_HR,
            COLUMN_NAME_MAX_TENSION,
            COLUMN_NAME_MIN_TENSION,
            COLUMN_NAME_MAX_SPEED,
            COLUMN_NAME_MIN_SPEED,
            COLUMN_NAME_TEMPLATE_ID,
            COLUMN_NAME_ORDER_NUMBER,
            COLUMN_NAME_DURATION,
            COLUMN_NAME_STATUS
    };

    public ActivityInfoDAO(Context context) {
        super(context);
    }

    @Override
    public ContentValues getContentValues(ActivityInfoEntity item) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, item.getId());
        cv.put(COLUMN_NAME_WORKOUT_ID, item.getWorkoutId());
        cv.put(COLUMN_NAME_SYNC, item.isSync());
        cv.put(COLUMN_NAME_NAME, item.getName());
        cv.put(COLUMN_NAME_DESCRIPTION, item.getDescription());
        cv.put(COLUMN_NAME_START_DATE, item.getStarted());
        cv.put(COLUMN_NAME_END_DATE, item.getEnded());
        cv.put(COLUMN_NAME_MAX_HR, item.getMaxHeartRate());
        cv.put(COLUMN_NAME_MIN_HR, item.getMinHeartRate());
        cv.put(COLUMN_NAME_MAX_TENSION, item.getMaxTension());
        cv.put(COLUMN_NAME_MIN_TENSION, item.getMinTension());
        cv.put(COLUMN_NAME_MAX_SPEED, item.getMaxSpeed());
        cv.put(COLUMN_NAME_MIN_SPEED, item.getMinSpeed());
        cv.put(COLUMN_NAME_TEMPLATE_ID, item.getExternalId());
        cv.put(COLUMN_NAME_ORDER_NUMBER, item.getOrderNumber());
        cv.put(COLUMN_NAME_DURATION, item.getDuration());
        cv.put(COLUMN_NAME_STATUS, item.getStatus().toString());
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
    public ActivityInfoEntity loadFromCursor(Cursor cursor) {
        return new ActivityInfoEntity(cursor);
    }


}
