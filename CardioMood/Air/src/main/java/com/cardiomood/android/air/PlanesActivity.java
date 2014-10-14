package com.cardiomood.android.air;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.data.AirSession;
import com.cardiomood.android.air.data.Aircraft;
import com.cardiomood.android.air.data.SynchronizableParseObject;
import com.cardiomood.android.air.db.HelperFactory;
import com.cardiomood.android.air.db.SyncDAO;
import com.cardiomood.android.air.db.entity.SyncEntity;
import com.cardiomood.android.air.tools.ParseTools;
import com.cardiomood.android.tools.CommonTools;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class PlanesActivity extends Activity {

    private static final String TAG = PlanesActivity.class.getSimpleName();

    private ListView mPlanesListView;
    private View mCurrentUserView;
    private Button mStartButton;
    private Button mHistoryButton;

    private ArrayAdapter<Aircraft> planesListAdapter;
    private List<Aircraft> planesList;
    private ParseQuery<Aircraft> planesQuery;
    private Aircraft selectedPlane = null;
    private boolean refreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if signed in
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            performLogout();
            return;
        }

        // initialize view
        setContentView(R.layout.activity_planes);

        mCurrentUserView = findViewById(R.id.current_user_box);
        mPlanesListView = (ListView) findViewById(R.id.planes_list);
        mPlanesListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        mPlanesListView.setSelector(R.drawable.list_selector_background);
        mPlanesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedPlane = planesListAdapter.getItem(i);
                selectedPlane.pinInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            mStartButton.setEnabled(true);
                        } else {
                            mStartButton.setEnabled(false);
                            Toast.makeText(PlanesActivity.this,
                                    "Unable to choose this aircraft.", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "pinInBackground() failed", e);
                        }
                    }
                });
            }
        });

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setEnabled(false);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trySelectedPlane(selectedPlane);
            }
        });

        mHistoryButton = (Button) findViewById(R.id.button_history);
        mHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistoryActivity();
            }
        });

        TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        text1.setText(ParseTools.getUserFullName(currentUser));
        TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text2.setText(currentUser.getEmail());

        // initialize planes list
        planesList = new ArrayList<Aircraft>();
        planesListAdapter = new PlanesListArrayAdapter(this, planesList);
        mPlanesListView.setAdapter(planesListAdapter);
        refreshPlanes(false);
    }

    private void trySelectedPlane(final Aircraft plane) {
        ParseQuery.getQuery(AirSession.class)
                .whereDoesNotExist("endDate")
                .whereEqualTo("aircraftId", plane.getObjectId())
                .findInBackground(new FindCallback<AirSession>() {
                    @Override
                    public void done(List<AirSession> airSessions, ParseException e) {
                        if (e == null) {
                            if (airSessions.isEmpty()) {
                                // start tracking
                                startTrackingActivity(plane);
                            } else {
                                showExistingSessionDialog(plane, airSessions);
                            }
                        }
                    }
                });
    }

    private void openHistoryActivity() {
        startActivity(new Intent(this, HistoryActivity.class));
    }

    private void showExistingSessionDialog(final Aircraft plane, final List<AirSession> airSessions) {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dialog_title_aircraft_in_air)
                .setMessage(R.string.dialog_message_active_session_found)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedPlane = null;
                        mPlanesListView.clearChoices();
                        mStartButton.setEnabled(false);
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        long time = System.currentTimeMillis();
                        for (AirSession session: airSessions) {
                            session.setEndDate(time);
                            session.saveEventually();
                        }
                        startTrackingActivity(plane);
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    private void startTrackingActivity(Aircraft plane) {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra(TrackingActivity.SELECTED_PLANE_PARSE_ID, plane.getObjectId());
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshItem = menu.findItem(R.id.menu_refresh);
        refreshItem.setEnabled(!refreshing);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_planes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshPlanes(false);
                return true;
            case R.id.menu_settings:
                openSettingsActivity();
                return true;
            case R.id.menu_logout:
                showLogoutDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_message)
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        performLogout();
                    }
                }).show();
    }

    private void performLogout() {
        ParseUser.logOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void refreshPlanes(final boolean fromPin) {
        if (!CommonTools.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.backend_servers_are_not_vailable, Toast.LENGTH_SHORT).show();
        }

        mStartButton.setEnabled(false);

        if (planesQuery != null && refreshing) {
            planesQuery.cancel();
            planesQuery = null;
        }

        refreshing = true;
        invalidateOptionsMenu();

        // prepare query
        planesQuery = ParseQuery.getQuery(Aircraft.class)
                .orderByAscending("name");
        if (fromPin) {
            planesQuery.fromPin("planes");
        }

        // execute query
        planesQuery.findInBackground(new FindCallback<Aircraft>() {
            @Override
            public void done(List<Aircraft> parseObjects, ParseException e) {
                if (e == null) {
                    planesList.clear();
                    planesList.addAll(parseObjects);
                    planesListAdapter.notifyDataSetChanged();
                    selectedPlane = null;
                    mPlanesListView.clearChoices();
                    mStartButton.setEnabled(false);
                    if (!fromPin) {
                        ParseObject.pinAllInBackground("planes", parseObjects);
                        try {
                            syncWithDB(parseObjects);
                        } catch (SQLException ex) {
                            Log.w(TAG, "failed to sync remote data with local DB", ex);
                        }
                    }
                } else {
                    Log.w(TAG, "Failed to refresh planes list.", e);
                    if (!fromPin) {
                        Toast.makeText(PlanesActivity.this, R.string.failed_to_refresh_planes_list, Toast.LENGTH_SHORT).show();
                        refreshPlanes(true);
                    }
                    mStartButton.setEnabled(selectedPlane != null && mPlanesListView.getSelectedItemPosition() >= 0);

                }
                planesQuery = null;
                refreshing = false;
                invalidateOptionsMenu();
            }
        });
    }

    private void syncWithDB(List<? extends SynchronizableParseObject> planes) throws SQLException {
        for (SynchronizableParseObject plane: planes) {
            SyncEntity entity = SyncEntity.fromParseObject(plane, plane.getEntityClass());
            SyncDAO dao = HelperFactory.getHelper().getDaoForClass(entity.getClass());
            SyncEntity existingEntity = dao.findBySyncId(entity.getSyncId());

            if (existingEntity == null) {
                dao.create(dao.getDataClass().cast(entity));
            } else {
                // compare updated_at field
                boolean updateRequired = entity.getSyncDate().after(
                        existingEntity.getSyncDate() != null ?
                                existingEntity.getSyncDate() : new Date(0)
                );
                if (updateRequired) {
                    entity.setId(existingEntity.getId());
                    dao.update(dao.getDataClass().cast(entity));
                }
            }

        }
    }


    public class PlanesListArrayAdapter extends ArrayAdapter<Aircraft> {

        public PlanesListArrayAdapter(Context context, List<Aircraft> src) {
            super(context, android.R.layout.simple_list_item_2, src);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View itemView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            itemView.setBackgroundResource(R.drawable.list_selector_background);
            Aircraft plane = getItem(position);

            TextView text1 = (TextView) itemView.findViewById(android.R.id.text1);
            text1.setText(plane.getName());

            TextView text2 = (TextView) itemView.findViewById(android.R.id.text2);
            text2.setText(plane.getAircraftType() + " / " + plane.getAircraftId());

            return itemView;
        }
    }

}
