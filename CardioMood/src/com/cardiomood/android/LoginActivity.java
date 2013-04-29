package com.cardiomood.android;

import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.config.ConfigurationManager;
import com.cardiomood.android.server.CardioMood;
import com.cardiomood.android.services.CardioMoodService;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.cardiomood.android.tools.Tools;
import com.omnihealth.client_server_interaction.AccessToken;
import com.omnihealth.client_server_interaction.ServerResponse;
import com.omnihealth.client_server_interaction.ServerResponseCallback;


/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity implements ConfigurationConstants {
	/**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.cardiomood.android.extra.EMAIL";

	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;

    private boolean loginInProgress = false;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	
	private Toast toast;
	private long lastBackPressTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		loadConfigs();
		
		if (isLoggedIn()) {
			startMainActivity();
			return;
		}

		setContentView(R.layout.activity_login);

		// Set up the login form.
		mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
		if (TextUtils.isEmpty(mEmail)) {
			mEmail = ConfigurationManager.getInstance().getString(ConfigurationConstants.USER_EMAIL_KEY);
		}
		mEmailView = (EditText) findViewById(R.id.email);
		mEmailView.setText(mEmail);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
		findViewById(R.id.register_button).setOnClickListener(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						LoginActivity.this.startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_service_settings:
			startActivity(new Intent(this, ServiceSettingsActivity.class));
			return true;
		}
		return true;
	}
	
	protected boolean isLoggedIn() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		return sharedPref.getBoolean("loggedIn", false);
	}
	
	protected void loadConfigs() {
		Log.d("CardioMood", "LoginActivity.loadConfigs()");
		ConfigurationManager config = ConfigurationManager.getInstance();
		SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(SERVICE_HOST, sharedPref.getString(SERVICE_HOST, DEFAULT_SERVICE_HOST));
		config.setString(SERVICE_HOST, sharedPref.getString(SERVICE_HOST, DEFAULT_SERVICE_HOST));
		editor.putString(SERVICE_PATH, sharedPref.getString(SERVICE_PATH, DEFAULT_SERVICE_PATH));
		config.setString(SERVICE_PATH, sharedPref.getString(SERVICE_PATH, DEFAULT_SERVICE_PATH));
		editor.putString(SERVICE_PROTOCOL, sharedPref.getString(SERVICE_PROTOCOL, DEFAULT_SERVICE_PROTOCOL));
		config.setString(SERVICE_PROTOCOL, sharedPref.getString(SERVICE_PROTOCOL, DEFAULT_SERVICE_PROTOCOL));
		editor.putString(SERVICE_PORT, sharedPref.getString(SERVICE_PORT, DEFAULT_SERVICE_PORT));
		config.setString(SERVICE_PORT, sharedPref.getString(SERVICE_PORT, DEFAULT_SERVICE_PORT));
		editor.commit();
		CardioMoodService.markRebuildRequired();
		
		config.setString(USER_EMAIL_KEY, sharedPref.getString(USER_EMAIL_KEY, null));
		config.setString(USER_PASSWORD_KEY, sharedPref.getString(USER_PASSWORD_KEY, null));
        config.setString(USER_ACCESS_TOKEN_KEY, sharedPref.getString(USER_ACCESS_TOKEN_KEY, null));
	}
	
	protected void performLogIn(AccessToken accessToken) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean("loggedIn", true);
		editor.putString(USER_EMAIL_KEY, mEmailView.getText().toString());
		editor.putString(USER_PASSWORD_KEY, mPasswordView.getText().toString());
        editor.putString(USER_ACCESS_TOKEN_KEY, accessToken.getToken());
		editor.apply();
		
		ConfigurationManager conf = ConfigurationManager.getInstance();		
		conf.setString(ConfigurationConstants.USER_EMAIL_KEY, mEmailView.getText().toString());
		conf.setString(ConfigurationConstants.USER_PASSWORD_KEY, mPasswordView.getText().toString());
        conf.setString(ConfigurationConstants.USER_ACCESS_TOKEN_KEY, accessToken.getToken());
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
        if (loginInProgress) {
            return;
        }

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!mEmail.contains("@")) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);

            // TODO: it is strongly recommended to send SHA2 hash of the password
            CardioMood.getInstance().logIn(
                    mEmailView.getText().toString(),
                    mPasswordView.getText().toString(),
                    Tools.getAndroidDeviceID(getApplicationContext()),
                    new LoginCallback()
            );
            loginInProgress = true;
        }
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
	protected void startMainActivity() {
		startActivity(new Intent(this, WebMainActivity.class));
	}
	
	@Override
	public void onBackPressed() {
        if (this.lastBackPressTime < System.currentTimeMillis() - 4000) {
            toast = Toast.makeText(this, "Press BACK once more to close this application", Toast.LENGTH_SHORT);
            toast.show();
            this.lastBackPressTime = System.currentTimeMillis();
        } 
        else 
        {
            if (toast != null) 
            {
                toast.cancel();
            }
            loginInProgress = false;
            finish();
        }
	}

    public class LoginCallback implements ServerResponseCallback<AccessToken> {

        @Override
        public void onResponse(ServerResponse<AccessToken> accessTokenServerResponse) {
            showProgress(false);
            loginInProgress = false;

            if (accessTokenServerResponse.getResponseCode() == ServerResponse.OK) {
                performLogIn(accessTokenServerResponse.getResponse());
                startMainActivity();
            } else if (accessTokenServerResponse.getResponseCode() == ServerResponse.ServerError) {
                mPasswordView
                        .setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else {
                Log.d("LoginActivity", "Unexpected error: " + accessTokenServerResponse.getError());
                if (getApplicationContext() != null) {
                    Toast.makeText(getApplicationContext(), "Unexpected error. See logs for details.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
