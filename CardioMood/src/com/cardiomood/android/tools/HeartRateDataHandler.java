package com.cardiomood.android.tools;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.cardiomood.android.db.HeartRateDataItemDAO;
import com.cardiomood.android.db.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class HeartRateDataHandler extends Thread {
	// session is terminated if no data were received in MAX_TIME_GAP
	private static final long MAX_TIME_GAP = 10*1000;

	private BlockingQueue<HeartRateDataItem> heartRateDataQueue;
	private HeartRateDataItemDAO hrDAO;
	private HeartRateSessionDAO sessionDAO;
	private long lastTimeStamp = 0;
	private HeartRateSession currentSession;

	public HeartRateDataHandler(Context context,
			BlockingQueue<HeartRateDataItem> heartRateQueue) {
		this.heartRateDataQueue = heartRateQueue;
		this.hrDAO = new HeartRateDataItemDAO(context);
		this.sessionDAO = new HeartRateSessionDAO(context);
	}

	@Override
	public void run() {
		try {
			hrDAO.open();
			sessionDAO.open();
			while (!isInterrupted()) {
				try {
					HeartRateDataItem hrDataItem = heartRateDataQueue.poll(300, TimeUnit.MILLISECONDS);
					processHeartRateDataItem(hrDataItem);
				} catch (InterruptedException e) {
					break;
				}
			}
			hrDAO.close();
			sessionDAO.close();
		} catch (Exception e) {
			Log.d("HeartRateDataHandler", "run(): exception = " + e + " // " + e.getLocalizedMessage());
		}
	}
	
	private void closeSession() {
		if (currentSession == null) {
			return;
		}
		if (Math.abs(System.currentTimeMillis() - lastTimeStamp) > MAX_TIME_GAP) {
			if (currentSession.getDateEnded() != null) {
				return;
			}
			final SQLiteDatabase db = hrDAO.getDatabase();
			db.beginTransaction();
			try {
				currentSession.setDateEnded(new Date());
				currentSession.setStatus(HeartRateSession.COMPLETED_STATUS);
				sessionDAO.update(currentSession);
				db.setTransactionSuccessful();
				Log.i("HeartRateDataHandler", "Session ended: id = " + currentSession.getId());
			} finally {
				db.endTransaction();
			}
		}
	}

	private void processHeartRateDataItem(HeartRateDataItem item) {
		if (item == null) {
			closeSession();
			return;
		}
		
		final SQLiteDatabase db = hrDAO.getDatabase();
		db.beginTransaction();
		try {
			closeSession();
			if (Math.abs(item.getTimeStamp().getTime() - lastTimeStamp) > MAX_TIME_GAP) {
				HeartRateSession session = new HeartRateSession();
				session.setDateStarted(new Date());
				currentSession = session;
				sessionDAO.insert(currentSession);
				Log.i("HeartRateDataHandler", "new session created: id = " + session.getId());
			}
			lastTimeStamp = item.getTimeStamp().getTime();
			item.setSessionId(currentSession.getId());
			hrDAO.insert(item);
			Log.i("HeartRateDataHandler",
				"new item added to DB: " + item.getRrTime() + " id = " + item.getId());
			currentSession.setStatus(HeartRateSession.IN_PROGRESS_STATUS);
			sessionDAO.update(currentSession);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
}
