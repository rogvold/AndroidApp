package com.cardiomood.android.fragments;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cardiomood.android.R;
import com.cardiomood.android.controls.CircledTextView;
import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.HeartRateDBContract;
import com.cardiomood.android.db.entity.ContinuousSessionEntity;
import com.cardiomood.android.tools.CommonTools;

import java.text.DateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by danshin on 03.11.13.
 */
public class SessionsArrayAdapter extends ArrayAdapter<ContinuousSessionEntity> {

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
    private int selectedPosition = -1;
    private DatabaseHelper databaseHelper;
    private Map<Long, Long> cache = Collections.synchronizedMap(new HashMap<Long, Long>());


    public SessionsArrayAdapter(Context context, DatabaseHelper databaseHelper, List<ContinuousSessionEntity> objects) {
        super(context, R.layout.history_item, objects);
        this.databaseHelper = databaseHelper;
    }

    public void setSelectedItem(int position) {
        selectedPosition = position;
    }

    public int getSelectedItem() {
        return selectedPosition;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = null;
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            itemView = inflater.inflate(R.layout.history_item, null);
        } else {
            itemView = convertView;
        }

        TextView name = (TextView) itemView.findViewById(R.id.item_title);
        TextView date = (TextView) itemView.findViewById(R.id.item_date);
        TextView duration = (TextView) itemView.findViewById(R.id.item_extra);
        CircledTextView avgHeartRate = (CircledTextView) itemView.findViewById(R.id.average_heart_rate);
        ContinuousSessionEntity session = getItem(position);

        if (session.getName() != null)
            name.setText(session.getName());
        else name.setText(getContext().getText(R.string.dafault_measurement_name) + " #" + session.getId());
        date.setText(DATE_FORMAT.format(session.getDateStarted()).toUpperCase());
        if (session.getDateEnded() != null) {
            String d = CommonTools.timeToHumanString(Math.abs(session.getDateEnded().getTime() - session.getDateStarted().getTime()));
            duration.setText(getContext().getText(R.string.duration) + " " + d);
        } else duration.setText(getContext().getText(R.string.duration) + " N/A");

        if (position == selectedPosition) {
            itemView.setBackgroundColor(getContext().getResources().getColor(com.cardiomood.android.tools.R.color.SkyBlue));
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        requestAvgHeartRate(session.getId(), avgHeartRate);
        return itemView;
    }

    private void requestAvgHeartRate(final Long id, final CircledTextView tv) {
        if (id == null)
            return;

        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                Long bpm = cache.get(id);
                if (bpm == null) {
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    Cursor cursor = db.query(
                            HeartRateDBContract.HeartRateData.TABLE_NAME,
                            new String[]{"sum(" + HeartRateDBContract.HeartRateData.COLUMN_NAME_BPM + "*" + HeartRateDBContract.HeartRateData.COLUMN_NAME_RR_TIME + ")/sum(" + HeartRateDBContract.HeartRateData.COLUMN_NAME_RR_TIME + ") as AVG_BPM"},
                            HeartRateDBContract.HeartRateData.COLUMN_NAME_SESSION_ID + "=?",
                            new String[]{String.valueOf(id)},
                            null, null, null
                    );
                    if (cursor.moveToFirst())
                        bpm = (long) Math.round(cursor.getFloat(cursor.getColumnIndex("AVG_BPM")));
                }
                if (bpm == null)
                    bpm = 0L;
                cache.put(id, bpm);
                return bpm;
            }

            @Override
            protected void onPostExecute(Object o) {
                long bpm = (Long) o;
                tv.setText(String.valueOf(bpm));
                if (bpm < 72) {
                    tv.setCircleColor(Color.rgb(124,222,230));
                } else if (bpm < 95) {
                    tv.setCircleColor(Color.rgb(0,1,241));
                } else if (bpm < 115) {
                    tv.setCircleColor(Color.rgb(1,156,100));
                } else if (bpm < 135) {
                    tv.setCircleColor(Color.rgb(250,132,1));
                } else {
                    tv.setCircleColor(Color.rgb(224,27,0));
                }
            }
        }.execute();
    }
}
