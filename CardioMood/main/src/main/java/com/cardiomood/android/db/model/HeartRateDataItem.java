package com.cardiomood.android.db.model;

import android.database.Cursor;

import com.cardiomood.android.db.HeartRateDBContract;

import java.util.Date;

public class HeartRateDataItem extends Entity implements HeartRateDBContract.HeartRateData {
	private static final long serialVersionUID = -6334894776709518038L;

	private Long sessionId;
	private int heartBeatsPerMinute;
	private double rrTime;
	private Date timeStamp;
	
	public HeartRateDataItem() {
		this(0, 0);
	}
	
	public HeartRateDataItem(int heartBeatsPerMinute, int rrTime) {
		this.heartBeatsPerMinute = heartBeatsPerMinute;
		this.rrTime = rrTime;
		this.timeStamp = new Date();
	}

    public HeartRateDataItem(Cursor cursor) {
        super(cursor);
        heartBeatsPerMinute = cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_BPM));
        rrTime = cursor.getDouble(cursor.getColumnIndex(COLUMN_NAME_RR_TIME));
        sessionId = cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_SESSION_ID));
        if (sessionId <= 0)
            sessionId = null;
    }
	
	public int getHeartBeatsPerMinute() {
		return heartBeatsPerMinute;
	}
	
	public void setHeartBeatsPerMinute(int heartBeatsPerMinute) {
		this.heartBeatsPerMinute = heartBeatsPerMinute;
	}
	
	public double getRrTime() {
		return rrTime;
	}
	
	public void setRrTime(double rrTime) {
		this.rrTime = rrTime;
	}
	
	public Date getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public Long getSessionId() {
		return sessionId;
	}
	
	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}
}
