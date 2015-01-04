package com.cardiomood.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cardiomood.android.db.entity.CardioItemDAO;
import com.cardiomood.android.db.entity.CardioItemEntity;
import com.cardiomood.android.db.entity.LocationDAO;
import com.cardiomood.android.db.entity.LocationEntity;
import com.cardiomood.android.db.entity.SessionDAO;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.tools.PreferenceHelper;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by danon on 27.05.2014.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static final String DATABASE_NAME   = "heart_rate.db";
    private static final int DATABASE_VERSION   = 30;

    private Context mContext;
    private PreferenceHelper pHelper;
    private SQLiteDatabase database;

    private SessionDAO sessionDao = null;
    private CardioItemDAO cardioItemDao = null;
    private LocationDAO locationDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        pHelper = new PreferenceHelper(context);
        pHelper.setPersistent(true);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        this.database = db;
    }


    @Override
    public synchronized void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        Log.d(TAG, "onCreate()");
        try {
            TableUtils.createTable(connectionSource, SessionEntity.class);
            TableUtils.createTable(connectionSource, LocationEntity.class);
            TableUtils.createTable(connectionSource, CardioItemEntity.class);

            // pre-create DAO classes
            getSessionDao();
            getLocationDao();
            getCardioItemDao();
        } catch (SQLException ex) {
            Log.e(TAG, "onCreate(): failed to create tables", ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        // Upgrade or downgrade the database schema
        new DBUpgradeHelper(mContext, database, connectionSource)
                .performUpgrade(oldVersion, newVersion);
    }

    public SQLiteDatabase getDatabase() {
        if (database != null && database.isOpen()) {
            return database;
        } else return null;
    }

    public Context getContext() {
        return mContext;
    }

    public synchronized SessionDAO getSessionDao() throws SQLException {
        if (sessionDao == null) {
            sessionDao = new SessionDAO(getConnectionSource(), SessionEntity.class);
            sessionDao.setObjectCache(true);
        }
        return sessionDao;
    }

    public synchronized LocationDAO getLocationDao() throws SQLException {
        if (locationDao == null) {
            locationDao = new LocationDAO(getConnectionSource(), LocationEntity.class);
        }
        return locationDao;
    }

    public synchronized CardioItemDAO getCardioItemDao() throws SQLException {
        if (cardioItemDao == null) {
            cardioItemDao = new CardioItemDAO(getConnectionSource(), CardioItemEntity.class);
        }
        return cardioItemDao;
    }
}
