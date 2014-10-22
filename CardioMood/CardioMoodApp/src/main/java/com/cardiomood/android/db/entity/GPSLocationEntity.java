package com.cardiomood.android.db.entity;

import android.location.Location;

import com.cardiomood.android.db.dao.GPSLocationDAO;
import com.cardiomood.data.json.JsonGPS;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by danon on 27.05.2014.
 */
@DatabaseTable(tableName = "gps_data", daoClass = GPSLocationDAO.class)
public class GPSLocationEntity extends SessionDataItem<JsonGPS> {

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
        setTimestamp(System.currentTimeMillis());
    }

    public GPSLocationEntity(Location location) {
        setTimestamp(System.currentTimeMillis());
        lat = location.getLatitude();
        lon = location.getLongitude();
        if (location.hasAltitude())
            alt = location.getAltitude();
        if (location.hasSpeed())
            speed = (double) location.getSpeed();
        if (location.hasBearing())
            bearing = (double) location.getBearing();
        if (location.hasAccuracy())
            accuracy = (double) location.getAccuracy();
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

    @Override
    public JsonGPS toJsonDataItem() {
        JsonGPS json = new JsonGPS();
        json.setLat(lat);
        json.setLon(lon);
        json.setAlt(alt);
        json.setSpeed(speed);
        json.setBearing(bearing);
        json.setAccuracy(accuracy);
        return json;
    }



}
