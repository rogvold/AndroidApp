package com.cardiomood.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.lite.R;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.analytics.AnalyticsHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Project: CardioMood
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

    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback =
            new Session.StatusCallback() {
                @Override
                public void call(Session session, SessionState state, Exception exception) {
                    onSessionStateChange(session, state, exception);
                }
            };

    private boolean loginInProgress = false;
    private boolean resumed = false;

    // UI references.
    @InjectView(R.id.email) EditText mEmailView;
    @InjectView(R.id.password) EditText mPasswordView;
    @InjectView(R.id.login_form) View mLoginFormView;
    @InjectView(R.id.login_status) View mLoginStatusView;
    @InjectView(R.id.login_status_message) TextView mLoginStatusMessageView;
    @InjectView(R.id.sign_in_button) Button mSignInButton;
    @InjectView(R.id.register_button) Button mRegisterButton;
    @InjectView(R.id.facebook_login_button) LoginButton mFacebookButton;

    private PreferenceHelper prefHelper;
    private AnalyticsHelper analyticsHelper;

    private Toast toast;
    private long lastBackPressTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        prefHelper = new PreferenceHelper(this, true);
        analyticsHelper = new AnalyticsHelper(this);

        if (isLoggedIn()) {
            ParseUser user = ParseUser.getCurrentUser();
            user.put("lastLogin", new Date());
            user.saveEventually();
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
                        analyticsHelper.logEvent("sign_in_clicked", "Sign in button clicked");
                        attemptLogin();
                    }
                });
        mRegisterButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        analyticsHelper.logEvent("register_clicked", "Sign up button clicked");
                        attemptRegister();
                    }
                });
        mFacebookButton.setReadPermissions(Arrays.asList("public_profile", "email"));

        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        getSupportActionBar().hide();
    }

    @Override
    protected void onStart() {
        super.onStart();
        analyticsHelper.logActivityStart(this);
        if (prefHelper.getBoolean(ConfigurationConstants.USER_LOGGED_IN)) {
            showRestoreLoginRequest();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        analyticsHelper.logActivityStop(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
        resumed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
        resumed = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uiHelper != null) {
            uiHelper.onDestroy();
            uiHelper = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    public void startMainActivity() {
        Toast.makeText(this, "Logged in as " + ParseUser.getCurrentUser().getUsername() , Toast.LENGTH_SHORT)
                .show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        // Only make changes if the activity is visible
        if (resumed) {
            if (state.isOpened()) {
                // session is opened => request email
                // make request to the /me API
                mFacebookButton.setEnabled(false);
                loginInProgress = true;
                showProgress(true);
                Request request = Request.newMeRequest(session,
                        new Request.GraphUserCallback() {
                            // callback after Graph API response with user object

                            @Override
                            public void onCompleted(GraphUser user,
                                                    Response response) {
                                if (user != null) {
                                    attemptFacebookLogin(user, Session.getActiveSession().getAccessToken(),
                                            Session.getActiveSession().getExpirationDate());
                                } else {
                                    if (Session.getActiveSession().isOpened()) {
                                        Session.getActiveSession().closeAndClearTokenInformation();
                                    }
                                    loginInProgress = false;
                                    mFacebookButton.setEnabled(true);
                                    showProgress(false);
                                    Toast.makeText(LoginActivity.this, "Facebook authentication failed!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                Request.executeBatchAsync(request);
            } else {
                mFacebookButton.setEnabled(true);
                showProgress(false);
            }
        }
    }


    private void attemptFacebookLogin(final GraphUser facebookUser, final String accessToken, final Date expirationDate) {
        analyticsHelper.logEvent("attempt_facebook_login", "Attempt Facebook login");
        @SuppressWarnings("unchecked")
        final String email = (String) facebookUser.asMap().get("email");
        // validate email
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, R.string.email_is_not_accessible, Toast.LENGTH_SHORT).show();
            if (Session.getActiveSession().isOpened()) {
                Session.getActiveSession().closeAndClearTokenInformation();
            }
            loginInProgress = false;
            mFacebookButton.setEnabled(true);
            showProgress(false);
            return;
        }
        checkUserExists(email).continueWith(new Continuation<ParseUser, Object>() {
            @Override
            public Object then(Task<ParseUser> task) throws Exception {
                if (task.isFaulted()) {
                    mFacebookButton.setEnabled(true);
                    showProgress(false);
                    Toast.makeText(LoginActivity.this, "Unable to check user existence." +
                            " Check your internet connection.", Toast.LENGTH_SHORT).show();
                } else {
                    ParseUser user = task.getResult();
                    if (user != null) {
                        // the existing user
                        signUpAndLink(user, facebookUser, accessToken, expirationDate);
                    } else {
                        // new user --> create account and link it to facebook
                        String firstName = facebookUser.getFirstName();
                        String lastName = facebookUser.getLastName();
                        String password = CommonTools.generateRandomString(8);
                        ParseUser parseUser = createParseUser(email, password, firstName, lastName);

                        signUpAndLink(parseUser, facebookUser, accessToken, expirationDate);
                    }
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void signUpAndLink(final ParseUser parseUser, GraphUser facebookUser, final String accessToken, final Date expirationDate) {
        if (parseUser.getObjectId() != null) {
            // existing user
            final String facebookId = facebookUser.getId();
            ParseFacebookUtils.logIn(facebookId, accessToken, expirationDate, new LogInCallback() {
                @Override
                public void done(ParseUser parseUser, ParseException e) {
                    loginInProgress = false;
                    if (e != null) {
                        // facebook login failed
                        analyticsHelper.logUserSignIn(parseUser.getObjectId());
                        analyticsHelper.logEvent("facebook_login", "User logged in via Facebook");
                    } else {
                        startMainActivity();
                        prefHelper.putString(ConfigurationConstants.USER_EMAIL_KEY,
                                parseUser.getUsername());
                        mPasswordView.setText(null);
                        mPassword = null;
                    }
                    mFacebookButton.setEnabled(true);
                    showProgress(false);
                }
            });
            return;
        }

        String gender = (String) facebookUser.asMap().get("gender");
        if (!TextUtils.isEmpty(gender)) {
            if ("MALE".equalsIgnoreCase(gender)) {
                parseUser.put("gender", "MALE");
            } else if ("FEMALE".equalsIgnoreCase(gender)) {
                parseUser.put("gender", "FEMALE");
            } else {
                parseUser.put("gender", "UNSPECIFIED");
            }
        }
        final String facebookId = facebookUser.getId();
        parseUser.signUpInBackground()
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(Task<Void> task) throws Exception {
                        loginInProgress = false;
                        if (task.isFaulted()) {
                            throw task.getError();
                        } else if (task.isCompleted()) {
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Please change your password!",
                                    Toast.LENGTH_LONG
                            ).show();
                            ParseFacebookUtils.link(parseUser, facebookId, accessToken,
                                    expirationDate, new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                analyticsHelper.logUserSignUp(ParseUser.getCurrentUser().getObjectId());
                                                analyticsHelper.logEvent("facebook_sign_up", "User signed up via Facebook");
                                                startMainActivity();
                                                prefHelper.putString(ConfigurationConstants.USER_EMAIL_KEY,
                                                        parseUser.getUsername());
                                            } else {
                                                // failed to link
                                                ParseUser.logOut();
                                            }
                                            mPasswordView.setText(null);
                                            mPassword = null;
                                        }
                                    });
                        }
                        mFacebookButton.setEnabled(true);
                        showProgress(false);
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR)
                .continueWith(new Continuation<Object, Object>() {
                    @Override
                    public Object then(Task<Object> task) throws Exception {
                        if (task.isFaulted()) {
                            Toast.makeText(LoginActivity.this, "Unable to use facebook login.",
                                    Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "signUpAndLink() failed", task.getError());
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
    }

    private Task<ParseUser> checkUserExists(String email) {
        return ParseUser.getQuery()
                .whereEqualTo("username", email)
                .findInBackground()
                .continueWith(new Continuation<List<ParseUser>, ParseUser>() {
                    @Override
                    public ParseUser then(Task<List<ParseUser>> task) throws Exception {
                        if (task.isFaulted())
                            throw task.getError();
                        if (task.isCompleted()) {
                            List<ParseUser> result = task.getResult();
                            if (result.isEmpty()) {
                                return null;
                            }
                            return result.iterator().next();
                        }
                        return null;
                    }
                });
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

            ParseUser user = createParseUser(mEmail, mPassword, "Cardio", "User");

            user.signUpInBackground().continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    loginInProgress = false;
                    if (task.isFaulted()) {
                        showProgress(false);
                        mPasswordView.setError(task.getError().getLocalizedMessage());
                        mPasswordView.requestFocus();
                    } else if (task.isCompleted()) {
                        analyticsHelper.logUserSignUp(ParseUser.getCurrentUser().getObjectId());
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
                                ParseUser user = task.getResult();
                                analyticsHelper.logUserSignIn(user.getObjectId());
                                user.put("lastLogin", new Date());
                                user.saveEventually();
                                startMainActivity();
                                prefHelper.putString(ConfigurationConstants.USER_EMAIL_KEY,
                                        user.getUsername());
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

    private ParseUser createParseUser(String email, String password, String firstName, String lastName) {
        ParseUser user = new ParseUser();
        user.setUsername(email);
        user.setPassword(password);
        user.setEmail(email);
        user.put("userRole", "user");
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("unitSystem", "METRIC");
        user.put("realTimeMonitoring", false);
        user.put("reg_via", "android_lite");
        user.put("locale", Locale.getDefault().toString());
        user.put("lastLogin", new Date());
        return user;
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

}
