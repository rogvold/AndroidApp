package com.cardiomood.android.air;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.air.gps.GPSService;
import com.cardiomood.android.air.tools.ParseTools;
import com.cardiomood.android.tools.CommonTools;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;


/**
 * A login screen that offers login via email/password.

 */
public class LoginActivity extends Activity{

    private static final String TAG = LoginActivity.class.getSimpleName();

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Spinner mUserListView;
    private ArrayAdapter<ParseUser> userListAdapter;
    private List<ParseUser> userList = new ArrayList<ParseUser>();
    private ProgressDialog mProgressDialog;
    private ParseQuery<ParseUser> userQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if we already logged in
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            if (GPSService.isServiceStarted) {
                startTrackingActivity();
            } else {
                startMainActivity();
            }
            return;
        } else {
            Toast.makeText(this, "Welcome! Please, sign in before your take off!",
                    Toast.LENGTH_SHORT).show();

            setContentView(R.layout.activity_login);
        }

        // Set up the login form.
        userListAdapter = new UserListArrayAdapter(this, userList);

        mUserListView = (Spinner) findViewById(R.id.users_list);
        mUserListView.setAdapter(userListAdapter);
        mUserListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ParseUser pu = userListAdapter.getItem(i);
                mEmailView.setText(pu.getEmail());
                mPasswordView.setText(null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // nothing selected
                mEmailView.setText(null);
                mPasswordView.setText(null);
            }
        });

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        if (savedInstanceState == null) {
            refreshUsersList();
        }
    }

    private void startTrackingActivity() {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra(TrackingActivity.GET_PLANE_FROM_SERVICE, true);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // process menu item click
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshUsersList();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startMainActivity() {
        startActivity(new Intent(this, PlanesActivity.class));
        finish();
    }

    private void refreshUsersList() {
        if (!CommonTools.isNetworkAvailable(this, "https://parse.com/")) {
            Toast.makeText(this, "Back-end servers are not accessible at the moment. \n" +
                    "Check Internet connection and try again.", Toast.LENGTH_SHORT).show();
        }

        // initialize progress dialog
        mProgressDialog = new ProgressDialog(this, android.R.style.Theme_Holo_Light_Dialog);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage("Contacting servers...");
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mProgressDialog.dismiss();
                if (userQuery != null)
                    userQuery.cancel();
            }
        });
        //mProgressDialog.setTitle("Please wait");
        mProgressDialog.show();

        // request user list
        userQuery = ParseUser.getQuery();
        userQuery.orderByAscending("lastName")
                .orderByAscending("firstName")
                .findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> parseUsers, ParseException e) {
                        if (e == null) {
                            userList.clear();
                            userList.addAll(parseUsers);
                            userListAdapter.notifyDataSetChanged();
                        } else {
                            // something went wrong
                            mProgressDialog.dismiss();
                            Toast.makeText(LoginActivity.this, "Something went wrong. \n" +
                                    "Check your internet connection.", Toast.LENGTH_SHORT).show();
                        }
                        if (mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                    }
                });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
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
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }
    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return true; //password.length() >= 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, ParseUser> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected ParseUser doInBackground(Void... params) {
            try {
                ParseUser pu = ParseUser.logIn(mEmail, mPassword);
                if (pu != null) {
                    return pu;
                }
            } catch (ParseException ex) {
                Log.e(TAG, "Parse authorization failed.", ex);
            }

            return null;
        }

        @Override
        protected void onPostExecute(final ParseUser result) {
            mAuthTask = null;
            showProgress(false);

            if (result != null) {
                // Successful sign in
                startMainActivity();
            } else {
                if (CommonTools.isNetworkAvailable(LoginActivity.this, "https://parse.com/")) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                } else {
                    mPasswordView.setError(getString(R.string.error_no_internet));
                    mPasswordView.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    public class UserListArrayAdapter extends ArrayAdapter<ParseUser> {

        public UserListArrayAdapter(Context context, List<ParseUser> objects) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, objects);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            // inflate view
            LayoutInflater inflater = getLayoutInflater();
            View itemView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            TextView text1 = (TextView) itemView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) itemView.findViewById(android.R.id.text2);

            // extract user name
            ParseUser pu = getItem(position);
            String fullName = ParseTools.getUserFullName(pu);

            // update view
            text1.setText(fullName);
            text2.setText(pu.getEmail());

            return itemView;
        }

    }
}



