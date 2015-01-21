package com.cardiomood.framework.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danshin on 05.11.13.
 */
public abstract class AbstractEndlessAdapter<E> extends EndlessAdapter implements Filterable {

    private static final int STEP = 25;

    private final List<E> cachedSessions = new ArrayList<>(STEP*2);

    private String query = null;
    private Filter mFilter;


    public AbstractEndlessAdapter(ArrayAdapter<E> wrapped) {
        super(wrapped);
        wrapped.setNotifyOnChange(false);
    }

    @Override
    protected View getPendingView(ViewGroup parent) {
        View row = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, null);
        ((TextView) row.findViewById(android.R.id.text1)).setText("Loading content...");
        ((TextView) row.findViewById(android.R.id.text2)).setText("Please wait.");
        return row;
    }

    protected abstract List<E> getNextData(String query, int skip, int limit);

    protected ArrayAdapter<E> getWrappedAdapter() {
        @SuppressWarnings("unchecked")
        ArrayAdapter<E> adapter = (ArrayAdapter<E>) super.getWrappedAdapter();
        return adapter;
    }

    @Override
    protected final boolean cacheInBackground() throws Exception {
        List<E> sessions = getNextData(query, getWrappedAdapter().getCount(), STEP);
        if (sessions == null)
            return false;
        synchronized (cachedSessions) {
            cachedSessions.addAll(sessions);
            return !cachedSessions.isEmpty();
        }
    }

    @Override
    protected void appendCachedData() {
        ArrayAdapter<E> wrapped = getWrappedAdapter();
        synchronized (cachedSessions) {
            for (E item: cachedSessions) {
                wrapped.add(item);
            }
            cachedSessions.clear();
        }
        wrapped.notifyDataSetChanged();
    }

    public void refresh() {
        restartAppending();
        ArrayAdapter<E> adapter = getWrappedAdapter();
        adapter.clear();
        adapter.notifyDataSetChanged();
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
