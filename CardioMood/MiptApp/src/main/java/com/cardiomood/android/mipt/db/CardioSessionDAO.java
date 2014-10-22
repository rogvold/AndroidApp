package com.cardiomood.android.mipt.db;

import com.cardiomood.android.mipt.db.entity.CardioSessionEntity;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by antondanhsin on 20/10/14.
 */
public class CardioSessionDAO extends SyncDAO<CardioSessionEntity, Long> {

    public CardioSessionDAO(ConnectionSource connectionSource, Class<CardioSessionEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }
}
