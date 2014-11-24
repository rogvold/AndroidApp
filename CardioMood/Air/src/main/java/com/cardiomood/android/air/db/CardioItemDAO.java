package com.cardiomood.android.air.db;

import com.cardiomood.android.air.db.entity.CardioItemEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by antondanhsin on 16/11/14.
 */
public class CardioItemDAO extends BaseDaoImpl<CardioItemEntity, Long> {

    public CardioItemDAO(ConnectionSource connectionSource, Class<CardioItemEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }
}
