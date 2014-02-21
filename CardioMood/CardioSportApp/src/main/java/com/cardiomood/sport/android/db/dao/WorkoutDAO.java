package com.cardiomood.sport.android.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.cardiomood.sport.android.db.DBContract;
import com.cardiomood.sport.android.db.entity.WorkoutEntity;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 19:01
 */
public class WorkoutDAO extends BaseDAO<WorkoutEntity> implements DBContract.Workouts {

    public static final String[] ALL_COLUMNS = new String[] {
            _ID,
            COLUMN_NAME_NAME,
            COLUMN_NAME_DESCRIPTION,
            COLUMN_NAME_START_DATE,
            COLUMN_NAME_PLANNED_START_DATE,
            COLUMN_NAME_STOP_DATE,
            COLUMN_NAME_USER_ID,
            COLUMN_NAME_COACH_ID,
            COLUMN_NAME_EXTERNAL_ID,
            COLUMN_NAME_STATUS
    };

    public WorkoutDAO(Context context) {
        super(context);
    }

    @Override
    public ContentValues getContentValues(WorkoutEntity item) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, item.getId());
        cv.put(COLUMN_NAME_COACH_ID, item.getCoachId());
        cv.put(COLUMN_NAME_NAME, item.getName());
        cv.put(COLUMN_NAME_DESCRIPTION, item.getDescription());
        cv.put(COLUMN_NAME_START_DATE, item.getStartDate());
        cv.put(COLUMN_NAME_USER_ID, item.getUserId());
        cv.put(COLUMN_NAME_PLANNED_START_DATE, item.getPlannedStartDate());
        cv.put(COLUMN_NAME_EXTERNAL_ID, item.getExternalId());
        cv.put(COLUMN_NAME_STOP_DATE, item.getStopDate());
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
    public WorkoutEntity loadFromCursor(Cursor cursor) {
        return new WorkoutEntity(cursor);
    }


}
