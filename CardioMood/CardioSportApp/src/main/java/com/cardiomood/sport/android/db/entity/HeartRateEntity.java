package com.cardiomood.sport.android.db.entity;

import android.database.Cursor;

import com.cardiomood.sport.android.db.DBContract;
import com.cardiomood.sport.android.tools.Tools;

import java.io.Serializable;

/**
 * Project: CardioSport
 * User: danon
 * Date: 09.06.13
 * Time: 19:15
 */
public class HeartRateEntity extends WorkoutElementEntity implements Serializable, DBContract.HeartRateData {
    private Long timestamp;
    private short[] rr;
    private int energyExpended;
    private short bpm;

    public HeartRateEntity() {
    }

    public HeartRateEntity(Cursor cursor) {
        super(cursor);
        timestamp = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TIMESTAMP));
        rr = Tools.parseArrayOfShort(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_TIMESTAMP)));
        energyExpended = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ENERGY_EXPENDED));
        bpm = cursor.getShort(cursor.getColumnIndex(COLUMN_NAME_BPM));
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public short[] getRr() {
        return rr;
    }

    public void setRr(short[] rr) {
        this.rr = rr;
    }

    public int getEnergyExpended() {
        return energyExpended;
    }

    public void setEnergyExpended(int energyExpended) {
        this.energyExpended = energyExpended;
    }

    public short getBpm() {
        return bpm;
    }

    public void setBpm(short bpm) {
        this.bpm = bpm;
    }
}
