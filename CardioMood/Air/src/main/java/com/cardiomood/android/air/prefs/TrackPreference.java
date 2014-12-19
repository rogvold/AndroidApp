package com.cardiomood.android.air.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.R;
import com.cardiomood.android.air.db.AirSessionDAO;
import com.cardiomood.android.air.db.AircraftDAO;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.entity.AirSessionEntity;
import com.cardiomood.android.air.db.entity.AircraftEntity;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Anton Danshin on 15/12/14
 */
public class TrackPreference extends DialogPreference {

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);

    private ListView mListView;
    private ArrayAdapter<AirSessionInfo> sessionArrayAdapter;
    private List<AirSessionInfo> airSessions;

    private long mValue = -1L;

    public TrackPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateDialogView() {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        View container = inflater.inflate(R.layout.dialog_tracks, null);
        mListView = (ListView) container.findViewById(R.id.tracks);

        airSessions = new ArrayList<>();
        sessionArrayAdapter = new AirSessionListArrayAdapter(getContext(), airSessions);
        mListView.setAdapter(sessionArrayAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AirSessionInfo info = sessionArrayAdapter.getItem(position);
                if (info != null) {
                    mValue = info.id;
                } else {
                    mValue = -1L;
                }
            }
        });

        return container;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        refreshSessionList();
        setDialogTitle("Select track");
        Toast.makeText(getContext(), "Loading list of tracks....", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore)
            mValue = shouldPersist() ? getPersistedLong(-1L) : -1L;
        else
            mValue = (Long) defaultValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return -1L;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            if (callChangeListener(mValue)) {
                if (shouldPersist()) {
                    persistLong(mValue);
                }
            }
        }
    }

    private void refreshSessionList() {
        Task.callInBackground(new Callable<List<AirSessionEntity>>() {
            @Override
            public List<AirSessionEntity> call() throws Exception {
                AirSessionDAO dao = HelperFactory.getHelper().getAirSessionDao();
                return dao.queryBuilder()
                        .orderBy("creation_timestamp", false)
                        .where().eq("sync_user_id", ParseUser.getCurrentUser().getObjectId())
                        .and().ne("deleted", true)
                        .query();
            }
        }).onSuccessTask(new Continuation<List<AirSessionEntity>, Task<List<AirSessionInfo>>>() {
            @Override
            public Task<List<AirSessionInfo>> then(Task<List<AirSessionEntity>> task) throws Exception {
                return extractSessionInfoAsync(task.getResult());
            }
        }).continueWith(new Continuation<List<AirSessionInfo>, Object>() {
            @Override
            public Object then(Task<List<AirSessionInfo>> task) throws Exception {
                if (task.isFaulted()) {
                    Toast.makeText(getContext(), "Task failed with exception: "
                            + task.getError().getMessage(), Toast.LENGTH_SHORT).show();
                } else if (task.isCompleted()) {
                    airSessions.clear();
                    airSessions.add(null);
                    airSessions.addAll(task.getResult());
                    sessionArrayAdapter.notifyDataSetChanged();
                    final int index = findItemPosition(mValue);
                    mListView.post(new Runnable() {
                        @Override
                        public void run() {
                            mListView.setSelection(index);
                        }
                    });
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    public Task<List<AirSessionInfo>> extractSessionInfoAsync(final List<AirSessionEntity> sessions) {
        return Task.callInBackground(new Callable<List<AirSessionInfo>>() {
            @Override
            public List<AirSessionInfo> call() throws Exception {
                AircraftDAO aircraftDAO = HelperFactory.getHelper().getAircraftDao();
                List<AirSessionInfo> result = new ArrayList<AirSessionInfo>(sessions.size());
                for (AirSessionEntity entity: sessions) {
                    AirSessionInfo info = new AirSessionInfo();
                    info.id = entity.getId();
                    info.creationDate = entity.getCreationDate();
                    info.lastUpdated = entity.getSyncDate();
                    info.name = entity.getName();
                    info.syncId = entity.getSyncId();
                    info.planeSyncId = entity.getSyncAircraftId();
                    AircraftEntity aircraft = aircraftDAO.findBySyncId(info.planeSyncId);
                    if (aircraft != null) {
                        info.planeId = aircraft.getId();
                        info.planeName = aircraft.getName();
                        info.planeCallName = aircraft.getCallName();
                        info.planeNumber = aircraft.getAircraftId();
                        info.planeType = aircraft.getAircraftType();
                    } else {
                        // unknown aircraft
                        info.planeId = -1;
                        info.planeName = "Unknown Aircraft";
                        info.planeCallName = "";
                        info.planeNumber = "";
                        info.planeType = "";
                    }
                    result.add(info);
                }
                return result;
            }
        });
    }

    private int findItemPosition(long id) {
        if (id < 0) {
            return 0;
        }
        for (int i=1; i<sessionArrayAdapter.getCount(); i++) {
            AirSessionInfo info = sessionArrayAdapter.getItem(i);
            if (info.id == id)
                return i;
        }
        return 0;
    }


    public class AirSessionListArrayAdapter extends ArrayAdapter<AirSessionInfo> {

        public AirSessionListArrayAdapter(Context context, List<AirSessionInfo> src) {
            super(context, R.layout.two_lines_layout, src);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                itemView = inflater.inflate(R.layout.two_lines_layout, parent, false);
                itemView.setBackgroundResource(R.drawable.list_selector_background);
            }

            AirSessionInfo info = getItem(position);
            TextView text1 = (TextView) itemView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) itemView.findViewById(android.R.id.text2);

            if (info != null) {
                text1.setTypeface(null, Typeface.NORMAL);
                if (info.name == null || info.name.trim().isEmpty()) {
                    text1.setText(info.planeName + " " + info.planeNumber);
                } else {
                    text1.setText(info.name.trim());
                }

                text2.setText(DATE_FORMAT.format(info.creationDate));
            } else {
                text1.setText("Select none");
                text2.setText("Disable the previous track");;
            }
            return itemView;
        }
    }

    public static class AirSessionInfo {
        long id;
        String syncId;
        Date creationDate;
        Date lastUpdated;
        String name;
        long planeId;
        String planeSyncId;
        String planeType;
        String planeName;
        String planeCallName;
        String planeNumber;
    }
}
