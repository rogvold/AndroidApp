package com.cardiomood.heartrate.android;

import android.os.Bundle;
import android.view.MenuItem;

import com.cardiomood.android.tools.settings.PreferenceActivityBase;
import com.cardiomood.heartrate.android.fragments.SettingsFragment;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.flurry.android.FlurryAgent;

/**
 * Created by danon on 11.02.14.
 */
public class SettingsActivity extends PreferenceActivityBase<SettingsFragment> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
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
    public void onBackPressed() {
        super.onBackPressed();
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
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}