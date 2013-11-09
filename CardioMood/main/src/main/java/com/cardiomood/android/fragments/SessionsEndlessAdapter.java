package com.cardiomood.android.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateSession;
import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danshin on 05.11.13.
 */
public class SessionsEndlessAdapter extends EndlessAdapter {

    private static final int STEP = 25;

    private HeartRateSessionDAO sessionDAO;
    private final List<HeartRateSession> cachedSessions = new ArrayList<HeartRateSession>(STEP*2);


    public SessionsEndlessAdapter(ListAdapter wrapped) {
        super(wrapped);
        setSerialized(true) ;
        sessionDAO = new HeartRateSessionDAO();
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, null);
        ((TextView) row.findViewById(android.R.id.text1)).setText("Loading content...");
        ((TextView) row.findViewById(android.R.id.text2)).setText("Please wait.");
        return row;
    }

    @Override
    protected boolean cacheInBackground() throws Exception {
        List<HeartRateSession> sessions =  sessionDAO.getAllSessions(STEP, getWrappedAdapter().getCount());
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
