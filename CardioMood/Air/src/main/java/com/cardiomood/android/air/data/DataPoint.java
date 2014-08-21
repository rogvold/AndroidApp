package com.cardiomood.android.air.data;

import java.io.Serializable;

/**
 * Created by danon on 18.08.2014.
 */
public class DataPoint implements Serializable {
    private long t;
    private double lat;
    private double lon;
    private Double alt;
    private Float bea;
    private Float vel;
    private Float acc;
    private Integer HR;
    private Integer stress;

    public long getT() {
        return t;
    }

    public void setT(long t) {
        this.t = t;
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

    public Float getBea() {
        return bea;
    }

    public void setBea(Float bea) {
        this.bea = bea;
    }

    public Float getVel() {
        return vel;
    }

    public void setVel(Float vel) {
        this.vel = vel;
    }

    public Float getAcc() {
        return acc;
    }

    public void setAcc(Float acc) {
        this.acc = acc;
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
