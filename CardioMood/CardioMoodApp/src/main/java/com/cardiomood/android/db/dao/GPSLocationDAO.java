package com.cardiomood.android.db.dao;

import com.cardiomood.android.db.entity.GPSLocationEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * Created by danon on 28.05.2014.
 */
public class GPSLocationDAO extends BaseDaoImpl<GPSLocationEntity, Long> {

    public GPSLocationDAO(Class<GPSLocationEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public GPSLocationDAO(ConnectionSource connectionSource, Class<GPSLocationEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public GPSLocationDAO(ConnectionSource connectionSource, DatabaseTableConfig<GPSLocationEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }
}
