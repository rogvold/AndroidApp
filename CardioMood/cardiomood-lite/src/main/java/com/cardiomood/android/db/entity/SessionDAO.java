package com.cardiomood.android.db.entity;

import com.cardiomood.android.sync.ormlite.SyncDAO;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by antondanhsin on 12/10/14.
 */
public class SessionDAO extends SyncDAO<SessionEntity, Long> {

    public SessionDAO(ConnectionSource connectionSource, Class<SessionEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }
}
