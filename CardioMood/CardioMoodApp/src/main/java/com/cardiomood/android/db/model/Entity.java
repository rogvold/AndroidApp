package com.cardiomood.android.db.model;

import android.database.Cursor;
import android.provider.BaseColumns;

import java.io.Serializable;

/**
 * Created by danshin on 31.10.13.
 */
public abstract class Entity implements Serializable, BaseColumns {
    private Long id;

    protected Entity() {
    }

    protected Entity(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndex(_ID));
        if (id == 0)
            id = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}