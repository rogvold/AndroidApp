package com.cardiomood.android.air.db.entity;

import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.sync.annotations.ParseClass;
import com.cardiomood.android.sync.annotations.ParseField;
import com.cardiomood.android.sync.ormlite.SyncEntity;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by antondanhsin on 08/10/14.
 */
@DatabaseTable(tableName = "air_sessions", daoClass = AirSessionDAO.class) @ParseClass(name = "AirSession")
public class AirSessionEntity extends SyncEntity implements Serializable {

    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

    @DatabaseField(columnName = "sync_user_id")
    @ParseField(name = "userId")
    private String syncUserId;

    @DatabaseField(columnName = "sync_aircraft_id")
    @ParseField(name = "aircraftId")
    private String syncAircraftId;

    @DatabaseField(columnName = "original_session_id")
    @ParseField(name = "originalSessionId")
    private String originalSessionId;

    @DatabaseField(columnName = "end_timestamp")
    @ParseField(name = "endDate")
    private Long endDate;

    @DatabaseField(columnName = "name")
    @ParseField(name = "name")
    private String name;

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

    public String getSyncAircraftId() {
        return syncAircraftId;
    }

    public void setSyncAircraftId(String syncAircraftId) {
        this.syncAircraftId = syncAircraftId;
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
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

}
