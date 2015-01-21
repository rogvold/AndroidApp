package com.cardiomood.framework.fragment;

import android.widget.ArrayAdapter;

import com.commonsware.cwac.adapter.AdapterWrapper;

/**
 * Created by danshin on 03.11.13.
 */
public class SelectableAdapter<E> extends AdapterWrapper {

    private int selectedPosition = -1;

    public SelectableAdapter(ArrayAdapter<E> wrapped) {
        super(wrapped);
    }

    @Override @SuppressWarnings("unchecked")
    public E getItem(int position) {
        return (E) super.getItem(position);
    }

    public void setSelectedItem(int position) {
        selectedPosition = position;
    }

    public int getSelectedItem() {
        return selectedPosition;
    }
}
