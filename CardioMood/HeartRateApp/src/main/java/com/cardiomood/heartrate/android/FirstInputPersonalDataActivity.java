package com.cardiomood.heartrate.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.fragments.ProfileFragment;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.flurry.android.FlurryAgent;

public class FirstInputPersonalDataActivity extends FragmentActivity implements ProfileFragment.Callback {

    private Button buttonContinue;
    private TextView buttonSkip;
    private PreferenceHelper prefHelper;
    private ProfileFragment profileFragment;

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

        buttonSkip = (TextView) findViewById(R.id.button_skip);
        String uData = buttonSkip.getText().toString();
        SpannableString content = new SpannableString(uData);
        content.setSpan(new UnderlineSpan(), 0, uData.length(), 0);
        buttonSkip.setText(content);

        buttonSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlurryAgent.logEvent("button_skip_clicked");
                prefHelper.putBoolean(ConfigurationConstants.DO_NOT_ASK_INFO_ON_START, true);
                startActivity(new Intent(FirstInputPersonalDataActivity.this, MainActivity.class));
                finish();
            }
        });

        buttonContinue.setEnabled(allDataSpecified());

        profileFragment = (ProfileFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_profile);
        profileFragment.setCallback(this);
    }

    private boolean allDataSpecified() {
        if (TextUtils.isEmpty(prefHelper.getString(ConfigurationConstants.USER_EMAIL_KEY)))
            return false;
        if (TextUtils.isEmpty(prefHelper.getString(ConfigurationConstants.USER_FIRST_NAME_KEY)))
            return false;
        if (TextUtils.isEmpty(prefHelper.getString(ConfigurationConstants.USER_LAST_NAME_KEY)))
            return false;
        if (prefHelper.getInt(ConfigurationConstants.USER_SEX_KEY, 2) == 2)
            return false;
        if (TextUtils.isEmpty(prefHelper.getString(ConfigurationConstants.USER_BIRTH_DATE_KEY)))
            return false;
        return true;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_first_input_personal_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                FlurryAgent.logEvent("menu_settings_clicked");
                openSettingsActivity();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openSettingsActivity() {
        startActivity(new Intent(this, SettingsActivity.class));
    }


    @Override
    public void onSave() {
        buttonContinue.setEnabled(allDataSpecified());
    }

    @Override
    public void onSync() {
        // do nothing for now
    }
}
