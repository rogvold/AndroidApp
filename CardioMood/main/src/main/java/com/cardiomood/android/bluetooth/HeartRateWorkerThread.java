package com.cardiomood.android.bluetooth;

import android.content.Context;
import android.util.Log;

import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;
import com.cardiomood.android.tools.WorkerThread;

import java.util.Date;

/**
 * Created by danshin on 31.10.13.
 */
public class HeartRateWorkerThread extends WorkerThread<HeartRateDataItem> {

    private static final String TAG = "CardioMood.HeartRateWorkerThread";

    private final Context context;
    private HeartRateDataItemDAO hrDAO;
    private HeartRateSessionDAO sessionDAO;

    private HeartRateSession currentSession;

    public HeartRateWorkerThread(Context context) {
        this.context = context;

        hrDAO = new HeartRateDataItemDAO();
        sessionDAO = new HeartRateSessionDAO();
    }

    public HeartRateSession getCurrentSession() {
        return currentSession;
    }

    @Override
    public void processItem(HeartRateDataItem item) {
        if (currentSession.getStatus() != SessionStatus.IN_PROGRESS) {
            currentSession.setStatus(SessionStatus.IN_PROGRESS);
            currentSession = sessionDAO.update(currentSession);
        }
        item.setSessionId(currentSession.getId());
        hrDAO.insert(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        currentSession = new HeartRateSession();
        currentSession.setDateStarted(new Date());
        currentSession.setStatus(SessionStatus.NEW);
        currentSession = sessionDAO.insert(currentSession);
        Log.d(TAG, "onStart(): session created // id = " + currentSession.getId());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (currentSession != null) {
            currentSession.setDateEnded(new Date());
            currentSession.setStatus(SessionStatus.COMPLETED);
            sessionDAO.update(currentSession);
        }
    }
}
