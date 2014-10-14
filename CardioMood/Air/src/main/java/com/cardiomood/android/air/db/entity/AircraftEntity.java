package com.cardiomood.android.air.db.entity;

import com.cardiomood.android.air.db.annotations.ParseClass;
import com.cardiomood.android.air.db.annotations.ParseField;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

/**
 * Created by antondanhsin on 08/10/14.
 */
@DatabaseTable(tableName = "aircrafts") @ParseClass(name = "Aircraft")
public class AircraftEntity extends SyncEntity implements Serializable {

    @DatabaseField(columnName = "aircraft_id")
    @ParseField(name = "aircraftId")
    private String aircraftId;

    @ParseField(name = "name")
    @DatabaseField(columnName = "name")
    private String name;

    @DatabaseField(columnName = "call_name")
    @ParseField(name = "callName")
    private String callName;

    @DatabaseField(columnName = "aircraft_type")
    @ParseField(name = "aircraftType")
    private String aircraftType;

    public String getAircraftId() {
        return aircraftId;
    }

    public void setAircraftId(String aircraftId) {
        this.aircraftId = aircraftId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCallName() {
        return callName;
    }

    public void setCallName(String callName) {
        this.callName = callName;
    }

    public String getAircraftType() {
        return aircraftType;
    }

    public void setAircraftType(String aircraftType) {
        this.aircraftType = aircraftType;
    }
}
