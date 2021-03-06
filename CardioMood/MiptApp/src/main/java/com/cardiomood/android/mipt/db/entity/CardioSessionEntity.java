package com.cardiomood.android.mipt.db.entity;

import com.cardiomood.android.mipt.db.CardioSessionDAO;
import com.cardiomood.android.sync.annotations.ParseClass;
import com.cardiomood.android.sync.annotations.ParseField;
import com.cardiomood.android.sync.ormlite.SyncEntity;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by antondanhsin on 19/10/14.
 */
@DatabaseTable(tableName = "cardio_sessions", daoClass = CardioSessionDAO.class)
@ParseClass(name = "CardioSession")
public class CardioSessionEntity extends SyncEntity {

    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

    @ParseField
    @DatabaseField(columnName = "name")
    private String name;

    @ParseField(name = "userId")
    @DatabaseField(columnName = "sync_user_id")
    private String syncUserId;

    @ParseField
    @DatabaseField(columnName = "start_timestamp")
    private long startTimestamp;

    @ParseField
    @DatabaseField(columnName = "end_timestamp")
    private Long endTimestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public long getEndTimestamp() {
        return endTimestamp == null ? 0L : endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }
}
