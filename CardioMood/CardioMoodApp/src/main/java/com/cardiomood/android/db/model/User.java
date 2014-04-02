package com.cardiomood.android.db.model;

import android.database.Cursor;

import com.cardiomood.android.db.HeartRateDBContract;

/**
 * Created by danon on 09.03.14.
 */
public class User extends Entity implements HeartRateDBContract.Users {

    private Long externalId;
    private String email;
    private String password;
    private UserStatus status;

    public User() {
        status = UserStatus.NEW;
    }

    public User(String email, UserStatus status) {
        this.email = email;
        this.status = status;
    }

    public User(Long externalId, String email, UserStatus status) {
        this.externalId = externalId;
        this.email = email;
        this.status = status;
    }

    public User(Cursor cursor) {
        super(cursor);
        this.email = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_EMAIL));
        this.password = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_PASSWORD));
        this.externalId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_EXTERNAL_ID));
        if (externalId == 0L)
            externalId = null;
        this.status = UserStatus.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_STATUS)));
    }

    public Long getExternalId() {
        return externalId;
    }

    public void setExternalId(Long externalId) {
        this.externalId = externalId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
