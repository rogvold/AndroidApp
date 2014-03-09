package com.cardiomood.android.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.dao.UserDAO;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.User;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danshin on 05.11.13.
 */
public class SessionsEndlessAdapter extends EndlessAdapter {

    private static final int STEP = 25;

    private HeartRateSessionDAO sessionDAO;
    private PreferenceHelper pHelper;
    private Long userId = null;
    private final List<HeartRateSession> cachedSessions = new ArrayList<HeartRateSession>(STEP*2);


    public SessionsEndlessAdapter(ListAdapter wrapped, Context context) {
        super(wrapped);
        sessionDAO = new HeartRateSessionDAO();
        pHelper = new PreferenceHelper(context);
        pHelper.setPersistent(true);
        long externalId = pHelper.getLong(ConfigurationConstants.USER_EXTERNAL_ID);
        User u = new UserDAO().findByExternalId(externalId);
        if (u != null)
            userId = u.getId();
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        ((TextView) row.findViewById(android.R.id.text1)).setText("Loading content...");
        ((TextView) row.findViewById(android.R.id.text2)).setText("Please wait.");
        parent.removeView(row);
        return row;
    }

    @Override
    protected boolean cacheInBackground() throws Exception {
        List<HeartRateSession> sessions =  sessionDAO.getAllSessionsOfUser(userId, STEP, getWrappedAdapter().getCount());
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
            for (HeartRateSession session: cachedSessions) {
                a.add(session);
            }
            cachedSessions.clear();
        }
    }
}
