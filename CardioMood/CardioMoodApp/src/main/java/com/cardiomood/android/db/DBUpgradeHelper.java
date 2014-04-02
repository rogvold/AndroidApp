package com.cardiomood.android.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cardiomood.android.db.model.SessionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danon on 01.04.2014.
 */
public class DBUpgradeHelper {

    public static final String TAG = DBUpgradeHelper.class.getSimpleName();

    private SQLiteDatabase database;

    public DBUpgradeHelper(SQLiteDatabase database) {
        this.database = database;

        addUpgrader(19, 21, new DBUpgrader.Callback() {

            @Override
            public void onUpgrade(SQLiteDatabase db) {
                try {
                    db.execSQL(HeartRateDBContract.SQL.CREATE_TABLE_USERS);
                    String ADD_COLUMN_EXTERNAL_ID = "ALTER TABLE " + HeartRateDBContract.Sessions.TABLE_NAME
                            + " ADD COLUMN " + HeartRateDBContract.Sessions.COLUMN_NAME_EXTERNAL_ID + " INTEGER";
                    db.execSQL(ADD_COLUMN_EXTERNAL_ID);
                } catch (Exception e) {
                    Log.d(TAG, "onUpgrade() exception", e);
                }
            }
        });

        addUpgrader(21, 22, new DBUpgrader.Callback() {

            @Override
            public void onUpgrade(SQLiteDatabase db) {
                try {
                    db.execSQL("UPDATE " + HeartRateDBContract.Sessions.TABLE_NAME + " SET " + HeartRateDBContract.Sessions.COLUMN_NAME_STATUS + " = '" + SessionStatus.COMPLETED + "'");
                    db.execSQL("DELETE FROM " + HeartRateDBContract.Sessions.TABLE_NAME + " WHERE " + HeartRateDBContract.Sessions._ID + " NOT IN ("
                            + "SELECT " + HeartRateDBContract.HeartRateData.COLUMN_NAME_SESSION_ID
                            + " FROM " + HeartRateDBContract.HeartRateData.TABLE_NAME
                            + " GROUP BY " + HeartRateDBContract.HeartRateData.COLUMN_NAME_SESSION_ID
                            +")");
                } catch (Exception e) {
                    Log.d(TAG, "onUpgrade() exception", e);
                }
            }
        });

        addUpgrader(22, 23, new DBUpgrader.Callback() {

            @Override
            public void onUpgrade(SQLiteDatabase db) {
                try {
                    String ADD_COLUMN_PASSWORD = "ALTER TABLE " + HeartRateDBContract.Users.TABLE_NAME
                            + " ADD COLUMN " + HeartRateDBContract.Users.COLUMN_NAME_PASSWORD + " TEXT";
                    db.execSQL(ADD_COLUMN_PASSWORD);
                } catch (Exception e) {
                    Log.d(TAG, "onUpgrade() exception", e);
                }
            }
        });
    }

    public void addUpgrader(int oldVersion, int newVersion, DBUpgrader.Callback callback) {
        upgradeChain.add(new DBUpgrader(database, oldVersion, newVersion).onUpgrade(callback));
    }

    public void performUpgrade(int oldVersion, int newVersion) {
        for (DBUpgrader upgrader: upgradeChain) {
            if (upgrader.getNewVersion() <= oldVersion)
                continue;
            if (upgrader.getNewVersion() > newVersion)
                break;
            upgrader.upgrade();
        }
    }

    private List<DBUpgrader> upgradeChain = new ArrayList<DBUpgrader>();

    private static class DBUpgrader {
        private SQLiteDatabase database;
        private int oldVersion;
        private int newVersion;
        private Callback callback;

        private DBUpgrader(SQLiteDatabase database, int oldVersion, int newVersion) {
            this.database = database;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }

        public int getOldVersion() {
            return oldVersion;
        }

        public void setOldVersion(int oldVersion) {
            this.oldVersion = oldVersion;
        }

        public int getNewVersion() {
            return newVersion;
        }

        public void setNewVersion(int newVersion) {
            this.newVersion = newVersion;
        }

        public DBUpgrader onUpgrade(Callback callback) {
            this.callback = callback;
            return this;
        }

        public void upgrade() {
            if (this.callback != null)
                this.callback.onUpgrade(database);
        }

        private interface Callback {
            void onUpgrade(SQLiteDatabase db);
        }
    }
}
