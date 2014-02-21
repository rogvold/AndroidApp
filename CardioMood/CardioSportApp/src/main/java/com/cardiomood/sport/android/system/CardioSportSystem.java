package com.cardiomood.sport.android.system;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.cardiomood.android.tools.ConfigurationManager;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.sport.android.client.CardioSportService;
import com.cardiomood.sport.android.client.CardioSportServiceHelper;
import com.cardiomood.sport.android.client.json.ActivityStatus;
import com.cardiomood.sport.android.client.json.JsonActivity;
import com.cardiomood.sport.android.client.json.JsonError;
import com.cardiomood.sport.android.client.json.JsonResponse;
import com.cardiomood.sport.android.client.json.JsonWorkout;
import com.cardiomood.sport.android.client.json.ResponseConstants;
import com.cardiomood.sport.android.client.json.WorkoutStatus;
import com.cardiomood.sport.android.db.dao.ActivityInfoDAO;
import com.cardiomood.sport.android.db.dao.GPSInfoDAO;
import com.cardiomood.sport.android.db.dao.HeartRateDAO;
import com.cardiomood.sport.android.db.dao.WorkoutDAO;
import com.cardiomood.sport.android.db.entity.ActivityInfoEntity;
import com.cardiomood.sport.android.db.entity.GPSInfoEntity;
import com.cardiomood.sport.android.db.entity.HeartRateEntity;
import com.cardiomood.sport.android.db.entity.WorkoutEntity;
import com.cardiomood.sport.android.system.bluetooth.HeartRateMonitor;
import com.cardiomood.sport.android.system.gps.GPSMonitor;
import com.cardiomood.sport.android.tools.WorkerThread;
import com.cardiomood.sport.android.tools.config.ConfigurationConstants;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Project: CardioSport
 * User: danon
 * Date: 10.06.13
 * Time: 19:00
 */
public class CardioSportSystem {

    public static final int MSG_LOCATION_CHANGED = 1;
    public static final int MSG_LOCATION_STATUS_CHANGED = 2;
    public static final int MSG_LOCATION_PROVIDER_ENABLED = 3;
    public static final int MSG_LOCATION_PROVIDER_DISABLED = 4;
    public static final int MSG_HR_STATE_CHANGED = 5;
    public static final int MSG_HR_DATA_RECEIVED = 6;
    public static final int MSG_HR_SENSOR_LOCATION_CHANGED = 7;
    public static final int MSG_HR_BPM_CHANGED = 8;
    public static final int MSG_STATUS_CHANGED = 9;
    public static final int MSG_ACTIVITY_CHANGED = 10;
    public static final int MSG_SOUND_TRACK_CHANGED = 11;
    public static final int MSG_TIMER_EVENT = 12;

    public static final long WORKOUT_TIMER_INTERVAL = 1000L;


    public static final int STATUS_INITIAL = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_IN_PROGRESS = 2;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_STOPPED = 4;

    private final ConfigurationManager config = ConfigurationManager.getInstance();
    private final PreferenceHelper pref;
    private final Activity activity;
    private HeartRateMonitor hrMonitor;
    private GPSMonitor gpsMonitor;
    private String lastDeviceAddress = null;

    private LocationListener gpsListener = new GPSListener();
    private HeartRateMonitor.Callback hrListener = new HRListener();
    private Handler uiHandler = null;

    private GPSDBWorker gpsDBWorker = new GPSDBWorker();
    private HRDBWorker hrDBWorker = new HRDBWorker();
    private Timer inetTimer;
    private Timer workoutTimer;

    private List<GPSInfoEntity> gpsInetQueue = Collections.synchronizedList(new LinkedList<GPSInfoEntity>());
    private List<HeartRateEntity> hrInetQueue = Collections.synchronizedList(new LinkedList<HeartRateEntity>());

    private WorkoutEntity currentWorkout;
    private ActivityInfoEntity currentActivity;
    private Iterator<ActivityInfoEntity> activitiesIterator;

    private int status = STATUS_INITIAL;

    private int lastHeartRate = 0;

    private CardioSportServiceHelper cardioSportService =  null;
    private long workoutTime = 0;
    private long pauseTime = 0;
    private long activityTime;

    private ActivityInfoEntity pauseActivity = null;
    private int orderNumber = 0;


    public CardioSportSystem(Activity activity, JsonWorkout workout) {
        this.activity = activity;

        currentWorkout = new WorkoutEntity();
        currentWorkout.setName(workout.getName());
        currentWorkout.setDescription(workout.getDescription());
        currentWorkout.setStatus(WorkoutStatus.CURRENT);
        currentWorkout.setTemplateId(workout.getId());
        currentWorkout.setPlannedStartDate(workout.getStartDate());
        mergeWorkout(currentWorkout);

        if (workout.getActivities() == null || workout.getActivities().isEmpty()) {
            throw new IllegalArgumentException("Workout doesn't contain any activity.");
        }

        List<ActivityInfoEntity> activities = new LinkedList<ActivityInfoEntity>();
        for (JsonActivity a: workout.getActivities()) {
            ActivityInfoEntity act = new ActivityInfoEntity();
            act.setTemplateId(a.getId());
            act.setName(a.getName());
            act.setDuration(a.getDuration());
            act.setWorkoutId(currentWorkout.getId());
            act.setDescription(a.getDescription());
            act.setMaxHeartRate(a.getMaxHeartRate());
            act.setMinHeartRate(a.getMinHeartRate());
            act.setMaxSpeed(a.getMaxSpeed());
            act.setMinSpeed(a.getMinSpeed());
            act.setMaxTension(a.getMaxTension());
            act.setMinTension(a.getMinTension());
            act.setStatus(ActivityStatus.NEW);
            act.setOrderNumber(-1);
            activities.add(act);
        }
        for (ActivityInfoEntity a: activities) {
            mergeActivity(a);
        }

        activitiesIterator = activities.iterator();

        hrMonitor = new HeartRateMonitor(activity);
        gpsMonitor = new GPSMonitor(activity);

        hrMonitor.setCallback(hrListener);
        gpsMonitor.setListener(gpsListener);

        inetTimer = new Timer("inetTimer");
        workoutTimer = new Timer("workoutTimer");

        pref = new PreferenceHelper(activity.getApplicationContext());
    }

    public Activity getActivity() {
        return activity;
    }

    public Handler getUiHandler() {
        return uiHandler;
    }

    public void setUiHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    public Long getCurrentWorkoutId() {
        return currentWorkout == null ? null : currentWorkout.getId();
    }

    public WorkoutEntity getCurrentWorkout() {
        return currentWorkout;
    }

    public int getStatus() {
        return status;
    }

    private synchronized void setStatus(int status) {
        if (this.status == STATUS_INITIAL) {
            if (status != STATUS_READY)
                return;
            // INITIAL -> READY
        } else if (this.status == STATUS_READY) {
            if (status != STATUS_IN_PROGRESS) {
                return;
            }
            // READY -> IN_PROGRESS
        } else if (this.status == STATUS_IN_PROGRESS) {
            if (status != STATUS_STOPPED && status != STATUS_PAUSED) {
                return;
            }
            // IN_PROGRESS -> {STOPPED, PAUSED}
        } else if (this.status == STATUS_PAUSED) {
            if (status != STATUS_STOPPED && status != STATUS_IN_PROGRESS) {
                return;
            }
            // PAUSED -> {STOPPED, IN_PROGRESS}
        } else if (this.status == STATUS_STOPPED) {
            if (status != STATUS_INITIAL) {
                return;
            }
            // STOPPED -> INITIAL
        }
        if (status != this.status) {
            notifyUI(MSG_STATUS_CHANGED, status, 0, null);
            this.status = status;
        }
    }

    public void init() {
        try {
            setCurrentActivity(activitiesIterator.next());

            if (!gpsMonitor.isGPSEnabled()) {
                Toast.makeText(getActivity(), "Please, allow access to the GPS location provider.", Toast.LENGTH_SHORT).show();
            }
            gpsMonitor.start();
            hrMonitor.initBLEService();
            hrMonitor.attemptConnection();

            gpsDBWorker.start();
            hrDBWorker.start();

            cardioSportService = new CardioSportServiceHelper(CardioSportService.getInstance());
            cardioSportService.setEmail(pref.getString(ConfigurationConstants.USER_EMAIL_KEY, true));
            cardioSportService.setPassword(pref.getString(ConfigurationConstants.USER_PASSWORD_KEY, true));

        } catch (Exception ex) {
            Toast.makeText(getActivity(), "Cannot initialize service: " + ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            status = STATUS_INITIAL;
        }
    }

    public long getActivityTimeLeft() {
        if (currentActivity != null && currentActivity.getDuration() > activityTime) {
            return currentActivity.getDuration() - activityTime;
        } else return 0;
    }

    public long getWorkoutTime() {
        return workoutTime;
    }

    public long getActivityTime() {
        return activityTime;
    }

    public long getPauseTime() {
        return pauseTime;
    }

    public void start() {
        if (getStatus() != STATUS_READY)
            throw new IllegalStateException("Status should be READY(1) but it was actually " + getStatus());
        workoutTime = 0;
        activityTime = 0;
        inetTimer.scheduleAtFixedRate(new InetTask(), 5000L, 5000L);
        workoutTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final int s = getStatus();
                notifyUI(MSG_TIMER_EVENT, 0, 0, null);
                if (s == STATUS_IN_PROGRESS) {
                    workoutTime += WORKOUT_TIMER_INTERVAL;
                    activityTime += WORKOUT_TIMER_INTERVAL;
                    if (currentActivity.getDuration() < activityTime) {
                        // switch to next activity
                        switchToNextActivity();
                    }
                } else if (s == STATUS_PAUSED) {
                    pauseTime += WORKOUT_TIMER_INTERVAL;
                }
            }
        }, WORKOUT_TIMER_INTERVAL, WORKOUT_TIMER_INTERVAL);
        notifyUI(MSG_TIMER_EVENT, 0, 0, null);
        setStatus(STATUS_IN_PROGRESS);
        currentWorkout.setStartDate(System.currentTimeMillis());
        currentWorkout.setStatus(WorkoutStatus.IN_PROGRESS);
        mergeWorkout(currentWorkout);
        currentActivity.setStarted(System.currentTimeMillis());
        currentActivity.setStatus(ActivityStatus.IN_PROGRESS);
        currentActivity.setOrderNumber(orderNumber++);
        mergeActivity(currentActivity);

        // Start workout service command
        cardioSportService.startWorkout(currentWorkout.getTemplateId(), new CardioSportServiceHelper.Callback<Long>() {
            @Override
            public void onResult(Long result) {
                currentWorkout.setExternalId(result);
                mergeWorkout(currentWorkout);
            }

            @Override
            public void onError(JsonError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT);
            }
        });

        final ActivityInfoEntity startedActivity = currentActivity;
        // Start activity service command
        cardioSportService.startActivity(currentWorkout.getTemplateId(), currentActivity.getTemplateId(), new CardioSportServiceHelper.Callback<Long>() {
            @Override
            public void onResult(Long result) {
                startedActivity.setExternalId(result);
                mergeActivity(startedActivity);
            }

            @Override
            public void onError(JsonError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private void mergeWorkout(WorkoutEntity w) {
        try {
            WorkoutDAO wDao = new WorkoutDAO(getActivity().getApplicationContext());
            wDao.open(true);
            try {
                wDao.merge(w);
            } finally {
                wDao.close();
            }
        } catch (Exception ex) {
            Log.e("database", "Failed to save workout", ex);
        }
    }

    private void mergeActivity(ActivityInfoEntity a) {
        try {
            ActivityInfoDAO aDao = new ActivityInfoDAO(getActivity().getApplicationContext());
            aDao.open(true);
            try {
                aDao.merge(a);
            } finally {
                aDao.close();
            }
        } catch (Exception ex) {
            Log.e("database", "Failed to save activity", ex);
        }
    }

    public void switchToNextActivity() {
        activityTime = 0;
        if (activitiesIterator.hasNext()) {
            final ActivityInfoEntity stoppedActivity = currentActivity;
            // Stop activity service command
            cardioSportService.stopActivity(currentWorkout.getTemplateId(), currentActivity.getTemplateId(), getActivityTime(), new CardioSportServiceHelper.Callback() {
                @Override
                public void onResult(Object result) {
                    stoppedActivity.setSync(true);
                    mergeActivity(stoppedActivity);
                }

                @Override
                public void onError(JsonError error) {
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT);
                }
            });

            currentActivity.setEnded(System.currentTimeMillis());
            currentActivity.setStatus(ActivityStatus.COMPLETED);
            currentActivity.setDuration(getActivityTime());
            mergeActivity(currentActivity);

            setCurrentActivity(activitiesIterator.next());
            currentActivity.setStarted(System.currentTimeMillis());
            currentActivity.setStatus(ActivityStatus.IN_PROGRESS);
            currentActivity.setOrderNumber(orderNumber++);
            mergeActivity(currentActivity);

            final ActivityInfoEntity startedActivity = currentActivity;
            // Start activity service command
            cardioSportService.startActivity(currentWorkout.getTemplateId(), currentActivity.getTemplateId(), new CardioSportServiceHelper.Callback<Long>() {
                @Override
                public void onResult(Long result) {
                    startedActivity.setExternalId(result);
                    mergeActivity(startedActivity);
                }

                @Override
                public void onError(JsonError error) {
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT);
                }
            });
        } else {
            stop();
        }
    }

    public void stop() {
        if (status != STATUS_STOPPED && status != STATUS_INITIAL && status != STATUS_READY) {
            try {
                currentActivity.setEnded(System.currentTimeMillis());
                currentActivity.setStatus(ActivityStatus.COMPLETED);
                currentActivity.setDuration(getActivityTime());
                mergeActivity(currentActivity);
                currentWorkout.setStatus(WorkoutStatus.FINISHED);
                currentWorkout.setStopDate(System.currentTimeMillis());
                mergeWorkout(currentWorkout);
                setStatus(STATUS_STOPPED);

                final ActivityInfoEntity stoppedActivity = currentActivity;
                // Stop activity service command
                cardioSportService.stopActivity(currentWorkout.getTemplateId(), currentActivity.getTemplateId(), getActivityTime(), new CardioSportServiceHelper.Callback() {
                    @Override
                    public void onResult(Object result) {
                        stoppedActivity.setSync(true);
                        mergeActivity(stoppedActivity);
                    }

                    @Override
                    public void onError(JsonError error) {
                        Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT);
                    }
                });

                // Stop workout service command
                cardioSportService.stopWorkout(currentWorkout.getTemplateId(), new CardioSportServiceHelper.Callback() {
                    @Override
                    public void onResult(Object result) {
                        currentWorkout.setSync(true);
                        mergeWorkout(currentWorkout);
                    }

                    @Override
                    public void onError(JsonError error) {
                        Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT);
                    }
                });
                setCurrentActivity(null);
            } catch (Exception ex) {
                Log.e("CardioSportSystem", "stop() failed", ex);
            }
        }
    }

    public void pause() {
        pauseTime = 0L;
        setStatus(STATUS_PAUSED);
        pauseActivity = new ActivityInfoEntity();
        pauseActivity.setStatus(ActivityStatus.IN_PROGRESS);
        pauseActivity.setStarted(System.currentTimeMillis());
        pauseActivity.setName("Pause");
        pauseActivity.setWorkoutId(currentWorkout.getId());
        pauseActivity.setOrderNumber(orderNumber++);
        mergeActivity(pauseActivity);

        long newDuration = getActivityTimeLeft();

        currentActivity.setStatus(ActivityStatus.COMPLETED);
        currentActivity.setDuration(getActivityTime());
        currentActivity.setEnded(System.currentTimeMillis());
        mergeActivity(currentActivity);

        currentActivity.setId(null);
        currentActivity.setStatus(ActivityStatus.NEW);
        currentActivity.setStarted(0);
        currentActivity.setDuration(newDuration);
        mergeActivity(currentActivity);
        activityTime = 0;

        final ActivityInfoEntity pauseAct = pauseActivity;
        // Pause activity service command
        cardioSportService.pauseActivity(currentWorkout.getTemplateId(), currentActivity.getTemplateId(), new CardioSportServiceHelper.Callback<Long>() {
            @Override
            public void onResult(Long result) {
                pauseAct.setExternalId(result);
                mergeActivity(pauseAct);
            }

            @Override
            public void onError(JsonError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    public void resume() {
        if (status != STATUS_PAUSED) {
            return;
        }
        pauseActivity.setDuration(getPauseTime());
        pauseActivity.setEnded(System.currentTimeMillis());
        pauseActivity.setStatus(ActivityStatus.COMPLETED);
        mergeActivity(pauseActivity);

        final ActivityInfoEntity pauseAct = pauseActivity;
        // Resume activity service command
        cardioSportService.resumeActivity(currentWorkout.getTemplateId(), pauseTime, new CardioSportServiceHelper.Callback() {
            @Override
            public void onResult(Object result) {
                pauseAct.setSync(true);
                mergeActivity(pauseAct);
            }

            @Override
            public void onError(JsonError error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_SHORT);
            }
        });

        pauseActivity = null;
        currentActivity.setStarted(System.currentTimeMillis());
        currentActivity.setStatus(ActivityStatus.IN_PROGRESS);
        currentActivity.setOrderNumber(orderNumber++);
        mergeActivity(currentActivity);
        setStatus(STATUS_IN_PROGRESS);

    }

    public void release() {
        try {
            stop();
            gpsDBWorker.interrupt();
            hrDBWorker.interrupt();
            hrMonitor.disconnect();
            hrMonitor.cleanup();
            gpsMonitor.stop();
            gpsInetQueue.clear();
            hrInetQueue.clear();
            inetTimer.cancel();
            workoutTimer.cancel();
            inetTimer.purge();
            workoutTimer.purge();
        } catch (Exception ex) {
            Log.e("CardioSportSystem", "release() failed", ex);
        }
    }

    private void notifyUI(int what, int arg1, int arg2, Object obj) {
        final Handler h = uiHandler;
        if (h != null) {
            final Message msg = h.obtainMessage(what, arg1, arg2, obj);
            h.sendMessage(msg);
        }
    }

    public void setCurrentActivity(ActivityInfoEntity currentActivity) {
        if (currentActivity != this.currentActivity) {
            this.currentActivity = currentActivity;
            notifyUI(MSG_ACTIVITY_CHANGED, 0, 0, currentActivity);
        }
    }

    private class GPSListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            CardioSportSystem.this.notifyUI(MSG_LOCATION_CHANGED, 0, 0, location);
            if (getStatus() == STATUS_IN_PROGRESS || getStatus() == STATUS_PAUSED) {
                GPSInfoEntity entity = new GPSInfoEntity();
                entity.setAccuracy(location.getAccuracy());
                entity.setTimestamp(System.currentTimeMillis());
                entity.setWorkoutId(getCurrentWorkoutId());
                entity.setSpeed(location.getSpeed());
                entity.setAltitude(location.getAltitude());
                entity.setLatitude(location.getLatitude());
                entity.setLongitude(location.getLongitude());
                gpsDBWorker.put(entity);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            CardioSportSystem.this.notifyUI(MSG_LOCATION_STATUS_CHANGED, status, 0, new Object[] {provider, extras});
        }

        @Override
        public void onProviderEnabled(String provider) {
            CardioSportSystem.this.notifyUI(MSG_LOCATION_PROVIDER_ENABLED, 0, 0, provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            CardioSportSystem.this.notifyUI(MSG_LOCATION_PROVIDER_DISABLED, 0, 0, provider);
        }
    }

    private class HRListener implements HeartRateMonitor.Callback {
        public static final int RECONNECT_ATTEMPTS = Integer.MAX_VALUE;

        private int reconnectCount = 0;

        private long lastIntervalDate = 0;
        private short lastIntervalsLength = 0;

        @Override
        public void onConnectionStateChange(int oldState, int newState) {
            CardioSportSystem.this.notifyUI(MSG_HR_STATE_CHANGED, oldState, newState, null);
            int systemStatus = getStatus();
            if (newState == HeartRateMonitor.CONNECTED) {
                BluetoothDevice d = hrMonitor.getDevice();
                if (d != null)
                    lastDeviceAddress = d.getAddress();
            }
            if (systemStatus == STATUS_INITIAL || systemStatus == STATUS_STOPPED) {
                if (newState == HeartRateMonitor.CONNECTED) {
                    setStatus(STATUS_READY);
                    reconnectCount = RECONNECT_ATTEMPTS;
                }
            } else {
                if (newState == HeartRateMonitor.DISCONNECTED && (status == STATUS_IN_PROGRESS || status == STATUS_PAUSED)) {
                    if (reconnectCount > 0 && lastDeviceAddress != null) {
                        reconnectCount--;
                        hrMonitor.attemptConnection(lastDeviceAddress);
                    } else hrMonitor.attemptConnection();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {}
                }
            }

        }

        @Override
        public void onHRDataRecieved(int heartBeatsPerMinute, int energyExpended, short[] rrIntervals) {
            CardioSportSystem.this.notifyUI(MSG_HR_DATA_RECEIVED, heartBeatsPerMinute, energyExpended, rrIntervals);
            if (heartBeatsPerMinute != lastHeartRate) {
                CardioSportSystem.this.notifyUI(MSG_HR_BPM_CHANGED, heartBeatsPerMinute, lastHeartRate, rrIntervals);
                lastHeartRate = heartBeatsPerMinute;
            }
            if (getStatus() == STATUS_IN_PROGRESS || getStatus() == STATUS_PAUSED) {
                long time = System.currentTimeMillis();
                short length = 0;
                for (short rr: rrIntervals) {
                    length += rr;
                }
                if (lastIntervalsLength + lastIntervalDate > time) {
                    return;
                }
                lastIntervalDate = time;
                lastIntervalsLength = length;

                HeartRateEntity entity = new HeartRateEntity();
                entity.setWorkoutId(getCurrentWorkoutId());
                entity.setTimestamp(time);
                entity.setEnergyExpended(energyExpended);
                entity.setBpm((short) heartBeatsPerMinute);
                entity.setRr(rrIntervals);
                hrDBWorker.put(entity);
            }
        }

        @Override
        public void onSensorLocationChange(int oldLocation, int newLocation) {
            CardioSportSystem.this.notifyUI(MSG_HR_SENSOR_LOCATION_CHANGED, oldLocation, newLocation, null);
        }
    }

    private class GPSDBWorker extends WorkerThread<GPSInfoEntity> {

        private GPSInfoEntity saveToDB(GPSInfoEntity item) {
            try {
                GPSInfoDAO dao = new GPSInfoDAO(CardioSportSystem.this.getActivity());
                dao.open(true);
                dao.insert(item);
                dao.close();
            } catch (Exception ex) {
                Log.d("database", "GPSDBWorker.saveToDB failed", ex);
            }
            return item;
        }

        @Override
        public void processItem(GPSInfoEntity item) {
            item = saveToDB(item);
            gpsInetQueue.add(item);
        }
    }

private class HRDBWorker extends WorkerThread<HeartRateEntity> {

    private HeartRateEntity saveToDB(HeartRateEntity item) {
        try {
            HeartRateDAO dao = new HeartRateDAO(CardioSportSystem.this.getActivity());
            dao.open(true);
            dao.insert(item);
            dao.close();
        } catch (Exception ex) {
            Log.d("database", "HRDBWorker.saveToDB failed", ex);
        }
        return item;
    }

    @Override
    public void processItem(HeartRateEntity item) {
        item = saveToDB(item);
        hrInetQueue.add(item);
    }
}

    private class InetTask extends TimerTask {

        private InetObject object = null;
        private List<GPSInfoEntity> gps = new LinkedList<GPSInfoEntity>();
        private List<HeartRateEntity> hr = new LinkedList<HeartRateEntity>();
        private HeartRateDAO hrDAO = new HeartRateDAO(getActivity().getApplicationContext());
        private GPSInfoDAO gpsDAO = new GPSInfoDAO(getActivity().getApplicationContext());

        @Override
        public void run() {
            if (hrInetQueue.isEmpty() && gpsInetQueue.isEmpty() && object == null
                    && (getStatus() != STATUS_IN_PROGRESS && getStatus() != STATUS_PAUSED))
                return;

            if (object == null) {
                object = new InetObject();
            }
            // 1. extract all collected RR-intervals
            while (!hrInetQueue.isEmpty()) {
                HeartRateEntity entity = hrInetQueue.remove(0);
                hr.add(entity);
                if (object.rrTimestamp > entity.getTimestamp())
                    object.rrTimestamp = entity.getTimestamp();
                for (short r: entity.getRr()) {
                    object.rr.add(r);
                }
                object.hr.add(new Pair<Long, Integer>(entity.getTimestamp(), (int)entity.getBpm()));
            }
            // 2. extract all collected GPS coordinates
            while (!gpsInetQueue.isEmpty()) {
                GPSInfoEntity entity = gpsInetQueue.remove(0);
                gps.add(entity);
                GPSObject o = new GPSObject();
                o.accuracy = entity.getAccuracy();
                o.altitude = entity.getAltitude();
                o.latitude = entity.getLatitude();
                o.longitude = entity.getLongitude();
                o.speed = entity.getSpeed();
                o.timestamp = entity.getTimestamp();
                object.geo.add(o);
            }
            System.out.println("json: " + new Gson().toJson(object));
            // 3. calculate required values (e.g. Tension Index)
            // TODO

            // 4. obtain current music track, position
            // TODO

            try {
                JsonResponse response = cardioSportService.sendWorkoutData(currentWorkout.getTemplateId(), new Gson().toJson(object));
                if (response.getResponseCode() == ResponseConstants.OK) {
                    for (GPSInfoEntity e: gps) {
                        e.setSync(true);
                    }
                    for (HeartRateEntity e: hr) {
                        e.setSync(true);
                    }
                    hrDAO.open(true);
                    hrDAO.bulkUpdate(hr);
                    hrDAO.close();
                    gpsDAO.open(true);
                    gpsDAO.bulkUpdate(gps);
                    gpsDAO.close();
                    hr.clear();
                    gps.clear();
                    object = null;
                } else throw new RuntimeException(response.getError().getMessage());
            } catch (Exception ex) {
                Log.e("CardioSport.InetTask", "Failed to send workout data", ex);
            }
        }
    }

    private static class InetObject implements Serializable {
        public List<Short> rr = new LinkedList<Short>();
        public List<Pair<Long, Integer>> hr = new LinkedList<Pair<Long, Integer>>();
        public List<GPSObject> geo = new LinkedList<GPSObject>();
        public long rrTimestamp = System.currentTimeMillis();
    }

    private static class GPSObject implements Serializable {
        public double latitude;
        public double longitude;
        public double speed;
        public double accuracy;
        public double altitude;
        public long timestamp;
    }

}
