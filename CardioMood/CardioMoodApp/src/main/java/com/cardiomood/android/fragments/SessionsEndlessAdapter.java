package com.cardiomood.android.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.entity.HRSessionEntity;
import com.cardiomood.android.db.entity.UserEntity;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.commonsware.cwac.endless.EndlessAdapter;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danshin on 05.11.13.
 */
public class SessionsEndlessAdapter extends EndlessAdapter {

    private static final int STEP = 25;

    private RuntimeExceptionDao<HRSessionEntity, Long> sessionDAO;
    private RuntimeExceptionDao<UserEntity, Long> userDAO;
    private PreferenceHelper pHelper;
    private Long userId = null;
    private final List<HRSessionEntity> cachedSessions = new ArrayList<HRSessionEntity>(STEP*2);


    public SessionsEndlessAdapter(ListAdapter wrapped, Context context, DatabaseHelper databaseHelper) {
        super(wrapped);

        pHelper = new PreferenceHelper(context);
        pHelper.setPersistent(true);
        userId = pHelper.getLong(ConfigurationConstants.USER_ID);


        sessionDAO = databaseHelper.getRuntimeExceptionDao(HRSessionEntity.class);
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        View row = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, null);
        ((TextView) row.findViewById(android.R.id.text1)).setText("Loading content...");
        ((TextView) row.findViewById(android.R.id.text2)).setText("Please wait.");
        return row;
    }

    @Override
    protected boolean cacheInBackground() throws Exception {
        List<HRSessionEntity> sessions =  sessionDAO.queryBuilder()
                .limit((long) STEP).offset((long) getWrappedAdapter().getCount())
                .orderBy("date_started", false)
                .where().eq("user_id", userId)
                .query();
        if (sessions == null || sessions.isEmpty())
            return false;
        synchronized (cachedSessions) {
            cachedSessions.addAll(sessions);
            return !cachedSessions.isEmpty();
        }
    }

    @Override
    protected void appendCachedData() {
        @SuppressWarnings("unchecked")
        ArrayAdapter a=(ArrayAdapter)getWrappedAdapter();

        synchronized (cachedSessions) {
            for (HRSessionEntity session: cachedSessions) {
                a.add(session);
            }
            cachedSessions.clear();
        }
    }
}
