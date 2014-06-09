package com.cardiomood.android.db;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cardiomood.android.db.entity.GPSLocationEntity;
import com.cardiomood.android.db.entity.SessionStatus;
import com.cardiomood.android.db.entity.UserEntity;
import com.cardiomood.android.db.entity.UserStatus;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by danon on 01.04.2014.
 */
public class DBUpgradeHelper {

    public static final String TAG = DBUpgradeHelper.class.getSimpleName();

    private SQLiteDatabase database;
    private DatabaseHelper databaseHelper;
    private PreferenceHelper preferenceHelper;

    public DBUpgradeHelper(final DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
        this.database = databaseHelper.getDatabase();
        this.preferenceHelper = new PreferenceHelper(databaseHelper.getmContext(), true);

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

        addUpgrader(23, 24, new DBUpgrader.Callback() {

            @Override
            public void onUpgrade(SQLiteDatabase db) {
                try {
                    String RESET_SESSIONS = "UPDATE " + HeartRateDBContract.Sessions.TABLE_NAME
                            + " SET " + HeartRateDBContract.Sessions.COLUMN_NAME_STATUS + "=?";
                    db.execSQL(RESET_SESSIONS, new String[] {String.valueOf(SessionStatus.COMPLETED)});
                } catch (Exception e) {
                    Log.d(TAG, "onUpgrade() exception", e);
                }
            }
        });
        addUpgrader(24, 25, new DBUpgrader.Callback() {

            @Override
            public void onUpgrade(SQLiteDatabase db) {
                try {
                    String ADD_COLUMN_ORIGINAL_SESSION_ID = "ALTER TABLE " + HeartRateDBContract.Sessions.TABLE_NAME
                            + " ADD COLUMN " + HeartRateDBContract.Sessions.COLUMN_NAME_ORIGINAL_SESSION_ID + " INTEGER";
                    db.execSQL(ADD_COLUMN_ORIGINAL_SESSION_ID);
                } catch (Exception e) {
                    Log.d(TAG, "onUpgrade() exception", e);
                }
            }
        });
        addUpgrader(26, 27, new DBUpgrader.Callback() {

            @Override
            public void onUpgrade(SQLiteDatabase db) {
                try {
                    // update SESSIONS s set s.end_date = s.start_date + (select sum(rrTime) from HEART_RATE_DATA where session_id = s._id);
                    String sql = "UPDATE " + HeartRateDBContract.Sessions.TABLE_NAME + "\n"
                            + "SET " + HeartRateDBContract.Sessions.COLUMN_NAME_DATE_ENDED + " = " + HeartRateDBContract.Sessions.COLUMN_NAME_DATE_STARTED
                            + " + " + "(\n"
                            + "SELECT sum(hr." + HeartRateDBContract.HeartRateData.COLUMN_NAME_RR_TIME + ")\n"
                            + "FROM " + HeartRateDBContract.HeartRateData.TABLE_NAME + " hr\n"
                            + "WHERE hr." + HeartRateDBContract.HeartRateData.COLUMN_NAME_SESSION_ID + " = " + HeartRateDBContract.Sessions.TABLE_NAME + "." + HeartRateDBContract.Sessions._ID
                            + ")" + " WHERE " + HeartRateDBContract.Sessions.COLUMN_NAME_DATE_ENDED + " IS NULL";
                    db.execSQL(sql);
                } catch (Exception e) {
                    Log.d(TAG, "onUpgrade() exception", e);
                }
            }
        });

        addUpgrader(27, 28, new DBUpgrader.Callback() {

            @Override
            public void onUpgrade(SQLiteDatabase db) {
                db.execSQL("ALTER TABLE RENAME TO sessions");
                try {
                    TableUtils.createTable(databaseHelper.getConnectionSource(), GPSLocationEntity.class);
                } catch (Exception e) {
                    Log.d(TAG, "onUpgrade() exception", e);
                }

                String SQL = null;
                try {
                    // +field users.first_name
                    SQL = "ALTER TABLE " + "users"
                            + " ADD COLUMN " + "first_name" + " TEXT";
                    db.execSQL(SQL);

                    // +field users.last_name
                    SQL = "ALTER TABLE " + "users"
                            + " ADD COLUMN " + "last_name" + " TEXT";
                    db.execSQL(SQL);

                    // +field users.weight
                    SQL = "ALTER TABLE " + "users"
                            + " ADD COLUMN " + "weight" + " REAL";
                    db.execSQL(SQL);

                    // +field users.height
                    SQL = "ALTER TABLE " + "users"
                            + " ADD COLUMN " + "height" + " REAL";
                    db.execSQL(SQL);

                    // +field users.phone_number
                    SQL = "ALTER TABLE " + "users"
                            + " ADD COLUMN " + "phone_number" + " TEXT";
                    db.execSQL(SQL);

                    // +field users.birth_date
                    SQL = "ALTER TABLE " + "users"
                            + " ADD COLUMN " + "birth_date" + " INTEGER";
                    db.execSQL(SQL);

                    // +field users.last_modified
                    SQL = "ALTER TABLE " + "users"
                            + " ADD COLUMN " + "last_modified" + " INTEGER DEFAULT 0";
                    db.execSQL(SQL);

                    // +field users.gender
                    SQL = "ALTER TABLE " + "users"
                            + " ADD COLUMN " + "gender" + " TEXT DEFAULT 'UNSPECIFIED'";
                    db.execSQL(SQL);

                    // +field sessions.last_modified
                    SQL = "ALTER TABLE " + "sessions"
                            + " ADD COLUMN " + "last_modified" + " INTEGER DEFAULT 0";
                    db.execSQL(SQL);

                    // +field sessions.last_modified
                    SQL = "ALTER TABLE " + "sessions"
                            + " ADD COLUMN " + "last_modified" + " INTEGER DEFAULT 0";
                    db.execSQL(SQL);

                    // +field sessions.data_class_name
                    SQL = "ALTER TABLE " + "sessions"
                            + " ADD COLUMN " + "data_class_name" + " TEXT DEFAULT 'JsonRRInterval'";
                    db.execSQL(SQL);

                    RuntimeExceptionDao<UserEntity, Long> dao = databaseHelper.getRuntimeExceptionDao(UserEntity.class);
                    if (preferenceHelper.getBoolean(ConfigurationConstants.USER_LOGGED_IN)) {
                        // try to setup user
                        UserEntity user = null;

                        if (preferenceHelper.getLong(ConfigurationConstants.USER_EXTERNAL_ID, -1L) >= 0
                                && !preferenceHelper.getString(ConfigurationConstants.USER_EMAIL_KEY, "").isEmpty()
                                && !preferenceHelper.getString(ConfigurationConstants.USER_PASSWORD_KEY, "").isEmpty()) {

                            String email = preferenceHelper.getString(ConfigurationConstants.USER_EMAIL_KEY);
                            String password = preferenceHelper.getString(ConfigurationConstants.USER_PASSWORD_KEY);
                            long externalId = preferenceHelper.getLong(ConfigurationConstants.USER_EXTERNAL_ID);
                            long id = preferenceHelper.getLong(ConfigurationConstants.USER_ID, 0L);
                            try {
                                List<UserEntity> users = dao.queryBuilder()
                                        .where().eq("email", email).and().eq("password", CommonTools.SHA256(password)).query();
                                if (users.size() == 1) {
                                    user = users.get(0);
                                }
                            } catch (SQLException ex) {
                                Log.w(TAG, "onUpgrade()", ex);
                            }

                            if (user == null) {
                                // try to find by EXTERNAL_ID
                                List<UserEntity> users = dao.queryForEq("external_id", externalId);
                                if (users.size() == 1) {
                                    user = users.get(0);
                                }
                            }

                            if (id >= 0) {
                                user = dao.queryForId(id);
                            }

                            if (user == null) {
                                // user wasn't found => create one
                                user = new UserEntity(externalId, email, UserStatus.NEW);
                                user.setPassword(password);
                            }

                            user.setId(preferenceHelper.getLong(ConfigurationConstants.USER_ID));
                            user.setExternalId(preferenceHelper.getLong(ConfigurationConstants.USER_EXTERNAL_ID));
                            user.setEmail(preferenceHelper.getString(ConfigurationConstants.USER_EMAIL_KEY));
                            user.setPassword(CommonTools.SHA256(preferenceHelper.getString(ConfigurationConstants.USER_PASSWORD_KEY)));
                            user.setFirstName(preferenceHelper.getString(ConfigurationConstants.USER_FIRST_NAME_KEY));
                            user.setLastName(preferenceHelper.getString(ConfigurationConstants.USER_LAST_NAME_KEY));
                            user.setBirthDate(preferenceHelper.getLong(ConfigurationConstants.USER_BIRTH_DATE_KEY));
                            user.setWeight(preferenceHelper.getFloat(ConfigurationConstants.USER_WEIGHT_KEY));
                            user.setHeight(preferenceHelper.getFloat(ConfigurationConstants.USER_HEIGHT_KEY));
                            user.setPhoneNumber(preferenceHelper.getString(ConfigurationConstants.USER_PHONE_NUMBER_KEY));
                            user.setGender(preferenceHelper.getString(ConfigurationConstants.USER_SEX_KEY, "UNSPECIFIED"));
                            user.setLastModified(System.currentTimeMillis());
                            user.setStatus(UserStatus.NEW);

                            dao.createOrUpdate(user);
                        }

                    }
                } catch (Exception e) {
                    Log.d(TAG, "onUpgrade() exception when executing SQL: '" + SQL + "'", e);
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
