package com.cardiomood.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cardiomood.android.components.CustomViewPager;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.fragments.monitoring.ActivityCallback;
import com.cardiomood.android.fragments.monitoring.FragmentCallback;
import com.cardiomood.android.fragments.monitoring.HeartRateMonitoringFragment;
import com.cardiomood.android.heartrate.AbstractDataCollector;
import com.cardiomood.android.heartrate.CardioMoodHeartRateLeService;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.flurry.android.FlurryAgent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class MonitoringActivity extends ActionBarActivity implements ActionBar.TabListener, ActivityCallback {

    private static final String TAG = MonitoringActivity.class.getSimpleName();

    // Service registration parameters
    private CardioMoodHeartRateLeService mBluetoothLeService;
    private boolean receiverRegistered = false;
    private boolean serviceBound = false;

    private final Set<FragmentCallback> fragments = Collections.synchronizedSet(new LinkedHashSet<FragmentCallback>());

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = (CardioMoodHeartRateLeService) ((CardioMoodHeartRateLeService.LocalBinder) service).getService();
            LeHRMonitor monitor = mBluetoothLeService.getMonitor();
            if (monitor == null || monitor.getConnectionStatus() != LeHRMonitor.CONNECTED_STATUS) {
                Toast.makeText(MonitoringActivity.this, "Not connected. Closing...", Toast.LENGTH_SHORT).show();
                finish();
            }

            AbstractDataCollector collector = (AbstractDataCollector) mBluetoothLeService.getDataCollector();
            if (collector != null)
                collector.setListener(new AbstractDataCollector.SimpleListener() {
                    @Override
                    public void onDataSaved(HeartRateSession session) {
                        if (session != null && session.getId() != null) {
                            Intent intent = new Intent(MonitoringActivity.this, SessionDetailsActivity.class);
                            intent.putExtra(SessionDetailsActivity.SESSION_ID_EXTRA, session.getId());
                            startActivity(intent);
                            finish();
                        }
                    }
                });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            AbstractDataCollector collector = (AbstractDataCollector) mBluetoothLeService.getDataCollector();
            if (collector != null)
                collector.setListener(null);
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED.equals(action)) {
                final int newStatus = intent.getIntExtra(LeHRMonitor.EXTRA_NEW_STATUS, -100);
                final int oldStatus = intent.getIntExtra(LeHRMonitor.EXTRA_OLD_STATUS, -100);
                for (FragmentCallback c: fragments) {
                    c.notifyConnectionStatus(mBluetoothLeService, oldStatus, newStatus);
                }
            }

            if (LeHRMonitor.ACTION_BPM_CHANGED.equals(action)) {
                final int bpm = intent.getIntExtra(LeHRMonitor.EXTRA_NEW_BPM, -1);
                for (FragmentCallback c: fragments) {
                    c.notifyBPM(mBluetoothLeService, bpm);
                }
            }

            if (LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED.equals(action)) {
                final short[] rr = intent.getShortArrayExtra(LeHRMonitor.EXTRA_INTERVALS);
                AbstractDataCollector collector = (AbstractDataCollector) mBluetoothLeService.getDataCollector();
                for (FragmentCallback c: fragments) {
                    if (collector != null)
                        c.notifyProgress(mBluetoothLeService, collector.getProgress(), collector.getIntervalsCount(), (long) collector.getDuration());
                   // c.notifyRRIntervals(mBluetoothLeService, rr);
                }
            }

            updateView();
        }
    };

    SectionsPagerAdapter mSectionsPagerAdapter;
    CustomViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (CustomViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setPagingEnabled(true);
        mViewPager.setOffscreenPageLimit(3);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this)
            );
        }

        actionBar.setSelectedNavigationItem(0);

        // Establish connection to the Heart Rate Service
        if (!serviceBound) {
            Intent gattServiceIntent = new Intent(this, CardioMoodHeartRateLeService.class);
            bindService(gattServiceIntent, mServiceConnection, Activity.BIND_AUTO_CREATE);
            serviceBound = true;
        }
        if (!receiverRegistered) {
            registerReceiver(dataReceiver, makeGattUpdateIntentFilter());
            receiverRegistered = true;
        }

        updateView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, ConfigurationConstants.FLURRY_API_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (receiverRegistered) {
            receiverRegistered = false;
            unregisterReceiver(dataReceiver);
        }
        if (serviceBound) {
            serviceBound = false;
            unbindService(mServiceConnection);
        }
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_monitoring, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem stopItem = menu.findItem(R.id.menu_stop_monitoring);
        MenuItem startItem = menu.findItem(R.id.menu_start_monitoring);
        MenuItem bpmItem = menu.findItem(R.id.menu_bpm);
        if (mBluetoothLeService != null) {
            bpmItem.setTitle(String.valueOf(getCurrentBPM()));
            AbstractDataCollector collector = (AbstractDataCollector) mBluetoothLeService.getDataCollector();
            if (collector != null) {
                switch (collector.getStatus()) {
                    case COLLECTING:
                        startItem.setVisible(false);
                        stopItem.setVisible(true);
                        break;
                    case NOT_STARTED:
                        startItem.setVisible(true);
                        stopItem.setVisible(false);
                        break;
                    case COMPLETED:
                        startItem.setVisible(false);
                        stopItem.setVisible(false);
                        break;
                }
            }
        } else {
            stopItem.setVisible(false);
            startItem.setVisible(false);
            bpmItem.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_start_monitoring:
                FlurryAgent.logEvent("start_monitoring_clicked");
                startMonitoring();
                return false;
            case R.id.menu_stop_monitoring:
                FlurryAgent.logEvent("stop_monitoring_clicked");
                stopMonitoring();
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }



    private int getCurrentBPM() {
        if (mBluetoothLeService == null)
            return 0;
        LeHRMonitor monitor = mBluetoothLeService.getMonitor();
        if (monitor == null)
            return 0;
        else return monitor.getLastBPM();
    }

    private void updateView() {
        invalidateOptionsMenu();
    }

    private void startMonitoring() {
        if (mBluetoothLeService == null)
            return;
        AbstractDataCollector collector = (AbstractDataCollector) mBluetoothLeService.getDataCollector();
        if (collector != null)
            collector.startCollecting();
    }

    private void stopMonitoring() {
        if (mBluetoothLeService == null)
            return;
        AbstractDataCollector collector = (AbstractDataCollector) mBluetoothLeService.getDataCollector();
        if (collector != null)
            collector.stopCollecting();
    }

    @Override
    public void registerFragmentCallback(FragmentCallback callback) {
        fragments.add(callback);
    }

    @Override
    public void unregisterFragmentCallback(FragmentCallback callback) {
        fragments.remove(callback);
    }

    @Override
    public CardioMoodHeartRateLeService getService() {
        return mBluetoothLeService;
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return HeartRateMonitoringFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.monitoring_title_heart_rate).toUpperCase(l);
                case 1:
                    return getString(R.string.monitoring_title_stress).toUpperCase(l);
                case 2:
                    return getString(R.string.monitoring_title_spectrum).toUpperCase(l);
            }
            return null;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LeHRMonitor.ACTION_HEART_RATE_DATA_RECEIVED);
        intentFilter.addAction(LeHRMonitor.ACTION_BPM_CHANGED);
        intentFilter.addAction(LeHRMonitor.ACTION_CONNECTION_STATUS_CHANGED);
        return intentFilter;
    }

}
