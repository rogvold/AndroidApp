package com.cardiomood.android.db.entity;

import com.cardiomood.android.db.dao.GPSLocationDAO;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by danon on 27.05.2014.
 */
@DatabaseTable(tableName = "gps_data", daoClass = GPSLocationDAO.class)
public class GPSLocationEntity implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private Long id;
    @DatabaseField(index = true, columnName = "session_id")
    private Long sessionId;

    @DatabaseField(columnName = "lat")
    private double lat;
    @DatabaseField(columnName = "lon")
    private double lon;
    @DatabaseField(columnName = "alt")
    private Double alt;

    @DatabaseField(columnName = "speed")
    private Double speed;
    @DatabaseField(columnName = "bearing")
    private Double bearing;
    @DatabaseField(columnName = "accuracy")
    private Double accuracy;

    public GPSLocationEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Double getAlt() {
        return alt;
    }

    public void setAlt(Double alt) {
        this.alt = alt;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Double getBearing() {
        return bearing;
    }

    public void setBearing(Double bearing) {
        this.bearing = bearing;
    }
}
