package com.cardiomood.android.air.db.entity;

import com.cardiomood.android.air.db.annotations.ParseField;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by antondanhsin on 08/10/14.
 */
@DatabaseTable(tableName = "air_session_points")
public class DataPointEntity extends SyncEntity implements Serializable {

    @DatabaseField(columnName = "session_id")
    private long sessionId;

    @DatabaseField(columnName = "sync_session_id")
    @ParseField(name = "sessionId")
    private String syncSessionId;

    @DatabaseField(columnName = "t")
    @ParseField(name = "t")
    private long t;

    @DatabaseField(columnName = "lat")
    @ParseField(name = "lat")
    private double latitude;

    @DatabaseField(columnName = "lon")
    @ParseField(name = "lon")
    private double longitude;

    @DatabaseField(columnName = "alt")
    @ParseField(name = "alt")
    private Double altitude;

    @DatabaseField(columnName = "bea")
    @ParseField(name = "bea")
    private Float bearing;

    @DatabaseField(columnName = "vel")
    @ParseField(name = "vel")
    private Float velocity;

    @DatabaseField(columnName = "acc")
    @ParseField(name = "acc")
    private Float accuracy;

    @DatabaseField(columnName = "hr")
    @ParseField(name = "hr")
    private Integer HR;

    @DatabaseField(columnName = "stress")
    @ParseField(name = "stress")
    private Integer stress;


    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSyncSessionId() {
        return syncSessionId;
    }

    public void setSyncSessionId(String syncSessionId) {
        this.syncSessionId = syncSessionId;
    }

    public long getT() {
        return t;
    }

    public void setT(long t) {
        this.t = t;
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

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Float getBearing() {
        return bearing;
    }

    public void setBearing(Float bearing) {
        this.bearing = bearing;
    }

    public Float getVelocity() {
        return velocity;
    }

    public void setVelocity(Float velocity) {
        this.velocity = velocity;
    }

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Float accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getHR() {
        return HR;
    }

    public void setHR(Integer HR) {
        this.HR = HR;
    }

    public Integer getStress() {
        return stress;
    }

    public void setStress(Integer stress) {
        this.stress = stress;
    }
}
