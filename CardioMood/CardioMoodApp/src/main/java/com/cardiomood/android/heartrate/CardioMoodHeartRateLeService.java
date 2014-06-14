package com.cardiomood.android.heartrate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.heartrate.bluetooth.HeartRateLeService;
import com.cardiomood.heartrate.bluetooth.LeHRMonitor;

/**
 * Created by danon on 18.11.13.
 */
public class CardioMoodHeartRateLeService extends HeartRateLeService {

    @Override
    protected void onReceiveBroadcast(Context context, Intent intent) {
        super.onReceiveBroadcast(context, intent);

        String action = intent.getAction();
        if (LeHRMonitor.ACTION_BATTERY_LEVEL.equals(action)) {
            Integer batteryLevel = intent.getIntExtra(LeHRMonitor.EXTRA_BATTERY_LEVEL, -1);

            if (batteryLevel != -1) {
                // save to preferences
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(ConfigurationConstants.LAST_DEVICE_BATTERY_LEVEL, batteryLevel + "%");
                editor.commit();

                // check for low battery
                if (batteryLevel < 20) {
                    Toast.makeText(context, "The battery level of your Heart Rate Sensor is low: "
                            + batteryLevel + "%.\nConsider replacing the battery.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
