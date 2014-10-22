package com.cardiomood.android.db.entity;

import com.cardiomood.android.db.dao.RRIntervalDAO;
import com.cardiomood.data.json.JsonRRInterval;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by danon on 27.05.2014.
 */
@DatabaseTable(tableName = "heart_rate_data", daoClass = RRIntervalDAO.class)
public class RRIntervalEntity extends SessionDataItem<JsonRRInterval> {

    @DatabaseField(columnName = "bpm")
    protected int heartBeatsPerMinute;
    @DatabaseField(columnName = "rr_time")
    protected double rrTime;

    public RRIntervalEntity() {
        setTimestamp(System.currentTimeMillis());
    }

    public RRIntervalEntity(int heartBeatsPerMinute, double rrTime) {
        this.rrTime = rrTime;
        this.heartBeatsPerMinute = heartBeatsPerMinute;
        this.setTimestamp(System.currentTimeMillis());
    }

    public RRIntervalEntity(long timestamp, int heartBeatsPerMinute,  double rrTime) {
        this.rrTime = rrTime;
        this.heartBeatsPerMinute = heartBeatsPerMinute;
        this.setTimestamp(timestamp);
    }

    public int getHeartBeatsPerMinute() {
        return heartBeatsPerMinute;
    }

    public void setHeartBeatsPerMinute(int heartBeatsPerMinute) {
        this.heartBeatsPerMinute = heartBeatsPerMinute;
    }

    public double getRrTime() {
        return rrTime;
    }

    public void setRrTime(double rrTime) {
        this.rrTime = rrTime;
    }

    @Override
    public JsonRRInterval toJsonDataItem() {
        return new JsonRRInterval((int) rrTime);
    }
}
