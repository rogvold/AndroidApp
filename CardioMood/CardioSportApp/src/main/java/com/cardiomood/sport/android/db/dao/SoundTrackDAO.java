package com.cardiomood.sport.android.db.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.cardiomood.sport.android.db.DBContract;
import com.cardiomood.sport.android.db.entity.SoundTrackEntity;

/**
 * Project: CardioSport
 * User: danon
 * Date: 23.06.13
 * Time: 14:07
 */
public class SoundTrackDAO extends BaseDAO<SoundTrackEntity> implements DBContract.AudioTracks {

    public static final String[] ALL_COLUMNS = new String[] {
            _ID,
            COLUMN_NAME_EXTERNAL_ID,
            COLUMN_NAME_NAME,
            COLUMN_NAME_HASH,
            COLUMN_NAME_FILE_NAME,
            COLUMN_NAME_BPM
    };

    public SoundTrackDAO(Context context) {
        super(context);
    }

    @Override
    public ContentValues getContentValues(SoundTrackEntity item) {
        return null;  // TODO
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String[] getColumnNames() {
        return ALL_COLUMNS;
    }

    @Override
    public SoundTrackEntity loadFromCursor(Cursor cursor) {
        return new SoundTrackEntity(cursor);
    }
}
