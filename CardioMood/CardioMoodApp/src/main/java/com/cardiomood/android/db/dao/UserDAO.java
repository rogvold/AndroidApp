package com.cardiomood.android.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.cardiomood.android.db.HeartRateDBContract;
import com.cardiomood.android.db.model.User;

/**
 * Created by danon on 09.03.14.
 */
public class UserDAO extends BaseDAO<User> implements HeartRateDBContract.Users {

    public static final String[] COLUMN_NAMES = new String[] {
        _ID, COLUMN_NAME_EXTERNAL_ID, COLUMN_NAME_EMAIL, COLUMN_NAME_PASSWORD, COLUMN_NAME_STATUS
    };

    @Override
    public ContentValues getContentValues(User item) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, item.getId());
        cv.put(COLUMN_NAME_EMAIL, item.getEmail());
        cv.put(COLUMN_NAME_PASSWORD, item.getPassword());
        cv.put(COLUMN_NAME_EXTERNAL_ID, item.getExternalId());
        cv.put(COLUMN_NAME_STATUS, String.valueOf(item.getStatus()));
        return cv;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String[] getColumnNames() {
        return COLUMN_NAMES;
    }

    @Override
    public User loadFromCursor(Cursor cursor) {
        User user = new User(cursor);
        return user;
    }

    public User findByExternalId(Long externalId) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            Cursor cursor = null;
            if (externalId != null) {
                cursor = db.query(
                        getTableName(),
                        getColumnNames(),
                        COLUMN_NAME_EXTERNAL_ID + "=?",
                        new String[]{String.valueOf(externalId)},
                        null,
                        null,
                        null);
            } else {
                cursor = db.query(
                        getTableName(),
                        getColumnNames(),
                        COLUMN_NAME_EXTERNAL_ID + " is null",
                        new String[]{String.valueOf(externalId)},
                        null,
                        null,
                        null);
            }
            try {
                if (cursor.moveToFirst()) {
                    return loadFromCursor(cursor);
                } else return null;
            } finally {
                cursor.close();
            }

        }
    }
}
