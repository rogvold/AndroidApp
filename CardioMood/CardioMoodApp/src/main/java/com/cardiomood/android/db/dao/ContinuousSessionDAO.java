package com.cardiomood.android.db.dao;

import com.cardiomood.android.db.entity.ContinuousSessionEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * Created by danon on 27.05.2014.
 */
public class ContinuousSessionDAO extends BaseDaoImpl<ContinuousSessionEntity, Long> {

    public ContinuousSessionDAO(Class<ContinuousSessionEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public ContinuousSessionDAO(ConnectionSource connectionSource, Class<ContinuousSessionEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public ContinuousSessionDAO(ConnectionSource connectionSource, DatabaseTableConfig<ContinuousSessionEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }
}
