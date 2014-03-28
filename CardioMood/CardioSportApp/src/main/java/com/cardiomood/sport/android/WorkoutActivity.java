package com.cardiomood.sport.android;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.sport.android.audio.SportSoundManager;
import com.cardiomood.sport.android.client.CardioSportService;
import com.cardiomood.sport.android.client.CardioSportServiceHelper;
import com.cardiomood.sport.android.client.json.JsonError;
import com.cardiomood.sport.android.client.json.JsonWorkout;
import com.cardiomood.sport.android.db.entity.ActivityInfoEntity;
import com.cardiomood.sport.android.system.CardioSportSystem;
import com.cardiomood.sport.android.tools.config.ConfigurationConstants;

/**
 * Project: CardioSport
 * User: danon
 * Date: 15.06.13
 * Time: 21:45
 */
public class WorkoutActivity extends Activity implements ConfigurationConstants {

    public static final String WORKOUT_EXTRA = "com.cardiomood.sport.android.extra.WORKOUT";

    private Toast toast;
    private long lastBackPressTime = 0;

    private PreferenceHelper prefHelper;
    private CardioSportServiceHelper service;
    private Double metronomeBPM = null;

    private TextView heartRate;
    private TextView latitude;
    private TextView longitude;
    private TextView activity;
    private TextView workout;
    private TextView speed;
    private TextView status;
    private TextView workoutTime;
    private TextView activityTime;
    private TextView activityTimeLeft;
    private Button startButton;
    private Button pauseButton;
    private TextView pace;
    private Switch metronome;

    private Thread paceChecker;

    private CardioSportSystem system;
    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CardioSportSystem.MSG_STATUS_CHANGED) {
                switch (msg.arg1) {
                    case CardioSportSystem.STATUS_READY: status.setText("Status: Ready");
                        break;
                    case CardioSportSystem.STATUS_IN_PROGRESS: status.setText("Status: In progress");
                        break;
                    case CardioSportSystem.STATUS_PAUSED: status.setText("Status: Paused");
                        break;
                    case CardioSportSystem.STATUS_STOPPED: status.setText("Status: Stopped");
                        break;
                }
                startButton.setEnabled(msg.arg1 == CardioSportSystem.STATUS_READY || msg.arg1 == CardioSportSystem.STATUS_IN_PROGRESS);
                pauseButton.setEnabled(msg.arg1 == CardioSportSystem.STATUS_IN_PROGRESS || msg.arg1 == CardioSportSystem.STATUS_PAUSED);
                return;
            }
            if (msg.what == CardioSportSystem.MSG_HR_DATA_RECEIVED) {
                heartRate.setText(msg.arg1 + "");
                return;
            }
            if (msg.what == CardioSportSystem.MSG_LOCATION_CHANGED) {
                Location l = (Location) msg.obj;
                latitude.setText(l.getLatitude() + "");
                longitude.setText(l.getLongitude() + "");
                speed.setText(l.getSpeed() + " m/s");
                return;
            }
            if (msg.what == CardioSportSystem.MSG_ACTIVITY_CHANGED) {
                ActivityInfoEntity a = (ActivityInfoEntity) msg.obj;
                activity.setText(a == null ? "N/A" : a.getName());
                return;
            }
            if (msg.what == CardioSportSystem.MSG_TIMER_EVENT) {
                workoutTime.setText(system.getWorkoutTime()/1000 + " s");
                activityTime.setText(system.getActivityTime()/1000 + " s");
                activityTimeLeft.setText(system.getActivityTimeLeft()/1000 + " s");
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        prefHelper = new PreferenceHelper(getApplicationContext());
        prefHelper.setPersistent(true);
        service = new CardioSportServiceHelper(CardioSportService.getInstance());
        service.setEmail(prefHelper.getString(USER_EMAIL_KEY));
        service.setPassword(prefHelper.getString(USER_PASSWORD_KEY));

        workout = (TextView) findViewById(R.id.workout);
        heartRate = (TextView) findViewById(R.id.heart_rate);
        latitude = (TextView) findViewById(R.id.latitude);
        longitude = (TextView) findViewById(R.id.longitude);
        activity = (TextView) findViewById(R.id.activity);
        speed = (TextView) findViewById(R.id.speed);
        status = (TextView) findViewById(R.id.status);
        workoutTime = (TextView) findViewById(R.id.workoutTime);
        activityTime = (TextView) findViewById(R.id.activity_time);
        activityTimeLeft = (TextView) findViewById(R.id.activity_time_left);
        startButton = (Button) findViewById(R.id.start_button);
        pauseButton = (Button) findViewById(R.id.pause_button);
        pace = (TextView) findViewById(R.id.pace);
        metronome = (Switch) findViewById(R.id.metronome_switch);

        metronome.setChecked(false);
        metronome.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    playMetronome();
                } else stopMetronome();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (system.getStatus() == CardioSportSystem.STATUS_IN_PROGRESS) {
                        system.stop();
                        metronome.setChecked(false);
                    } else {
                        startButton.setEnabled(false);
                        system.start();
                        new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                try {
                                    Thread.currentThread().sleep(2000);
                                } catch (InterruptedException ex) {

                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                startButton.setText((system.getStatus() != CardioSportSystem.STATUS_READY && system.getStatus() != CardioSportSystem.STATUS_STOPPED) ? R.string.stop : R.string.start);
                                super.onPostExecute(aVoid);
                            }
                        }.execute();
                    }

                }  catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int s = system.getStatus();
                try {
                    if (s == CardioSportSystem.STATUS_IN_PROGRESS) {
                        system.pause();
                    } else if (s == CardioSportSystem.STATUS_PAUSED) {
                        system.resume();
                    }
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        JsonWorkout workout = (JsonWorkout) getIntent().getExtras().get(WORKOUT_EXTRA);

        if (workout == null) {
            finish();
        }

        this.workout.setText(workout.getName());

        system = new CardioSportSystem(this, workout);
        system.setUiHandler(uiHandler);
        uiHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                system.init();
            }
        }, 500);

        paceChecker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    service.getMetronomeRate(new CardioSportServiceHelper.Callback<Double>() {
                        @Override
                        public void onResult(Double result) {
                            if (!result.equals(metronomeBPM)) {
                                metronomeBPM = result;
                                pace.setText(String.valueOf(metronomeBPM == null ? "No pace" : metronomeBPM.intValue() + " bpm"));
                                playMetronome();
                            }
                            if (metronomeBPM == null)
                                stopMetronome();
                        }

                        @Override
                        public void onError(JsonError error) {
                            Toast.makeText(WorkoutActivity.this, "failed!!! " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        });
        paceChecker.start();
    }

    private void stopMetronome() {
        SportSoundManager.INSTANCE.stop();
    }

    private void playMetronome() {
        if (metronomeBPM == null || !metronome.isChecked()) {
            stopMetronome();
        } else {
            SportSoundManager.INSTANCE.play(metronomeBPM.intValue());
        }
    }

    @Override
    public void onBackPressed() {
        if (this.lastBackPressTime < System.currentTimeMillis() - 4000) {
            toast = Toast.makeText(this, getString(R.string.press_back_to_close_workout), Toast.LENGTH_SHORT);
            toast.show();
            this.lastBackPressTime = System.currentTimeMillis();
        }
        else
        {
            if (toast != null)
            {
                toast.cancel();
            }
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        system.stop();
        system.release();
        if (paceChecker!= null) {
            paceChecker.interrupt();
        }
        stopMetronome();
        super.onDestroy();
    }
}