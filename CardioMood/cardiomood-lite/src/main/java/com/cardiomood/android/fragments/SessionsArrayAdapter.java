package com.cardiomood.android.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cardiomood.android.controls.CircledTextView;
import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.db.entity.CardioItemDAO;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.lite.R;
import com.cardiomood.android.tools.CommonTools;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * Created by danshin on 03.11.13.
 */
public class SessionsArrayAdapter extends ArrayAdapter<SessionEntity> {

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
    private int selectedPosition = -1;
    private final LongSparseArray<Long> cache = new LongSparseArray<Long>();


    public SessionsArrayAdapter(Context context, List<SessionEntity> objects) {
        super(context, R.layout.history_item, objects);
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
        SessionEntity session = getItem(position);

        if (session.getName() != null)
            name.setText(session.getName());
        else name.setText(getContext().getText(R.string.dafault_measurement_name) + " #" + session.getId());
        date.setText(DATE_FORMAT.format(new Date(session.getStartTimestamp())).toUpperCase());
        if (session.getEndTimestamp() != null) {
            String d = CommonTools.timeToHumanString(Math.abs(session.getEndTimestamp() - session.getStartTimestamp()));
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

        new AsyncTask<Object, Object, Long>() {

            @Override
            protected Long doInBackground(Object[] params) {
                Long bpm = null;
                synchronized (cache) {
                    bpm = cache.get(id);
                }
                if (bpm == null) {
                    try {
                        CardioItemDAO dao = DatabaseHelperFactory.getHelper().getCardioItemDao();
                        List<String[]> results = dao.queryRaw(
                                "select 60000.0/(sum(rr)/count(rr)) as avg_bpm from cardio_items where session_id = ?",
                                new String[]{String.valueOf(id)}
                        ).getResults();
                        if (results.size() == 1) {
                            String s = results.get(0)[0];
                            if (s != null)
                                bpm = Math.round(Double.valueOf(s));
                            else bpm = 0L;
                        }
                    } catch (SQLException ex) {
                        Timber.d(ex, "Failed to calculate average BPM for session_id=" + id);
                    }
                }
                if (bpm == null)
                    bpm = 0L;
                synchronized (cache) {
                    cache.put(id, bpm);
                }
                return bpm;
            }

            @Override
            protected void onPostExecute(Long bpm) {
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
