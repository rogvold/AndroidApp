package com.cardiomood.heartrate.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cardiomood.heartrate.android.R;
import com.cardiomood.heartrate.android.db.entity.MeasurementEntity;
import com.cardiomood.heartrate.android.db.entity.UserEntity;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by danon on 19.04.2014.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "cardiomood.heart_rate.db";
    private static final int DATABASE_VERSION = 1;

    private Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        Log.d(TAG, "onCreate()");
        try {
            TableUtils.createTable(connectionSource, UserEntity.class);
            TableUtils.createTable(connectionSource, MeasurementEntity.class);
        } catch (SQLException ex) {
            Log.e(TAG, "onCreate(): failed to create tables", ex);
        }

        RuntimeExceptionDao<UserEntity, Long> dao = getRuntimeExceptionDao(UserEntity.class);
        UserEntity user = new UserEntity("_guest_", "_guest_");
        dao.create(user);
        Log.d(TAG, "onCreate(): created guest user -> " + user);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {

    }
}
