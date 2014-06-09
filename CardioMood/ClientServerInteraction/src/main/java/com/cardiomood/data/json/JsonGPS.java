package com.cardiomood.data.json;

import com.google.gson.Gson;

/**
 *
 * @author sabir
 */
public class JsonGPS {

    private static final Gson GSON = new Gson();
    
    private double lat;
    private double lon;
    private Double alt;
    
    private Double speed;
    private Double accuracy;
    private Double bearing;

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

    public String toString() {
        return toJson(this);
    }

    public static String toJson(JsonGPS gps) {
        return GSON.toJson(gps);
    }

    public static JsonGPS fromJson(String json) {
        return GSON.fromJson(json, JsonGPS.class);
    }
}