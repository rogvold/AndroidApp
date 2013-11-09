package com.cardiomood.android.fragments;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.db.model.HeartRateSession;

import java.text.DateFormat;
import java.util.List;

/**
 * Created by danshin on 03.11.13.
 */
public class SessionsArrayAdapter extends ArrayAdapter<HeartRateSession> {

    private DateFormat dateFormat = null;

    public SessionsArrayAdapter(Context context, List<HeartRateSession> objects) {
        super(context, R.layout.history_item, android.R.id.text1, objects);
        dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = super.getView(position, convertView, parent);

        TextView text1 = (TextView) itemView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) itemView.findViewById(android.R.id.text2);
        HeartRateSession session = getItem(position);

        if (session.getName() != null)
            text1.setText(session.getName());
        else text1.setText(getContext().getText(R.string.dafault_measurement_name) + " #" + session.getId());
        text2.setText(dateFormat.format(session.getDateStarted()));

        return itemView;
    }
}
