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
import com.cardiomood.android.air.tools.ParseTools;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;


public class PlanesActivity extends Activity {

    private static final String TAG = PlanesActivity.class.getSimpleName();

    private ListView mPlanesListView;
    private View mCurrentUserView;
    private Button mStartButton;

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

        TextView text1 = (TextView) mCurrentUserView.findViewById(android.R.id.text1);
        text1.setText(ParseTools.getUserFullName(currentUser));
        TextView text2 = (TextView) mCurrentUserView.findViewById(android.R.id.text2);
        text2.setText(currentUser.getEmail());

        // initialize planes list
        planesList = new ArrayList<Aircraft>();
        planesListAdapter = new PlanesListArrayAdapter(this, planesList);
        mPlanesListView.setAdapter(planesListAdapter);
        refreshPlanes();
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

    private void showExistingSessionDialog(final Aircraft plane, final List<AirSession> airSessions) {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("This aircraft is currently in air!")
                .setMessage("We found an active session for the selected aircraft. Do you want to discard existing session and start new one?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedPlane = null;
                        mPlanesListView.clearChoices();
                        mStartButton.setEnabled(false);
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
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
                refreshPlanes();
                return true;
            case R.id.menu_logout:
                showLogoutDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Confirm logout")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
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

    private void refreshPlanes() {
//        if (!CommonTools.isNetworkAvailable(this, "https://parse.com/")) {
//            Toast.makeText(this, "Back-end servers are not accessible at the moment. \n" +
//                    "Check Internet connection and try again.", Toast.LENGTH_SHORT).show();
//        }

        mStartButton.setEnabled(false);

        if (planesQuery != null && refreshing) {
            planesQuery.cancel();
            planesQuery = null;
        }

        refreshing = true;
        invalidateOptionsMenu();

        planesQuery = ParseQuery.getQuery(Aircraft.class)
                .orderByAscending("name");
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
                } else {
                    Log.w(TAG, "Failed to refresh planes list.", e);
                    Toast.makeText(PlanesActivity.this, "Failed to refresh planes list.", Toast.LENGTH_SHORT).show();
                    mStartButton.setEnabled(selectedPlane != null && mPlanesListView.getSelectedItemPosition() >= 0);
                }
                planesQuery = null;
                refreshing = false;
                invalidateOptionsMenu();
            }
        });
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
