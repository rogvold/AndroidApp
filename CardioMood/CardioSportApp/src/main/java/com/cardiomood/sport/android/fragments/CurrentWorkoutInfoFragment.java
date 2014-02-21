package com.cardiomood.sport.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.tools.ConfigurationManager;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.sport.android.R;
import com.cardiomood.sport.android.WorkoutActivity;
import com.cardiomood.sport.android.client.CardioSportService;
import com.cardiomood.sport.android.client.CardioSportServiceHelper;
import com.cardiomood.sport.android.client.json.JsonActivity;
import com.cardiomood.sport.android.client.json.JsonError;
import com.cardiomood.sport.android.client.json.JsonWorkout;
import com.cardiomood.sport.android.tools.Tools;
import com.cardiomood.sport.android.tools.config.ConfigurationConstants;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Project: CardioSport
 * User: danon
 * Date: 15.06.13
 * Time: 19:33
 */
public class CurrentWorkoutInfoFragment extends Fragment {

    public static int REQUEST_ENABLE_BT = 2;

    private ListView activityListView;
    private ArrayAdapter<JsonActivity> activityListAdapter;

    private JsonWorkout currentWorkout;

    private TextView workoutName;
    private TextView workoutDescription;
    private TextView workoutPlannedStart;
    private LinearLayout infoPanel;
    private LinearLayout messagePanel;

    private Button connectButton;
    private Button btButton;

    private volatile boolean refreshing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        activityListAdapter = new ArrayAdapter<JsonActivity>(getActivity(), R.layout.activity_name);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_workout, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_refresh);
        if (refreshing) {
            item.setEnabled(false);
            item.setTitle(R.string.refreshing);
        } else {
            item.setEnabled(true);
            item.setTitle(R.string.refresh);
        }
        super.onPrepareOptionsMenu(menu);
    }

    public void refresh() {
        if (refreshing)
            return;
        refreshing = true;
        getActivity().invalidateOptionsMenu();
        CardioSportServiceHelper service = new CardioSportServiceHelper(CardioSportService.getInstance());
        PreferenceHelper pref = new PreferenceHelper(getActivity().getApplicationContext());
        service.setEmail(pref.getString(ConfigurationConstants.USER_EMAIL_KEY, true));
        service.setPassword(pref.getString(ConfigurationConstants.USER_PASSWORD_KEY, true));
        service.getCurrentWorkout(new CardioSportServiceHelper.Callback<JsonWorkout>() {
            @Override
            public void onResult(JsonWorkout result) {
                try {
                    if (result != null) {
                        currentWorkout = result;
                        activityListAdapter.clear();
                        for (JsonActivity a: result.getActivities()) {
                            activityListAdapter.add(a);
                        }
                        workoutName.setText(currentWorkout.getName());
                        if (TextUtils.isEmpty(currentWorkout.getDescription()))
                            workoutDescription.setText("Workout description is not available.");
                        else workoutDescription.setText(currentWorkout.getDescription());
                        workoutPlannedStart.setText(new SimpleDateFormat(ConfigurationManager.getInstance().getString(ConfigurationConstants.APP_DATE_FORMAT)).format(new Date(currentWorkout.getStartDate())));
                        messagePanel.setVisibility(View.GONE);
                        infoPanel.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getActivity(), "No workout assigned.", Toast.LENGTH_SHORT).show();
                        messagePanel.setVisibility(View.VISIBLE);
                        infoPanel.setVisibility(View.GONE);
                        currentWorkout = null;
                    }
                } catch (Exception ex) {
                    Toast.makeText(getActivity(), "Incorrect workout data.", Toast.LENGTH_SHORT).show();
                }
                connectButton.setEnabled(currentWorkout != null);
                refreshing = false;
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onError(JsonError error) {
                refreshing = false;
                getActivity().invalidateOptionsMenu();
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_workout_info, container, false);

        activityListView = (ListView) v.findViewById(R.id.activities_list);
        activityListView.setAdapter(activityListAdapter);
        activityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                JsonActivity a = activityListAdapter.getItem(position);
                DialogFragment dialog = new ActivityDetailsDialog();
                dialog.setArguments(new Bundle());
                dialog.getArguments().putSerializable(ActivityDetailsDialog.ACTIVITY_ARGUMENT, a);
                dialog.show(getActivity().getSupportFragmentManager(), "activity_details");
            }
        });

        workoutName = (TextView) v.findViewById(R.id.workout_name);

        workoutDescription = (TextView) v.findViewById(R.id.workout_description);
        workoutDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentWorkout != null) {
                    new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT)
                            .setTitle(R.string.workout_description)
                            .setMessage(currentWorkout.getDescription())
                            .create()
                            .show();
                }
            }
        });

        workoutPlannedStart = (TextView) v.findViewById(R.id.planned_start);

        connectButton = (Button) v.findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnectButtonClicked();
            }
        });

        btButton = (Button) v.findViewById(R.id.bluetoothSettings);
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentBluetooth = new Intent();
                intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intentBluetooth);
            }
        });

        infoPanel = (LinearLayout) v.findViewById(R.id.info_panel);
        messagePanel = (LinearLayout) v.findViewById(R.id.message_panel);

        refresh();

        return v;
    }

    private void onConnectButtonClicked() {
        boolean bleSupported = Tools.isBLESupported();
        if (!bleSupported) {
            Toast.makeText(getActivity(), "This device doesn't support GATT service.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Toast.makeText(getActivity(), "Bluetooth adapter is not available.", Toast.LENGTH_SHORT).show();
            }
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else startWorkoutActivity();
        }
    }

    private void startWorkoutActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getActivity(), WorkoutActivity.class);
                intent.putExtra(WorkoutActivity.WORKOUT_EXTRA, currentWorkout);
                startActivity(intent);
            }
        }, 200);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                startWorkoutActivity();
            } else {
                Toast.makeText(getActivity(), "Bluetooth adapter is not enabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
