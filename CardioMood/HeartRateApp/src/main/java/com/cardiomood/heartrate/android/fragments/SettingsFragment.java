package com.cardiomood.heartrate.android.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.settings.PreferenceActivityBase;
import com.cardiomood.heartrate.android.R;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.flurry.android.FlurryAgent;

import java.util.HashMap;

/**
 * Created by danon on 17.02.14.
 */
public class SettingsFragment extends PreferenceActivityBase.AbstractMainFragment implements ConfigurationConstants {

    private CheckBoxPreference mDisableBluetoothOnClosePref;
    private CheckBoxPreference mDisableSplashScreenPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        PreferenceHelper helper = new PreferenceHelper(getActivity().getApplicationContext(), true);
        helper.putBoolean(CONNECTION_DISABLE_BT_ON_CLOSE, helper.getBoolean(CONNECTION_DISABLE_BT_ON_CLOSE));
        helper.putBoolean(DISABLE_SPLASH_SCREEN, helper.getBoolean(DISABLE_SPLASH_SCREEN));

        mDisableBluetoothOnClosePref = (CheckBoxPreference) findPreference(CONNECTION_DISABLE_BT_ON_CLOSE);
        mDisableSplashScreenPref = (CheckBoxPreference) findPreference(DISABLE_SPLASH_SCREEN);

        refreshSummaries();
    }

    private void refreshSummaries() {
        updatePrefSummary(mDisableBluetoothOnClosePref, mDisableSplashScreenPref);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, final String key) {
        FlurryAgent.logEvent("preference_updated", new HashMap<String, String>() {
            {
                put("key", key);
            }
        });
        refreshSummaries();
    }
}
