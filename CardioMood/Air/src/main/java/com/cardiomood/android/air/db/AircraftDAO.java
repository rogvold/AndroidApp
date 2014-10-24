package com.cardiomood.android.air.db;

import com.cardiomood.android.air.db.entity.AircraftEntity;
import com.cardiomood.android.sync.ormlite.SyncDAO;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by antondanhsin on 09/10/14.
 */
public class AircraftDAO extends SyncDAO<AircraftEntity, Long> {

    public AircraftDAO(ConnectionSource connectionSource, Class<AircraftEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

}
