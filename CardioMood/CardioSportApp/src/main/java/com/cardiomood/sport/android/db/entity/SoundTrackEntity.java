package com.cardiomood.sport.android.db.entity;

import android.database.Cursor;

import com.cardiomood.sport.android.db.DBContract;

/**
 * Project: CardioSport
 * User: danon
 * Date: 23.06.13
 * Time: 14:06
 */
public class SoundTrackEntity extends Entity implements DBContract.AudioTracks {

    public SoundTrackEntity() {
    }

    public SoundTrackEntity(Cursor cursor) {
        super(cursor);
    }

}
