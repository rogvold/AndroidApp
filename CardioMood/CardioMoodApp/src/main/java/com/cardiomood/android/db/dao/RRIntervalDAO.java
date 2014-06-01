package com.cardiomood.android.db.dao;

import com.cardiomood.android.db.entity.RRIntervalEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * Created by danon on 27.05.2014.
 */
public class RRIntervalDAO extends BaseDaoImpl<RRIntervalEntity, Long> {

    public RRIntervalDAO(Class<RRIntervalEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public RRIntervalDAO(ConnectionSource connectionSource, Class<RRIntervalEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public RRIntervalDAO(ConnectionSource connectionSource, DatabaseTableConfig<RRIntervalEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }
}
