package com.cardiomood.sport.android.db.entity;

import android.database.Cursor;
import android.provider.BaseColumns;

import java.io.Serializable;

/**
 * Project: CardioSport
 * User: danon
 * Date: 10.06.13
 * Time: 13:22
 */
public abstract class Entity implements Serializable, BaseColumns {
    private Long id;

    protected Entity() {
    }

    protected Entity(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndex(_ID));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
