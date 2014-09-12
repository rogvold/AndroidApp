package com.cardiomood.android.air;

import android.view.MenuItem;

import com.cardiomood.android.air.fragments.SettingsFragment;
import com.cardiomood.android.tools.settings.PreferenceActivityBase;


public class SettingsActivity extends PreferenceActivityBase<SettingsFragment> {

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
