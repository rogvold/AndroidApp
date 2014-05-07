package com.cardiomood.wa.android.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.settings.PreferenceActivityBase;
import com.cardiomood.wa.android.R;
import com.cardiomood.wa.android.tools.ConfigurationConstants;
import com.flurry.android.FlurryAgent;

import java.util.HashMap;

/**
 * Created by danon on 17.02.14.
 */
public class SettingsFragment extends PreferenceActivityBase.AbstractMainFragment implements ConfigurationConstants {

    private CheckBoxPreference mDisableBluetoothOnClosePref;
    private CheckBoxPreference mDisableSplashScreenPref;
    private ListPreference mPreferredMeasurementSystemPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        PreferenceHelper helper = new PreferenceHelper(getActivity().getApplicationContext(), true);
        helper.putBoolean(CONNECTION_DISABLE_BT_ON_CLOSE, helper.getBoolean(CONNECTION_DISABLE_BT_ON_CLOSE));
        helper.putBoolean(DISABLE_SPLASH_SCREEN, helper.getBoolean(DISABLE_SPLASH_SCREEN));
        helper.putString(PREFERRED_MEASUREMENT_SYSTEM, helper.getString(PREFERRED_MEASUREMENT_SYSTEM));

        mDisableBluetoothOnClosePref = (CheckBoxPreference) findPreference(CONNECTION_DISABLE_BT_ON_CLOSE);
        mDisableSplashScreenPref = (CheckBoxPreference) findPreference(DISABLE_SPLASH_SCREEN);
        mPreferredMeasurementSystemPref = (ListPreference) findPreference(PREFERRED_MEASUREMENT_SYSTEM);

        refreshSummaries();
    }

    private void refreshSummaries() {
        updatePrefSummary(mDisableBluetoothOnClosePref, mDisableSplashScreenPref, mPreferredMeasurementSystemPref);
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
