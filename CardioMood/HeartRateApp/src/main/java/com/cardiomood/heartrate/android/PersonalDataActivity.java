package com.cardiomood.heartrate.android;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import com.cardiomood.heartrate.android.ads.AdMobController;
import com.cardiomood.heartrate.android.ads.AdsControllerBase;
import com.cardiomood.heartrate.android.fragments.ProfileFragment;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.flurry.android.FlurryAgent;


public class PersonalDataActivity extends FragmentActivity {

    // Ads
    // Home Screen Interstitial
    private static final String HOME_SCREEN_INTERSTITIAL_UNIT_ID = "ca-app-pub-1994590597793352/2533537627";
    // Home Screen Bottom Banner
    private static final String HOME_SCREEN_BOTTOM_BANNER_UNIT_ID = "ca-app-pub-1994590597793352/1056804428";

    private AdsControllerBase adsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_data);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ProfileFragment())
                    .commit();
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);

        adsController = new AdMobController(
                this,
                (RelativeLayout) findViewById(R.id.container),
                HOME_SCREEN_BOTTOM_BANNER_UNIT_ID,
                HOME_SCREEN_INTERSTITIAL_UNIT_ID
        );
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
