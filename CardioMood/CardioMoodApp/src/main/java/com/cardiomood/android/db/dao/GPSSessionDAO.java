package com.cardiomood.android.db.dao;

import com.cardiomood.android.db.entity.GPSSessionEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * Created by danon on 27.05.2014.
 */
public class GPSSessionDAO extends BaseDaoImpl<GPSSessionEntity, Long> {

    public GPSSessionDAO(Class<GPSSessionEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public GPSSessionDAO(ConnectionSource connectionSource, Class<GPSSessionEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public GPSSessionDAO(ConnectionSource connectionSource, DatabaseTableConfig<GPSSessionEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }

}
