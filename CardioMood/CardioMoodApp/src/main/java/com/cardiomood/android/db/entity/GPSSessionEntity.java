package com.cardiomood.android.db.entity;

import com.cardiomood.android.db.dao.GPSSessionDAO;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by danon on 27.05.2014.
 */
@DatabaseTable(tableName = "gps_sessions", daoClass = GPSSessionDAO.class)
public class GPSSessionEntity implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private Long id;

    @DatabaseField(index = true, unique = true, columnName = "external_id")
    private Long externalId;

    @DatabaseField(index = true, columnName = "user_id")
    private Long userId;

    @DatabaseField(columnName = "status")
    private SessionStatus status;
    @DatabaseField(columnName = "date_started")
    private Date dateStarted;
    @DatabaseField(columnName = "date_ended")
    private Date dateEnded;

    public GPSSessionEntity() {
        status = SessionStatus.NEW;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Date getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(Date dateStarted) {
        this.dateStarted = dateStarted;
    }

    public Date getDateEnded() {
        return dateEnded;
    }

    public void setDateEnded(Date dateEnded) {
        this.dateEnded = dateEnded;
    }
}
