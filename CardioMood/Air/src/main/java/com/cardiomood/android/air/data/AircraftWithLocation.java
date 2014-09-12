package com.cardiomood.android.air.data;

/**
 * Created by antondanhsin on 09/09/14.
 */
public class AircraftWithLocation {

    private String sessionId;
    private String userId;
    private String aircraftId;
    private GpsPoint lastPoint;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public void setAircraftId(String aircraftId) {
        this.aircraftId = aircraftId;
    }

    public GpsPoint getLastPoint() {
        return lastPoint;
    }

    public void setLastPoint(GpsPoint lastPoint) {
        this.lastPoint = lastPoint;
    }

    public static class GpsPoint {
        private double lat;
        private double lon;
        private Float alt;
        private Float bea;
        private Float acc;
        private Float vel;
        private long t;

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

        public Float getAlt() {
            return alt;
        }

        public void setAlt(Float alt) {
            this.alt = alt;
        }

        public Float getBea() {
            return bea;
        }

        public void setBea(Float bea) {
            this.bea = bea;
        }

        public Float getAcc() {
            return acc;
        }

        public void setAcc(Float acc) {
            this.acc = acc;
        }

        public Float getVel() {
            return vel;
        }

        public void setVel(Float vel) {
            this.vel = vel;
        }

        public long getT() {
            return t;
        }

        public void setT(long t) {
            this.t = t;
        }
    }

}
