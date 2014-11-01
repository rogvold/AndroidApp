package com.cardiomood.android.kolomna.db;

import com.cardiomood.android.kolomna.db.entity.CardioItemEntity;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

/**
 * Created by antondanhsin on 20/10/14.
 */
public class CardioItemDAO extends BaseDaoImpl<CardioItemEntity, Long> {

    public CardioItemDAO(ConnectionSource connectionSource, Class<CardioItemEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }


}
