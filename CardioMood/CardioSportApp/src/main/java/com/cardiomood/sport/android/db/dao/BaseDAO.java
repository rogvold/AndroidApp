package com.cardiomood.sport.android.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.cardiomood.sport.android.db.DBHelper;
import com.cardiomood.sport.android.db.entity.Entity;

import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 18:46
 */
public abstract class BaseDAO<T extends Entity> implements BaseColumns {
    private static final Logger log = Logger.getLogger(BaseDAO.class);

    private final Context context;
    private final SQLiteOpenHelper dbHelper;
    private SQLiteDatabase database;

    public BaseDAO(Context context) {
        this.context = context;
        dbHelper = DBHelper.getInstance(context);
    }

    public void open(boolean writable) {
        if (database == null) {
            database = dbHelper.getWritableDatabase();
        } else if (!database.isOpen()) {
            database = null;
            open(true);
        }
    }

    public void close() {
        final SQLiteDatabase db = getDatabase();
        synchronized (db) {
            database = null;
            dbHelper.close();
        }
    }

    public synchronized Context getContext() {
        return context;
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
                if (log.isDebugEnabled()) {
                    log.debug("delete(id): id=" + id + " >> " + k + " rows deleted");
                }
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
                    System.out.println("database: entity inserted with id = " + id + " " + item.getClass().getSimpleName());
                    db.setTransactionSuccessful();
                    return item;
                } else return null;
            } finally {
                db.endTransaction();
            }
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
                log.error("bulkUpdate() error: ", ex);
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
                log.error("bulkUpdate() error: ", ex);
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
