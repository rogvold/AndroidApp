package com.cardiomood.android.air.data;

import com.cardiomood.android.air.db.entity.AircraftEntity;
import com.cardiomood.android.air.db.entity.SyncEntity;
import com.parse.ParseClassName;

/**
 * Created by danon on 14.08.2014.
 */
@ParseClassName("Aircraft")
public class Aircraft extends SynchronizableParseObject {

    public String getName() {
        return getString("name");
    }

    public String getAircraftId() {
        return getString("aircraftId");
    }

    public String getAircraftType() {
        return getString("aircraftType");
    }

    public String getCallName() {
        return getString("callName");
    }

    @Override
    public Class<? extends SyncEntity> getEntityClass() {
        return AircraftEntity.class;
    }
}
