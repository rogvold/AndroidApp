package com.cardiomood.android.db.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.cardiomood.android.CardioMoodApplication;
import com.cardiomood.android.db.model.Entity;

import java.util.Collection;

public abstract class BaseDAO<T extends Entity> implements BaseColumns {
    private static final String TAG = "CardioMood.BaseDAO";

    private final SQLiteDatabase database;

    public BaseDAO() {
        database = CardioMoodApplication.getOpenDatabase();
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public boolean exists(long id) {
        return findById(id) != null;
    }

    public void delete(long id) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                int k = db.delete(getTableName(), _ID + " = ?", new String[]{String.valueOf(id)});
                Log.d(TAG, "delete(id): id=" + id + " >> " + k + " rows deleted");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public T merge(T item) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                T entity = null;
                if (item.getId() != null && exists(item.getId()))
                    entity = update(item);
                else entity = insert(item);
                db.setTransactionSuccessful();
                return entity;
            } finally {
                db.endTransaction();
            }
        }
    }

    public T findById(long id) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            Cursor cursor = db.query(true, getTableName(), getColumnNames(), _ID + " = ?", new String[] {String.valueOf(id)}, null, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    return loadFromCursor(cursor);
                } else return null;
            } finally {
                cursor.close();
            }
        }
    }

    public T insert(T item) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                long id = db.insert(getTableName(), null,  getContentValues(item));
                if (id >= 0) {
                    item.setId(id);
                    Log.d(TAG, "database: entity inserted with id = " + id);
                    db.setTransactionSuccessful();
                    return item;
                } else return null;
            } finally {
                db.endTransaction();
            }
        }
    }

    public Collection<T> bulkInsert(Collection<T> items) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                for(T item: items) {
                    long id = db.insert(getTableName(), null, getContentValues(item));
                    if (id < 0)
                        throw new RuntimeException("insert failed for item: " + item);
                    item.setId(id);
                    Log.d(TAG, "database: entity inserted with id = " + id);
                }
                db.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.e(TAG, "bulkUpdate() error: ", ex);
                return null;
            } finally {
                db.endTransaction();
            }
            return items;
        }
    }

    public T update(T item) {
        T[] t = bulkUpdate(item);
        if (t == null)
            return null;
        return t[0];
    }

    public T[] bulkUpdate(T... items) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                for(T item: items) {
                    int r = db.update(getTableName(), getContentValues(item), _ID + " = ?", new String[] {String.valueOf(item.getId())});
                    if (r == -1)
                        throw new RuntimeException("update failed for item: " + item);
                }
                db.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.e(TAG, "bulkUpdate() error: ", ex);
                return null;
            } finally {
                db.endTransaction();
            }
            return items;
        }
    }

    public Collection<T> bulkUpdate(Collection<T> items) {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            db.beginTransaction();
            try {
                for(T item: items) {
                    int r = db.update(getTableName(), getContentValues(item), _ID + " = ?", new String[] {String.valueOf(item.getId())});
                    if (r == -1)
                        throw new RuntimeException("update failed for item: " + item);
                }
                db.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.e(TAG, "bulkUpdate() error: ", ex);
                return null;
            } finally {
                db.endTransaction();
            }
            return items;
        }
    }

    public abstract ContentValues getContentValues(T item);
    public abstract String getTableName();
    public abstract String[] getColumnNames();
    public abstract T loadFromCursor(Cursor cursor);
}