package com.cardiomood.android.fragments;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.db.entity.SessionEntity;
import com.commonsware.cwac.endless.EndlessAdapter;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danshin on 05.11.13.
 */
public class SessionsEndlessAdapter extends EndlessAdapter  implements Filterable {

    private static final int STEP = 25;

    private String userId = null;
    private final List<SessionEntity> cachedSessions = new ArrayList<>(STEP*2);

    private String query = null;
    private Filter mFilter;


    public SessionsEndlessAdapter(ListAdapter wrapped) {
        super(wrapped);

        userId = ParseUser.getCurrentUser().getObjectId();
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
        QueryBuilder<SessionEntity, Long> queryBuilder = DatabaseHelperFactory.getHelper()
                .getSessionDao()
                .queryBuilder()
                .limit((long) STEP).offset((long) getWrappedAdapter().getCount())
                .orderBy("start_timestamp", false);
        if (TextUtils.isEmpty(query)) {
            queryBuilder.where().eq("sync_user_id", userId)
            .and().eq("deleted", false);
        } else {
            queryBuilder.where().eq("sync_user_id", userId)
            .and().eq("deleted", false)
            .and().like("name", new SelectArg("%" + query.trim() + "%"));
        }

        List<SessionEntity> sessions = queryBuilder.query();
        if (sessions == null)
            return false;
        synchronized (cachedSessions) {
            cachedSessions.addAll(sessions);
            return !cachedSessions.isEmpty();
        }
    }

    @Override
    protected void appendCachedData() {
        @SuppressWarnings("unchecked")
        ArrayAdapter wrapped = (ArrayAdapter) getWrappedAdapter();
        synchronized (cachedSessions) {
            wrapped.addAll(cachedSessions);
            cachedSessions.clear();
        }
    }

    public void refresh() {
        restartAppending();
        @SuppressWarnings("unchecked")
        ArrayAdapter<SessionEntity> adapter = (ArrayAdapter<SessionEntity>) getWrappedAdapter();
        adapter.clear();
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new DataFilter();
        }
        return mFilter;
    }

    private class DataFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            return new FilterResults();
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (constraint == null || constraint.length() == 0) {
                if (query == null)
                    return;
                query = null;
            } else {
                if (constraint.toString().trim().equals(query))
                    return;
                query = constraint.toString();
            }
            refresh();
        }
    }



}
