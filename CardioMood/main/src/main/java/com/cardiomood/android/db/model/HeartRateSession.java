package com.cardiomood.android.db.model;

import android.annotation.SuppressLint;
import android.database.Cursor;

import com.cardiomood.android.db.HeartRateDBContract;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HeartRateSession extends Entity implements HeartRateDBContract.Sessions {
	private static final long serialVersionUID = -3730543014307045197L;
	
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm:ss");

	private Long userId;
	private String name;
	private String description;
	private SessionStatus status;
	private Date dateStarted;
	private Date dateEnded;

    public HeartRateSession() {
        status = SessionStatus.NEW;
        dateStarted = new Date();
    }

    public HeartRateSession(Cursor cursor) {
        super(cursor);
        userId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_USER_ID));
        if (userId == 0)
            userId = null;
        name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_NAME));
        description = cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DESCRIPTION));
        status = SessionStatus.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_NAME_STATUS)));
        long ts = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DATE_ENDED));
        dateEnded = (ts == 0) ? null : new Date(ts);
        ts = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_DATE_STARTED));
        dateStarted = (ts == 0) ? null : new Date(ts);
    }

    public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public SessionStatus getStatus() {
		return status;
	}
	public void setStatus(SessionStatus status) {
		this.status = status;
	}
	public Date getDateStarted() {
		return dateStarted;
	}
	public void setDateStarted(Date dateStarted) {
		this.dateStarted = dateStarted;
	}
	public Date getDateEnded() {
		return dateEnded;
	}
	public void setDateEnded(Date dateEnded) {
		this.dateEnded = dateEnded;
	}
	
	@Override
	public String toString() {
		String s = name == null ? "Measurement #" + getId() + " " + DATE_FORMAT.format(getDateStarted()) : name;
		if (status == SessionStatus.SYNCHRONIZED)
			return "[s] " + s;
		else return s;
	}
	
}
