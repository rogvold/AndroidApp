package com.cardiomood.sport.android.db.entity;

import android.database.Cursor;

import com.cardiomood.sport.android.client.json.WorkoutStatus;
import com.cardiomood.sport.android.db.DBContract;

import java.io.Serializable;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 19:16
 */
public class WorkoutEntity extends Entity implements Serializable, DBContract.Workouts {

    private Long coachId;
    private String description;
    private String name;
    private long startDate;
    private long plannedStartDate;
    private long stopDate;
    private WorkoutStatus status = WorkoutStatus.NEW;
    private Long userId;
    private Long externalId;
    private Long templateId;
    private boolean sync;


    public WorkoutEntity() {
    }

    public WorkoutEntity(Cursor cursor) {
        super(cursor);
        coachId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_COACH_ID));
        description = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION));
        name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME));
        startDate = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_START_DATE));
        plannedStartDate = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_PLANNED_START_DATE));
        stopDate = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_STOP_DATE));
        status = WorkoutStatus.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_STATUS)));
        userId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_USER_ID));
        externalId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_EXTERNAL_ID));
    }

    public Long getCoachId() {
        return coachId;
    }

    public void setCoachId(Long coachId) {
        this.coachId = coachId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(long plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public long getStopDate() {
        return stopDate;
    }

    public void setStopDate(long stopDate) {
        this.stopDate = stopDate;
    }

    public WorkoutStatus getStatus() {
        return status;
    }

    public void setStatus(WorkoutStatus status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }
}
