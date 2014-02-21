package com.cardiomood.android;

import android.os.Bundle;
import android.view.MenuItem;

import com.cardiomood.android.fragments.SettingsFragment;
import com.cardiomood.android.tools.PreferenceActivityBase;

/**
 * Created by danon on 11.02.14.
 */
public class SettingsActivity extends PreferenceActivityBase<SettingsFragment> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        }
        return super.onOptionsItemSelected(item);
    }
}