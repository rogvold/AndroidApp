package com.cardiomood.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;

import java.util.ArrayList;
import java.util.List;

public class DBUpgradeHelper {

    public static final String TAG = DBUpgradeHelper.class.getSimpleName();

    private PreferenceHelper preferenceHelper;
    private SQLiteDatabase database;

    public DBUpgradeHelper(Context context, SQLiteDatabase db) {
        preferenceHelper = new PreferenceHelper(context, true);
        database = db;

        addUpgrader(29,  30, new DBUpgrader.Callback() {

            @Override
            public void onUpgrade(SQLiteDatabase db) {
                try {
                    db.execSQL("DROP TABLE heart_rate_data");
                    db.execSQL("DROP TABLE gps_data");
                    db.execSQL("DROP TABLE sessions");
                    db.execSQL("DROP TABLE users");
                    db.execSQL("DROP TABLE users");
                    preferenceHelper.remove(ConfigurationConstants.USER_ID);
                    preferenceHelper.remove(ConfigurationConstants.USER_ABOUT_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_DEPARTMENT_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_DESCRIPTION_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_DIAGNOSIS_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_HEIGHT_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_SEX_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_PASSWORD_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_STATUS_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_ACCESS_TOKEN_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_WEIGHT_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_BIRTH_DATE_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_FIRST_NAME_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_FACEBOOK_ID);
                    preferenceHelper.remove(ConfigurationConstants.USER_LAST_NAME_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_PHONE_NUMBER_KEY);
                    preferenceHelper.remove(ConfigurationConstants.USER_EXTERNAL_ID);
                    preferenceHelper.remove(ConfigurationConstants.USER_LOGGED_IN);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
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

        public int getNewVersion() {
            return newVersion;
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