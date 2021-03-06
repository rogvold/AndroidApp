package com.cardiomood.android.db.entity;

import com.cardiomood.android.db.dao.ContinuousSessionDAO;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by danon on 27.05.2014.
 */
@DatabaseTable(tableName = "sessions", daoClass = ContinuousSessionDAO.class)
public class ContinuousSessionEntity implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private Long id;
    @DatabaseField(index = true, unique = true, columnName = "external_id")
    private Long externalId;
    @DatabaseField(index = true, columnName = "original_session_id", foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 2)
    private ContinuousSessionEntity originalSession;
    @DatabaseField(index = true, canBeNull = false, columnName = "user_id")
    private Long userId;
    @DatabaseField(columnName = "name")
    private String name;
    @DatabaseField(columnName = "description")
    private String description;
    @DatabaseField(columnName = "status")
    private SessionStatus status;
    @DatabaseField(columnName = "date_started", dataType = DataType.DATE_LONG)
    private Date dateStarted;
    @DatabaseField(columnName = "date_ended", dataType = DataType.DATE_LONG)
    private Date dateEnded;
    @DatabaseField(columnName = "last_modified", defaultValue = "0")
    private long lastModified;
    @DatabaseField(columnName = "data_class_name", defaultValue = "JsonRRInterval")
    private String dataClassName;

    public ContinuousSessionEntity() {
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

    public ContinuousSessionEntity getOriginalSession() {
        return originalSession;
    }

    public void setOriginalSession(ContinuousSessionEntity originalSession) {
        this.originalSession = originalSession;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getDataClassName() {
        return dataClassName;
    }

    public void setDataClassName(String dataClassName) {
        this.dataClassName = dataClassName;
    }
}
