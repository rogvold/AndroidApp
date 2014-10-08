package com.cardiomood.android.air.fragments;



import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.IntListPreference;

import com.cardiomood.android.air.R;
import com.cardiomood.android.air.tools.Constants;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.settings.PreferenceActivityBase;

public class SettingsFragment extends PreferenceActivityBase.AbstractMainFragment {


    private IntListPreference radarRadiusPref;
    private CheckBoxPreference drawTrackPref;

    private PreferenceHelper helper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        helper = new PreferenceHelper(getActivity(), true);

        addPreferencesFromResource(R.xml.settings);

        radarRadiusPref = (IntListPreference) findPreference(Constants.CONFIG_RADAR_RADIUS);
        radarRadiusPref.setEntries(getResources().getStringArray(R.array.entries_radar_radius));
        radarRadiusPref.setEntryValues(getResources().getIntArray(R.array.values_radar_radius));
        radarRadiusPref.setDefaultValue(String.valueOf(Constants.DEFAULT_RADAR_RADIUS));

        drawTrackPref = (CheckBoxPreference) findPreference(Constants.CONFIG_DRAW_TRACK);

        refreshSummaries();
    }

    private void refreshSummaries() {
        updatePrefSummary(radarRadiusPref, drawTrackPref);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        refreshSummaries();
    }
}
