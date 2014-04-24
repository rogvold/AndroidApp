package com.cardiomood.heartrate.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.flurry.android.FlurryAgent;

public class FirstInputPersonalDataActivity extends FragmentActivity {

    private Button buttonContinue;
    private PreferenceHelper prefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_input_personal_data);

        prefHelper = new PreferenceHelper(this, true);

        buttonContinue = (Button) findViewById(R.id.button_continue);
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("button_continue_clicked");
                prefHelper.putBoolean(ConfigurationConstants.DO_NOT_ASK_INFO_ON_START, true);
                startActivity(new Intent(FirstInputPersonalDataActivity.this, MainActivity.class));
                finish();
            }
        });
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
}
