package com.cardiomood.android.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;

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
    private CheckBoxPreference mUnlimitedLengthPref;
    private CheckBoxPreference mDisableBluetoothOnClosePref;

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
        helper.putBoolean(MEASUREMENT_UNLIMITED_LENGTH, helper.getBoolean(MEASUREMENT_UNLIMITED_LENGTH));
        helper.putBoolean(CONNECTION_DISABLE_BT_ON_CLOSE, helper.getBoolean(CONNECTION_DISABLE_BT_ON_CLOSE));

        mProtocolPref = (EditTextPreference) findPreference(SERVICE_PROTOCOL);
        mHostPref = (EditTextPreference) findPreference(SERVICE_HOST);
        mPortPref = (EditTextPreference) findPreference(SERVICE_PORT);
        mPathPref = (EditTextPreference) findPreference(SERVICE_PATH);
        mUnlimitedLengthPref = (CheckBoxPreference) findPreference(MEASUREMENT_UNLIMITED_LENGTH);
        mDisableBluetoothOnClosePref = (CheckBoxPreference) findPreference(CONNECTION_DISABLE_BT_ON_CLOSE);

        refreshSummaries();
    }

    private void refreshSummaries() {
        updatePrefSummary(mProtocolPref, mHostPref, mPortPref, mPathPref, mUnlimitedLengthPref);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        refreshSummaries();
    }
}
