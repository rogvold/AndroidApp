package com.cardiomood.android.air.db.entity;

import com.cardiomood.android.air.db.LocationDAO;
import com.cardiomood.android.sync.annotations.ParseField;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by antondanhsin on 20/10/14.
 */
@DatabaseTable(tableName = "gps_items", daoClass = LocationDAO.class)
public class LocationEntity {

    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

    @DatabaseField(index = true, canBeNull = false, columnName = "session_id", foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 2)
    private AirSessionEntity session;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AirSessionEntity getSession() {
        return session;
    }

    public void setSession(AirSessionEntity session) {
        this.session = session;
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
}
