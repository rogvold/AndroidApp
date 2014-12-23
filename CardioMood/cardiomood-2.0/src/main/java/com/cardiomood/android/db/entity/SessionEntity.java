package com.cardiomood.android.db.entity;

import com.cardiomood.android.sync.annotations.ParseClass;
import com.cardiomood.android.sync.annotations.ParseField;
import com.cardiomood.android.sync.ormlite.SyncEntity;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by antondanhsin on 08/10/14.
 */
@DatabaseTable(tableName = "sessions", daoClass = SessionDAO.class) @ParseClass(name = "CardioSession")
public class SessionEntity extends SyncEntity implements Serializable {

    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

    @DatabaseField(columnName = "sync_user_id")
    @ParseField(name = "userId")
    private String syncUserId;

    @DatabaseField(columnName = "original_session_id")
    @ParseField(name = "originalSessionId")
    private String originalSessionId;

    @ParseField
    @DatabaseField(columnName = "start_timestamp")
    private long startTimestamp;

    @ParseField
    @DatabaseField(columnName = "end_timestamp")
    private Long endTimestamp;


    @DatabaseField(columnName = "name")
    @ParseField(name = "name")
    private String name;

    @DatabaseField(columnName = "description")
    @ParseField(name = "description")
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSyncUserId() {
        return syncUserId;
    }

    public void setSyncUserId(String syncUserId) {
        this.syncUserId = syncUserId;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public Long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(Long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalSessionId() {
        return originalSessionId;
    }

    public void setOriginalSessionId(String originalSessionId) {
        this.originalSessionId = originalSessionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
