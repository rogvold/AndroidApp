package com.cardiomood.wa.android.db.entity;

import com.j256.ormlite.field.DatabaseField;

import java.util.Date;

/**
 * Created by danon on 19.04.2014.
 */
public class MeasurementEntity {

    @DatabaseField(generatedId = true)
    private long id;
    @DatabaseField(index = true)
    private Long externalId;
    @DatabaseField(index = true, canBeNull = false, foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
    private UserEntity user;
    @DatabaseField(index = true)
    private String name;
    @DatabaseField
    private String description;
    @DatabaseField
    private Date creationDate = new Date();
    @DatabaseField
    private Date startDate;
    @DatabaseField
    private Date endDate;
    @DatabaseField
    private long duration = 0L;
    @DatabaseField
    private Status status = Status.IN_PROGRESS;
    @DatabaseField
    private SourceType sourceType = SourceType.SENSOR;

    public MeasurementEntity() {
        // required by OrmLite!
    }

    public MeasurementEntity(String name, String description, UserEntity user) {
        this.name = name;
        this.description = description;
        this.user = user;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
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

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public String toString() {
        return "MeasurementEntity{" +
                "id=" + id +
                ", externalId=" + externalId +
                ", user=" + user +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", sourceType=" + sourceType +
                '}';
    }

    public static enum Status {
        IN_PROGRESS,
        COMPLETED,
        SYNCHRONIZING,
        SYNCHRONIZED
    }

    public static enum SourceType {
        SENSOR,
        CAMERA,
        EXTERNAL,
        UNKNOWN
    }

}
