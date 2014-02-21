package com.cardiomood.sport.android.db.entity;

import android.database.Cursor;

import java.io.Serializable;

/**
 * Project: CardioSport
 * User: danon
 * Date: 14.06.13
 * Time: 19:21
 */
public class WorkoutElementEntity extends Entity implements Serializable {
    public static final String COLUMN_NAME_WORKOUT_ID = "workout_id";
    public static final String COLUMN_NAME_SYNC = "sync";

    private Long workoutId;
    private boolean sync;

    public WorkoutElementEntity() {
    }

    public WorkoutElementEntity(Cursor cursor) {
        super(cursor);
        workoutId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_WORKOUT_ID));
        sync = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_SYNC)) == 0 ? false : true;
    }

    public Long getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(Long workoutId) {
        this.workoutId = workoutId;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }
}
