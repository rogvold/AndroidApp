package com.cardiomood.android.mipt.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cardiomood.android.mipt.db.CardioItemDAO;
import com.cardiomood.android.mipt.db.CardioSessionDAO;
import com.cardiomood.android.mipt.db.HelperFactory;
import com.cardiomood.android.mipt.db.entity.CardioItemEntity;
import com.cardiomood.android.mipt.db.entity.CardioSessionEntity;
import com.j256.ormlite.dao.GenericRawResults;

import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by antondanhsin on 03/11/14.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = BootCompletedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: this should be run in a single transaction
        Task.callInBackground(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                CardioSessionDAO sessionDAO = HelperFactory.getHelper().getCardioSessionDao();
                GenericRawResults<String[]> brokenSessions = sessionDAO.queryBuilder()
                        .selectColumns("_id", "end_timestamp")
                        .where().eq("end_timestamp", 0L)
                        .queryRaw();
                int count = 0;
                try {
                    for (String[] brokenSession: brokenSessions) {
                        fixBrokenSession(Long.parseLong(brokenSession[0]));
                        count++;
                    }
                } finally {
                    brokenSessions.close();
                }

                return count;
            }
        }).continueWith(new Continuation<Integer, Object>() {
            @Override
            public Object then(Task<Integer> task) throws Exception {
                if (task.isFaulted()) {
                    Log.w(TAG, "Task failed with exception", task.getError());
                } else if (task.isCompleted()) {
                    int count = task.getResult();
                    Log.i(TAG, "Fixed " + count + " broken session(s).");
                }
                return null;
            }
        });
    }

    void fixBrokenSession(long id) throws SQLException {
        Log.d(TAG, "fixBrokenSession() id = " + id);

        // load session entity
        CardioSessionDAO sessionDAO = HelperFactory.getHelper().getCardioSessionDao();
        CardioSessionEntity session = sessionDAO.queryForId(id);

        // get the last data item of the session
        CardioItemDAO itemDAO = HelperFactory.getHelper().getCardioItemDao();
        CardioItemEntity item = itemDAO.queryBuilder()
                .orderBy("_id", false)
                .where().eq("session_id", id)
                .queryForFirst();

        if (item == null) {
            // this session is empty
            // delete!
            session.setEndTimestamp(session.getStartTimestamp());
            session.setDeleted(true);
        } else {
            session.setEndTimestamp(item.getT());
        }

        session.setSyncDate(new Date());
        sessionDAO.update(session);
    }
}
