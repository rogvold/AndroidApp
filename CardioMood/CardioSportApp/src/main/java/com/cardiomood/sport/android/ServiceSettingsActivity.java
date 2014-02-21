package com.cardiomood.sport.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.cardiomood.sport.android.fragments.ServiceSettingsFragment;
import com.cardiomood.sport.android.tools.PreferenceActivityBase;

/**
 * Project: CardioSport
 * User: danon
 * Date: 15.06.13
 * Time: 17:20
 */
public class ServiceSettingsActivity extends PreferenceActivityBase<ServiceSettingsFragment> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    public ServiceSettingsFragment createMainFragment() {
        return new ServiceSettingsFragment();
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