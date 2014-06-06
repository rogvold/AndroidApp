package com.cardiomood.android.db.entity;

import com.cardiomood.android.db.dao.RRIntervalDAO;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by danon on 27.05.2014.
 */
@DatabaseTable(tableName = "heart_rate_data", daoClass = RRIntervalDAO.class)
public class RRIntervalEntity implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private Long id;
    @DatabaseField(index = true, canBeNull = false, columnName = "session_id")
    private Long sessionId;
    @DatabaseField(columnName = "bpm")
    private int heartBeatsPerMinute;
    @DatabaseField(columnName = "rr_time")
    private double rrTime;
    @DatabaseField(columnName = "time_stamp", dataType = DataType.DATE_LONG)
    private Date timeStamp;

    public RRIntervalEntity() {
        timeStamp = new Date();
    }

    public RRIntervalEntity(int heartBeatsPerMinute, double rrTime) {
        timeStamp = new Date();
        this.rrTime = rrTime;
        this.heartBeatsPerMinute = heartBeatsPerMinute;
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

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
}
