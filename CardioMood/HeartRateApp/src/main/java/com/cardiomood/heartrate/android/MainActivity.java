package com.cardiomood.heartrate.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.heartrate.android.ads.AdMobController;
import com.cardiomood.heartrate.android.ads.AdsControllerBase;
import com.cardiomood.heartrate.android.db.DatabaseHelper;
import com.cardiomood.heartrate.android.dialogs.AboutDialog;
import com.cardiomood.heartrate.android.service.BluetoothHRMService;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;


public class MainActivity extends OrmLiteBaseActivity<DatabaseHelper> {

    private static final String TAG = MainActivity.class.getSimpleName();
    // Bluetooth Intent request codes
    private static final  int REQUEST_ENABLE_BT = 2;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 100000;

    private boolean disableBluetoothOnClose = false;
    private boolean aboutDialogShown = false;

    private PreferenceHelper mPrefHelper;
    private Handler mHandler;
    private boolean mScanning = false;
    private AlertDialog alertSelectDevice;

    // Service registration
    private boolean receiverRegistered = false;
    private boolean serviceBound = false;

    private BluetoothHRMService mBluetoothLeService;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = (BluetoothHRMService) ((BluetoothHRMService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
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
//
//
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (getActivity() != null) {
//                            getActivity().invalidateOptionsMenu();
//                        }
//                    }
//                });
//
//                // update button ui state and options layout
//                boolean enableControls = (newStatus != LeHRMonitor.CONNECTING_STATUS);
//                connectDeviceButton.setEnabled(enableControls);
//                disableEnableControls(enableControls, measurementOptionsLayout);
//
//                if (newStatus == LeHRMonitor.CONNECTED_STATUS) {
//                    FlurryAgent.logEvent("device_connected");
//                    openMonitor();
//                }
//                if (newStatus == LeHRMonitor.DISCONNECTING_STATUS || newStatus == LeHRMonitor.READY_STATUS && oldStatus != LeHRMonitor.INITIAL_STATUS) {
//                    FlurryAgent.logEvent("device_disconnected");
//                    setDisconnectedView();
//                }
//                if (newStatus == LeHRMonitor.INITIAL_STATUS) {
//                    setDisconnectedView();
//                }

            }
        }
    };

    // Ads
    // Home Screen Interstitial
    private static final String HOME_SCREEN_INTERSTITIAL_UNIT_ID = "ca-app-pub-1994590597793352/2533537627";
    // Home Screen Bottom Banner
    private static final String HOME_SCREEN_BOTTOM_BANNER_UNIT_ID = "ca-app-pub-1994590597793352/1056804428";

    private AdsControllerBase adsController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // initialize utilities
        mHandler = new Handler();
        mPrefHelper = new PreferenceHelper(getApplicationContext(), true);

        // establish connection to BluetoothHRMService
        if (!serviceBound) {
            Intent gattServiceIntent = new Intent(this, BluetoothHRMService.class);
            bindService(gattServiceIntent, mServiceConnection, Activity.BIND_AUTO_CREATE);
            serviceBound = true;
        }

        // register broadcast receiver to collect data from sensor
        if (!receiverRegistered) {
            registerReceiver(dataReceiver, makeGattUpdateIntentFilter());
            receiverRegistered = true;
        }

        adsController = new AdMobController(
                this,
                (RelativeLayout) findViewById(R.id.container),
                HOME_SCREEN_BOTTOM_BANNER_UNIT_ID,
                HOME_SCREEN_INTERSTITIAL_UNIT_ID
        );
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                FlurryAgent.logEvent("menu_settings_clicked");
                openSettingsActivity();
                return true;
            case R.id.action_about:
                FlurryAgent.logEvent("menu_about_clicked");
                showAboutDialog();
                return true;
            case R.id.action_bt_settings:
                FlurryAgent.logEvent("menu_bt_settings_clicked");
                openBluetoothSettings();
                return true;
            case R.id.action_feedback:
                FlurryAgent.logEvent("menu_feedback_clicked");
                openFeedbackWindow();
                return true;
            case R.id.action_personal_data:
                FlurryAgent.logEvent("menu_personal_data_clicked");
                openPersonalDataWindow();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openPersonalDataWindow() {
        startActivity(new Intent(this, PersonalDataActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, ConfigurationConstants.FLURRY_API_KEY);
        adsController.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsController.onResume();

    }

    @Override
    protected void onPause() {
        adsController.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
        adsController.onStop();
    }

    @Override
    protected void onDestroy() {
        adsController.onDestroy();
        adsController = null;

        // try disconnect from HRM Service
        tryDisconnect();

        // unregister broadcast receiver
        if (receiverRegistered) {
            unregisterReceiver(dataReceiver);
            receiverRegistered = false;
        }

        // unbind BluetoothHRMService
        if (serviceBound) {
            unbindService(mServiceConnection);
            serviceBound = false;
        }

        super.onDestroy();
    }

    private void openBluetoothSettings() {
        Intent intent =  new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    private void openFeedbackWindow() {
        startActivity(new Intent(this, FeedbackActivity.class));
    }

    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public void displayInterstitial() {
        ((AdMobController) adsController).showInterstitial();
    }

    private void showAboutDialog() {
        if (!aboutDialogShown) {
            AboutDialog dlg = new AboutDialog(this);
            dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    aboutDialogShown = false;
                }
            });
            dlg.setTitle(R.string.title_about_dialog);
            dlg.show();
        }
    }

    private void openSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void tryDisconnect() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
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
