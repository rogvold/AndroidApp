package com.cardiomood.android.air.db;

import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by antondanhsin on 12/10/14.
 */
public class AirSessionDAO extends SyncDAO<AirSessionEntity, Long> {

    public AirSessionDAO(ConnectionSource connectionSource, Class<AirSessionEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }
}
