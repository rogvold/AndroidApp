package com.cardiomood.android.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;

import com.cardiomood.android.R;
import com.cardiomood.android.tools.PreferenceActivityBase;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;

/**
 * Created by danon on 17.02.14.
 */
public class SettingsFragment extends PreferenceActivityBase.AbstractMainFragment implements ConfigurationConstants {

    private EditTextPreference mProtocolPref;
    private EditTextPreference mHostPref;
    private EditTextPreference mPortPref;
    private EditTextPreference mPathPref;
    private CheckBoxPreference mDisableBluetoothOnClosePref;
    private ListPreference mPreferredUnitSystemPref;
    private CheckBoxPreference mDisableSplashScreenPref;
    private CheckBoxPreference mLogGPSDataPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        PreferenceHelper helper = new PreferenceHelper(getActivity().getApplicationContext());
        helper.setPersistent(true);
        helper.putString(SERVICE_HOST, helper.getString(SERVICE_HOST, DEFAULT_SERVICE_HOST));
        helper.putString(SERVICE_PROTOCOL, helper.getString(SERVICE_PROTOCOL, DEFAULT_SERVICE_PROTOCOL));
        helper.putString(SERVICE_PORT, helper.getString(SERVICE_PORT, DEFAULT_SERVICE_PORT));
        helper.putString(SERVICE_PATH, helper.getString(SERVICE_PATH, DEFAULT_SERVICE_PATH));
        helper.putBoolean(CONNECTION_DISABLE_BT_ON_CLOSE, helper.getBoolean(CONNECTION_DISABLE_BT_ON_CLOSE));
        helper.putBoolean(DISABLE_SPLASH_SCREEN, helper.getBoolean(DISABLE_SPLASH_SCREEN));
        helper.putString(PREFERRED_MEASUREMENT_SYSTEM, helper.getString(PREFERRED_MEASUREMENT_SYSTEM));
        helper.putBoolean(GPS_COLLECT_LOCATION, helper.getBoolean(GPS_COLLECT_LOCATION));

        mProtocolPref = (EditTextPreference) findPreference(SERVICE_PROTOCOL);
        mHostPref = (EditTextPreference) findPreference(SERVICE_HOST);
        mPortPref = (EditTextPreference) findPreference(SERVICE_PORT);
        mPathPref = (EditTextPreference) findPreference(SERVICE_PATH);
        mDisableBluetoothOnClosePref = (CheckBoxPreference) findPreference(CONNECTION_DISABLE_BT_ON_CLOSE);
        mDisableSplashScreenPref = (CheckBoxPreference) findPreference(DISABLE_SPLASH_SCREEN);
        mPreferredUnitSystemPref = (ListPreference) findPreference(PREFERRED_MEASUREMENT_SYSTEM);
        mLogGPSDataPref = (CheckBoxPreference) findPreference(GPS_COLLECT_LOCATION);

        refreshSummaries();
    }

    private void refreshSummaries() {
        updatePrefSummary(mProtocolPref, mHostPref, mPortPref, mPathPref,
                mDisableBluetoothOnClosePref, mDisableSplashScreenPref, mPreferredUnitSystemPref,
                mLogGPSDataPref);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        refreshSummaries();
    }
}
