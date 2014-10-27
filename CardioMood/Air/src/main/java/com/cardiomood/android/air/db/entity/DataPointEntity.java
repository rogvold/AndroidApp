package com.cardiomood.android.air.db.entity;

import com.cardiomood.android.air.db.DataPointDAO;
import com.cardiomood.android.sync.annotations.ParseClass;
import com.cardiomood.android.sync.annotations.ParseField;
import com.cardiomood.android.sync.ormlite.SyncEntity;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by antondanhsin on 08/10/14.
 */
@DatabaseTable(tableName = "air_session_points", daoClass = DataPointDAO.class)
@ParseClass(name = "AirSessionPoint")
public class DataPointEntity extends SyncEntity implements Serializable {

    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

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
    private double lat;

    @DatabaseField(columnName = "lon")
    @ParseField(name = "lon")
    private double lon;

    @DatabaseField(columnName = "alt")
    @ParseField(name = "alt")
    private Double alt;

    @DatabaseField(columnName = "bea")
    @ParseField(name = "bea")
    private Float bea;

    @DatabaseField(columnName = "vel")
    @ParseField(name = "vel")
    private Float vel;

    @DatabaseField(columnName = "acc")
    @ParseField(name = "acc")
    private Float acc;

    @DatabaseField(columnName = "hr")
    @ParseField(name = "HR")
    private Integer HR;

    @DatabaseField(columnName = "stress")
    @ParseField(name = "stress")
    private Integer stress;

    @DatabaseField(columnName = "is_sync")
    private boolean sync;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
        return lat;
    }

    public void setLatitude(double lat) {
        this.lat = lat;
    }

    public double getLongitude() {
        return lon;
    }

    public void setLongitude(double lon) {
        this.lon = lon;
    }

    public Double getAltitude() {
        return alt;
    }

    public void setAltitude(Double alt) {
        this.alt = alt;
    }

    public Float getBearing() {
        return bea;
    }

    public void setBearing(Float bea) {
        this.bea = bea;
    }

    public Float getVelocity() {
        return vel;
    }

    public void setVelocity(Float vel) {
        this.vel = vel;
    }

    public Float getAccuracy() {
        return vel;
    }

    public void setAccuracy(Float accuracy) {
        this.vel = vel;
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

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }
}
