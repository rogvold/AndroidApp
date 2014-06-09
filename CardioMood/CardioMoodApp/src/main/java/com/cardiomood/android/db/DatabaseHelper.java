package com.cardiomood.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.cardiomood.android.db.entity.ContinuousSessionEntity;
import com.cardiomood.android.db.entity.GPSLocationEntity;
import com.cardiomood.android.db.entity.RRIntervalEntity;
import com.cardiomood.android.db.entity.UserEntity;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.sql.SQLException;

/**
 * Created by danon on 27.05.2014.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper implements HeartRateDBContract {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private Context mContext;
    private PreferenceHelper pHelper;
    private SQLiteDatabase database;

    public DatabaseHelper(Context context) {
        super(context, HeartRateDBContract.DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        pHelper = new PreferenceHelper(context);
        pHelper.setPersistent(true);
    }

    private void backupDB() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dbFile = mContext.getDatabasePath(DATABASE_NAME);
            File outFile = new File(Environment.getExternalStorageDirectory(), "cardiomood.sqlite.db");
            try {
                FileUtils.copyFile(dbFile, outFile);
            } catch (Exception ex) {
                Log.e("HeartRateDBHelper", "backupDB() failed", ex);
            }
        }
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
            TableUtils.createTable(connectionSource, UserEntity.class);
            TableUtils.createTable(connectionSource, ContinuousSessionEntity.class);
            TableUtils.createTable(connectionSource, RRIntervalEntity.class);
            TableUtils.createTable(connectionSource, GPSLocationEntity.class);
        } catch (SQLException ex) {
            Log.e(TAG, "onCreate(): failed to create tables", ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        // Upgrade or downgrade the database schema
        new DBUpgradeHelper(this).performUpgrade(oldVersion, newVersion);

        if (oldVersion < 26) {
            // due to update in ProfileFragment
            pHelper.remove(ConfigurationConstants.USER_BIRTH_DATE_KEY);
            pHelper.remove(ConfigurationConstants.USER_SEX_KEY);
            pHelper.remove(ConfigurationConstants.USER_WEIGHT_KEY);
            pHelper.remove(ConfigurationConstants.USER_HEIGHT_KEY);
        }
    }

    public SQLiteDatabase getDatabase() {
        if (database != null && database.isOpen()) {
            return database;
        } else return null;
    }

    public Context getmContext() {
        return mContext;
    }
}
