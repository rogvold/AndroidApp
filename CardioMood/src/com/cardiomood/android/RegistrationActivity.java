package com.cardiomood.android;

import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.config.ConfigurationManager;
import com.cardiomood.android.server.CardioMood;
import com.cardiomood.android.server.ServerResponseCallbackRetry;
import com.cardiomood.android.services.CardioMoodService;
import com.cardiomood.android.services.ICardioMoodService;
import com.cardiomood.android.services.CardioMoodSimpleResponse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import com.cardiomood.android.tools.Tools;
import com.omnihealth.client_server_interaction.*;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class RegistrationActivity extends Activity {
    /**
	 * The default email to populate the email field with.
	 */
	public static final String EXTRA_EMAIL = "com.cardiomood.android.extra.EMAIL";

	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;
	private String mConfirmPassword;
    private boolean registrationInProgress = false;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;
	private EditText mFirstNameView;
	private EditText mLastNameView;
	private EditText mConfirmPasswordView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	

		setContentView(R.layout.activity_registration);
		setupActionBar();

		// Set up the login form.
		mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
		mEmailView = (EditText) findViewById(R.id.email_register);
		mEmailView.setText(mEmail);

		mPasswordView = (EditText) findViewById(R.id.password_register);
		mConfirmPasswordView = (EditText) findViewById(R.id.password_confirm_register);
		
		mFirstNameView = (EditText) findViewById(R.id.first_name_register);
		mLastNameView = (EditText) findViewById(R.id.last_name_register);

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_up_button_register).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Show the Up button in the action bar.
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			// TODO: If Settings has multiple levels, Up should navigate up
			// that hierarchy.
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_registration, menu);
		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (registrationInProgress) {
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();
		mConfirmPassword = mConfirmPasswordView.getText().toString();

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
		
		// Check confirmation password
		if (!mPassword.equals(mConfirmPassword)) {
			mConfirmPasswordView.setError(getString(R.string.error_passwords_dont_match));
			focusView = mConfirmPasswordView;
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
			registrationInProgress = true;

            final User user = new User();
            user.setPassword(mPassword);
            user.setEmail(mEmail);
            user.setFirstName(mFirstNameView.getText().toString());
            user.setLastName(mLastNameView.getText().toString());

            final ServerResponseCallbackRetry callback4 = new ServerResponseCallbackRetry<Integer>() {

                @Override
                public void retry() {
                    CardioMood.getInstance().updateProfile(user, this);
                }

                @Override
                public void onResponse(ServerResponse response) {
                    registrationInProgress = false;
                    showProgress(false);

                    if (response.getResponseCode() == ServerResponse.OK) {
                        performLogIn();
                    } else {

                    }
                }
            };

            final ServerResponseCallbackRetry callback3 = new ServerResponseCallbackRetry<User>() {

                @Override
                public void retry() {
                    CardioMood.getInstance().getProfile(this);
                }

                @Override
                public void onResponse(ServerResponse response) {
                    if (response.getResponseCode() == ServerResponse.OK) {
                        user.setUserId(((User) response.getResponse()).getUserId());
                        CardioMood.getInstance().updateProfile(user, callback4);
                    } else {
                        if (getApplicationContext() != null) {
                            Toast.makeText(getApplicationContext(), "Unexpected error.", Toast.LENGTH_SHORT).show();
                        }
                        registrationInProgress = false;
                        showProgress(false);
                    }
                }
            };

            final ServerResponseCallback<AccessToken> callback2 = new ServerResponseCallback<AccessToken>() {
                @Override
                public void onResponse(ServerResponse<AccessToken> response) {
                    if (response.getResponseCode() == ServerResponse.OK) {
                        // registered successfully, now login:
                        CardioMood.getInstance().getProfile(callback3);
                    } else {
                        if (getApplicationContext() != null) {
                            Toast.makeText(getApplicationContext(), "Unexpected error.", Toast.LENGTH_SHORT).show();
                        }
                        registrationInProgress = false;
                        showProgress(false);
                    }
                }
            };

            final ServerResponseCallback<Integer> callback1 = new ServerResponseCallback<Integer>() {
                @Override
                public void onResponse(ServerResponse<Integer> response) {
                    if (response.getResponseCode() == ServerResponse.OK) {
                        // registered successfully, now login:
                        CardioMood.getInstance().logIn(
                                mEmailView.getText().toString(),
                                mPasswordView.getText().toString(),
                                Tools.getAndroidDeviceID(getApplicationContext()),
                                callback2
                        );
                    } else {
                        if (response.getResponseCode() == ServerResponse.ServerError) {
                            if (getApplicationContext() != null) {
                                Toast.makeText(getApplicationContext(), response.getServerError().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            if (getApplicationContext() != null) {
                                Toast.makeText(getApplicationContext(), response.getError().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        registrationInProgress = false;
                        showProgress(false);

                        mEmailView.setError(getString(R.string.error_registration));
                    }
                }
            };

            CardioMood.register(
                    mEmailView.getText().toString(),
                    mPasswordView.getText().toString(),
                    callback1
            );
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
	
	protected void performLogIn() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean("loggedIn", true);
		editor.putString(ConfigurationConstants.USER_EMAIL_KEY, mEmailView.getText().toString());
		editor.putString(ConfigurationConstants.USER_PASSWORD_KEY, mPasswordView.getText().toString());
		editor.apply();

		ConfigurationManager conf = ConfigurationManager.getInstance();
		conf.setString(ConfigurationConstants.USER_EMAIL_KEY, mEmailView.getText().toString());
		conf.setString(ConfigurationConstants.USER_PASSWORD_KEY, mPasswordView.getText().toString());

		Intent loginIntent = new Intent(this, LoginActivity.class);
		loginIntent.putExtra(LoginActivity.EXTRA_EMAIL, mEmailView.getText().toString());
		startActivity(loginIntent);
	}
	
	@Override
	public void onBackPressed() {
        startActivity(new Intent(this, LoginActivity.class));
	}
}
