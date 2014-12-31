package com.cardiomood.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.expert.R;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.flurry.android.FlurryAgent;
import com.parse.ParseAnalytics;
import com.parse.ParseUser;

import java.security.MessageDigest;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Project: CardioSport
 * User: danon
 * Date: 15.06.13
 * Time: 14:16
 */
public class LoginActivity extends ActionBarActivity implements ConfigurationConstants {

    private static final String TAG = LoginActivity.class.getSimpleName();

    /**
     * The default email to populate the email field with.
     */
    public static final String EXTRA_EMAIL = "com.cardiomood.android.extra.EMAIL";

    // Values for email and password at the time of the login attempt.
    private String mEmail;
    private String mPassword;

    private String mFirstName;
    private String mLastName;

    private boolean loginInProgress = false;

    // UI references.
    @InjectView(R.id.email) EditText mEmailView;
    @InjectView(R.id.password) EditText mPasswordView;
    @InjectView(R.id.login_form) View mLoginFormView;
    @InjectView(R.id.login_status) View mLoginStatusView;
    @InjectView(R.id.login_status_message) TextView mLoginStatusMessageView;
    @InjectView(R.id.sign_in_button) Button mSignInButton;
    @InjectView(R.id.register_button) Button mRegisterButton;

    private PreferenceHelper prefHelper;

    private Toast toast;
    private long lastBackPressTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        // Add code to print out the key hash
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.cardiomood.android",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (Exception e) {

        }

        prefHelper = new PreferenceHelper(this, true);

        if (isLoggedIn()) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_login);

        // inject views
        ButterKnife.inject(this, this);

        // Set up the login form.
        mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        if (TextUtils.isEmpty(mEmail)) {
            mEmail = prefHelper.getString(ConfigurationConstants.USER_EMAIL_KEY);
        }
        mEmailView.setText(mEmail);

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

        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        mSignInButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FlurryAgent.logEvent("sign_in_clicked");
                        attemptLogin();
                    }
                });
        mRegisterButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        FlurryAgent.logEvent("register_clicked");
                        attemptRegister();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, FLURRY_API_KEY);
        if (prefHelper.getBoolean(ConfigurationConstants.USER_LOGGED_IN)) {
            showRestoreLoginRequest();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startMainActivity() {
        Toast.makeText(this, "Logged in as " + ParseUser.getCurrentUser().getUsername() , Toast.LENGTH_SHORT)
                .show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void attemptRegister() {
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
        } else if (!isEmailValid(mEmail)) {
            // TODO: validate email
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

            ParseUser user = new ParseUser();
            user.setUsername(mEmail);
            user.setPassword(mPassword);
            user.setEmail(mEmail);
            user.put("userRole", "user");
            user.put("firstName", "Cardio");
            user.put("lastName", "User");
            user.put("unitSystem", "METRIC");
            user.put("realTimeMonitoring", false);

            user.signUpInBackground().continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    loginInProgress = false;
                    if (task.isFaulted()) {
                        showProgress(false);
                        mPasswordView.setError(task.getError().getLocalizedMessage());
                        mPasswordView.requestFocus();
                    } else if (task.isCompleted()) {
                        startMainActivity();
                        prefHelper.putString(ConfigurationConstants.USER_EMAIL_KEY, mEmail);
                        mPasswordView.setText(null);
                        mPassword = null;
                        showProgress(false);
                    } else {
                        // canceled
                        showProgress(false);
                    }
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);

            loginInProgress = true;
        }
    }

    public void attemptLogin(String email, String password) {
        if (loginInProgress) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mEmail = email;
        mPassword = password;
        mEmailView.setText(mEmail);

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
        } else if (!isEmailValid(mEmail)) {
            // TODO: validate email
            // ^[-a-z0-9!#$%&'*+/=?^_`{|}~]+(?:\.[-a-z0-9!#$%&'*+/=?^_`{|}~]+)*@(?:[a-z0-9]([-a-z0-9]{0,61}[a-z0-9])?\.)*(?:aero|arpa|asia|biz|cat|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel|[a-z][a-z])$
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

            ParseUser.logInInBackground(mEmail, mPassword)
                    .continueWith(new Continuation<ParseUser, Object>() {
                        @Override
                        public Object then(Task<ParseUser> task) throws Exception {
                            loginInProgress = false;
                            if (task.isFaulted()) {
                                showProgress(false);
                                mPasswordView.setError(task.getError().getLocalizedMessage());
                                mPasswordView.requestFocus();
                            } else if (task.isCompleted()) {
                                startMainActivity();
                                prefHelper.putString(ConfigurationConstants.USER_EMAIL_KEY,
                                        task.getResult().getUsername());
                                mPasswordView.setText(null);
                                mPassword = null;
                                showProgress(false);
                            } else {
                                // canceled?
                                showProgress(false);
                            }
                            return null;
                        }
                    }, Task.UI_THREAD_EXECUTOR);
        }
    }

    public void attemptLogin() {
        attemptLogin(
                mEmailView.getText().toString(),
                mPasswordView.getText().toString()
        );
    }

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

    public boolean isEmailValid(String email) {
        return email != null && email.contains("@");
    }

    public boolean isLoggedIn() {
        return (ParseUser.getCurrentUser() != null);
    }

    private void showRestoreLoginRequest() {
        final String email = prefHelper.getString(USER_EMAIL_KEY, "");
        final String password = prefHelper.getString(USER_PASSWORD_KEY, "");
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            //s no login or password
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Restore previous authentication")
                .setMessage("We found previous authentication for account "
                                + email + ".\n"
                                + "Would you like to sign in under this ID?"
                )
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        attemptLogin(email, password);
                        mPasswordView.setText(null);
                        clearOldPreferences();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearOldPreferences();
                    }
                }).show();
    }

    private void clearOldPreferences() {
        prefHelper.remove(ConfigurationConstants.USER_LOGGED_IN);
        prefHelper.remove(ConfigurationConstants.USER_PASSWORD_KEY);
    }

    @Override
    public void onBackPressed() {
        if (this.lastBackPressTime < System.currentTimeMillis() - 4000) {
            toast = Toast.makeText(this, getString(R.string.press_back_to_close_app), Toast.LENGTH_SHORT);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.menu_service_settings:
//                startActivity(new Intent(this, ServiceSettingsActivity.class));
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
