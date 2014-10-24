package com.cardiomood.android.air.db;

import com.cardiomood.android.air.db.entity.DataPointEntity;
import com.cardiomood.android.sync.ormlite.SyncDAO;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by antondanhsin on 12/10/14.
 */
public class DataPointDAO extends SyncDAO<DataPointEntity, Long> {

    public DataPointDAO(ConnectionSource connectionSource, Class<DataPointEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }
}
