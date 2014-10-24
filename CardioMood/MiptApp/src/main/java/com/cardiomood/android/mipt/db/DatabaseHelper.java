package com.cardiomood.android.mipt.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cardiomood.android.mipt.db.entity.CardioItemEntity;
import com.cardiomood.android.mipt.db.entity.CardioSessionEntity;
import com.cardiomood.android.sync.ormlite.SyncDAO;
import com.cardiomood.android.sync.ormlite.SyncDatabaseHelper;
import com.cardiomood.android.sync.ormlite.SyncEntity;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by danon on 27.05.2014.
 */
public class DatabaseHelper extends SyncDatabaseHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static final String DATABASE_NAME   = "cardiomood_air.db";
    private static final int DATABASE_VERSION   = 1;

    private Context mContext;

    private volatile CardioSessionDAO cardioSessionDao = null;
    private volatile CardioItemDAO cardioItemDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }


    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        Log.d(TAG, "onCreate()");
        try {
            TableUtils.createTable(connectionSource, CardioSessionEntity.class);
            TableUtils.createTable(connectionSource, CardioItemEntity.class);

            // create DAO classes
            getCardioSessionDao();
            getCardioItemDao();
        } catch (SQLException ex) {
            Log.e(TAG, "onCreate(): failed to create tables", ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        // Upgrade or downgrade the database schema
    }

    public Context getContext() {
        return mContext;
    }

    public synchronized CardioSessionDAO getCardioSessionDao() throws SQLException {
        if (cardioSessionDao == null) {
            cardioSessionDao = new CardioSessionDAO(getConnectionSource(), CardioSessionEntity.class);
            //cardioSessionDao.setObjectCache(true);
        }
        return cardioSessionDao;
    }

    public synchronized CardioItemDAO getCardioItemDao() throws SQLException {
        if (cardioItemDao == null) {
            cardioItemDao = new CardioItemDAO(getConnectionSource(), CardioItemEntity.class);
            cardioItemDao.setObjectCache(true);
        }
        return cardioItemDao;
    }

    public <T extends SyncEntity> SyncDAO<T, Long> getSyncDao(Class<T> clazz) throws SQLException {
        if (CardioSessionEntity.class.equals(clazz))
            return (SyncDAO<T, Long>) getCardioSessionDao();

        // not supported class!!!
        throw new IllegalArgumentException("Class " + clazz + " is not supported!");
    }

}
