package com.cardiomood.android.db.model;

import android.annotation.SuppressLint;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HeartRateSession implements Serializable {
	private static final long serialVersionUID = -3730543014307045197L;
	
	public static final long NEW_STATUS = 0;
	public static final long IN_PROGRESS_STATUS = 1;
	public static final long COMPLETED_STATUS = 2;
	public static final long SYNCHRONIZING_STATUS = 3;
	public static final long SYNCRONIZED_STATUS = 4;
	
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm:ss");
	
	private Long id;
	private Long userId;
	private String name;
	private String description;
	private long status;
	private Date dateStarted;
	private Date dateEnded;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
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
	public long getStatus() {
		return status;
	}
	public void setStatus(long status) {
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
		String s = name == null ? "#" + id + " " + DATE_FORMAT.format(getDateStarted())  : name;
		if (getDateStarted() != null && getDateEnded() != null) {
			s += " [" + TIME_FORMAT.format(new Date(getDateEnded().getTime() - getDateStarted().getTime())) + "]";
		}
		if (status == SYNCRONIZED_STATUS) 
			return "[s] " + s;
		else return s;
	}
	
}
