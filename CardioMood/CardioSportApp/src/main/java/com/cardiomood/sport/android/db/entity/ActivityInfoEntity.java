package com.cardiomood.sport.android.db.entity;

import android.database.Cursor;

import com.cardiomood.sport.android.client.json.ActivityStatus;
import com.cardiomood.sport.android.db.DBContract;

import java.io.Serializable;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 19:13
 */
public class ActivityInfoEntity extends WorkoutElementEntity implements Serializable, DBContract.ActivityInfoData {

    private int minHeartRate;
    private int maxHeartRate;
    private double minTension;
    private double maxTension;
    private long duration;
    private String name;
    private String description;
    private double minSpeed;
    private double maxSpeed;
    private int orderNumber;
    private ActivityStatus status;
    private Long externalId;
    private Long templateId;
    private long started;
    private long ended;

    public ActivityInfoEntity() {
    }

    public ActivityInfoEntity(Cursor cursor) {
        super(cursor);
        minHeartRate = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_MIN_HR));
        maxHeartRate = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_MAX_HR));
        minTension = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_MIN_TENSION));
        maxTension = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_MAX_TENSION));
        minSpeed = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_MIN_SPEED));
        maxSpeed = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_MAX_SPEED));
        name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME));
        duration = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DURATION));
        maxTension = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_MAX_TENSION));
        description = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION));
        externalId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TEMPLATE_ID));
        orderNumber = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ORDER_NUMBER));
        String s = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_STATUS));
        status = s == null ? null : ActivityStatus.valueOf(s);
        started = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_START_DATE));
        ended = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_END_DATE));
    }

    public int getMinHeartRate() {
        return minHeartRate;
    }

    public void setMinHeartRate(int minHeartRate) {
        this.minHeartRate = minHeartRate;
    }

    public int getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(int maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public double getMinTension() {
        return minTension;
    }

    public void setMinTension(double minTension) {
        this.minTension = minTension;
    }

    public double getMaxTension() {
        return maxTension;
    }

    public void setMaxTension(double maxTension) {
        this.maxTension = maxTension;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getMinSpeed() {
        return minSpeed;
    }

    public void setMinSpeed(double minSpeed) {
        this.minSpeed = minSpeed;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public long getStarted() {
        return started;
    }

    public void setStarted(long started) {
        this.started = started;
    }

    public long getEnded() {
        return ended;
    }

    public void setEnded(long ended) {
        this.ended = ended;
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
}
