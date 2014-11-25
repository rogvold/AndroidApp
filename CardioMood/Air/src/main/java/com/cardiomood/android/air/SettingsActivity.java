package com.cardiomood.android.air;

import android.os.Build;
import android.os.Bundle;
import android.preference.IntListPreference;
import android.view.MenuItem;

import com.cardiomood.android.air.fragments.SettingsFragment;
import com.cardiomood.android.air.tools.Constants;
import com.cardiomood.android.tools.settings.PreferenceActivityBase;

public class SettingsActivity extends PreferenceActivityBase<SettingsFragment> {

    private IntListPreference radarRadiusPref = null;

    @Override @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            addPreferencesFromResource(R.xml.settings);

            radarRadiusPref = (IntListPreference) findPreference(Constants.CONFIG_RADAR_RADIUS);
            radarRadiusPref.setEntries(getResources().getStringArray(R.array.entries_radar_radius));
            radarRadiusPref.setEntryValues(getResources().getIntArray(R.array.values_radar_radius));
            radarRadiusPref.setDefaultValue(String.valueOf(Constants.DEFAULT_RADAR_RADIUS));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (radarRadiusPref != null) {
            radarRadiusPref.setSummary(radarRadiusPref.getValue());
        }
    }

    @Override
    public SettingsFragment createMainFragment() {
        return new SettingsFragment();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}
