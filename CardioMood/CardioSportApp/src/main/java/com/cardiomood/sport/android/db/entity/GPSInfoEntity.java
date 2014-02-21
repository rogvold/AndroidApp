package com.cardiomood.sport.android.db.entity;

import android.database.Cursor;

import com.cardiomood.sport.android.db.DBContract;

import java.io.Serializable;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 19:15
 */
public class GPSInfoEntity extends WorkoutElementEntity implements Serializable, DBContract.GPSInfo {
    private double latitude;
    private double longitude;
    private double speed;
    private double accuracy;
    private double altitude;
    private long timestamp;

    public GPSInfoEntity() {
    }

    public GPSInfoEntity(Cursor cursor) {
        super(cursor);
        latitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_LATITUDE));
        altitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_ALTITUDE));
        longitude = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_LONGITUDE));
        speed = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_SPEED));
        timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TIMESTAMP));
        accuracy = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_ACCURACY));
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
