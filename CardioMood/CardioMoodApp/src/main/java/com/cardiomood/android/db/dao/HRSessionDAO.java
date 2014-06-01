package com.cardiomood.android.db.dao;

import com.cardiomood.android.db.entity.HRSessionEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

import java.sql.SQLException;

/**
 * Created by danon on 27.05.2014.
 */
public class HRSessionDAO extends BaseDaoImpl<HRSessionEntity, Long> {

    public HRSessionDAO(Class<HRSessionEntity> dataClass) throws SQLException {
        super(dataClass);
    }

    public HRSessionDAO(ConnectionSource connectionSource, Class<HRSessionEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public HRSessionDAO(ConnectionSource connectionSource, DatabaseTableConfig<HRSessionEntity> tableConfig) throws SQLException {
        super(connectionSource, tableConfig);
    }
}
