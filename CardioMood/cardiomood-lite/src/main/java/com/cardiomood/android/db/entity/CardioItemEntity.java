package com.cardiomood.android.db.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by antondanhsin on 20/10/14.
 */
@DatabaseTable(tableName = "cardio_items", daoClass = CardioItemDAO.class)
public class CardioItemEntity {

    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

    @DatabaseField(index = true, canBeNull = false, columnName = "session_id", foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 2)
    private SessionEntity session;

    @DatabaseField(columnName = "t")
    private long t;

    @DatabaseField(columnName = "rr")
    private int rr;

    @DatabaseField(columnName = "bpm")
    private int bpm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SessionEntity getSession() {
        return session;
    }

    public void setSession(SessionEntity session) {
        this.session = session;
    }

    public long getT() {
        return t;
    }

    public void setT(long t) {
        this.t = t;
    }

    public int getRr() {
        return rr;
    }

    public void setRr(int rr) {
        this.rr = rr;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }
}
