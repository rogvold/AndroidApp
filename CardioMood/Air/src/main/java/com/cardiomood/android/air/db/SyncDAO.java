package com.cardiomood.android.air.db;

import com.cardiomood.android.air.db.entity.SyncEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by antondanhsin on 09/10/14.
 */
public class SyncDAO<T extends SyncEntity, ID> extends BaseDaoImpl<T, ID> {

    protected SyncDAO(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public T findBySyncId(String syncId) throws SQLException {
        return queryForFirst(
                queryBuilder().where().eq("sync_id", syncId).prepare()
        );
    }

}
