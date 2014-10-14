package com.cardiomood.android.db.entity;

import com.cardiomood.data.json.CardioDataItem;
import com.j256.ormlite.field.DatabaseField;

import java.io.Serializable;

/**
 * Created by danon on 08.06.2014.
 */
public abstract class SessionDataItem<T> implements Serializable {

    @DatabaseField(generatedId = true, columnName = "_id")
    private Long id;
    @DatabaseField(index = true, canBeNull = false, columnName = "session_id", foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 2)
    private ContinuousSessionEntity session;
    @DatabaseField(canBeNull = false, columnName = "time_stamp", defaultValue = "0")
    private long timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ContinuousSessionEntity getSession() {
        return session;
    }

    public void setSession(ContinuousSessionEntity session) {
        this.session = session;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public CardioDataItem toCardioDataItem() {
        final CardioDataItem item = new CardioDataItem();
        item.setCreationTimestamp(getTimestamp());
        item.setSessionId(session.getExternalId());
        item.setNumber(id);
        item.setDataItem(toJsonDataItem().toString());
        return item;
    }


    public abstract T toJsonDataItem();

}
