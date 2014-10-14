package com.cardiomood.android.air.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.AircraftEntity;
import com.cardiomood.android.air.db.entity.DataPointEntity;
import com.cardiomood.android.air.db.entity.SyncEntity;
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

    private static final String DATABASE_NAME   = "cardiomood_air.db";
    private static final int DATABASE_VERSION   = 1;

    private Context mContext;
    private PreferenceHelper pHelper;
    private SQLiteDatabase database;

    private AircraftDAO aircraftDao = null;
    private AirSessionDAO airSessionDao = null;

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
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        Log.d(TAG, "onCreate()");
        try {
            TableUtils.createTable(connectionSource, AircraftEntity.class);
            TableUtils.createTable(connectionSource, AirSessionEntity.class);
            TableUtils.createTable(connectionSource, DataPointEntity.class);

            // create DAO clsses
            getAircraftDao();
            getAirSessionDao();
        } catch (SQLException ex) {
            Log.e(TAG, "onCreate(): failed to create tables", ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        // Upgrade or downgrade the database schema
    }

    public SQLiteDatabase getDatabase() {
        if (database != null && database.isOpen()) {
            return database;
        } else return null;
    }

    public Context getContext() {
        return mContext;
    }

    public AircraftDAO getAircraftDao() throws SQLException {
        if (aircraftDao == null) {
            aircraftDao = new AircraftDAO(getConnectionSource(), AircraftEntity.class);
        }

        return aircraftDao;

    }

    public AirSessionDAO getAirSessionDao() throws SQLException {
        if (airSessionDao == null) {
            airSessionDao = new AirSessionDAO(getConnectionSource(), AirSessionEntity.class);
        }

        return airSessionDao;
    }

    public <T extends SyncEntity> SyncDAO<T, Long> getDaoForClass(Class<T> clazz) throws SQLException {
        if (AircraftEntity.class.equals(clazz))
            return (SyncDAO<T, Long>) getAircraftDao();
        if (AirSessionEntity.class.equals(clazz))
            return (SyncDAO<T, Long>) getAirSessionDao();

        // not supported class!!!
        throw new IllegalArgumentException("Class " + clazz + " is not supported!");
    }

}
