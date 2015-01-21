package com.cardiomood.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.cardiomood.android.dialogs.AboutDialog;
import com.cardiomood.android.dialogs.WhatsNewDialog;
import com.cardiomood.android.expert.R;
import com.cardiomood.android.fragments.EditParseUserFragment;
import com.cardiomood.android.fragments.HistoryFragment;
import com.cardiomood.android.fragments.NewMeasurementFragment;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.analytics.AnalyticsHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.ui.CustomViewPager;
import com.facebook.Session;
import com.parse.ParseUser;

import java.util.Locale;

import bolts.Continuation;
import bolts.Task;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, ConfigurationConstants {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toast toast;
    private long lastBackPressTime = 0;
    private boolean whatsNewDialogShown = false;

    private PreferenceHelper mPrefHelper;
    private ProgressDialog pDialog;
    private AnalyticsHelper analyticsHelper;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    CustomViewPager mViewPager;

    @Override @DebugLog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefHelper = new PreferenceHelper(this, true);
        analyticsHelper = new AnalyticsHelper(this);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setIcon(R.drawable.ic_launcher);
        actionBar.setDisplayShowHomeEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (CustomViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setPagingEnabled(true);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);

                // hide soft input keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        analyticsHelper.logActivityStart(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPrefHelper.getBoolean(WhatsNewDialog.CONFIG_SHOW_DIALOG_ON_STARTUP, true, true)) {
            showWhatsNewDialog();
            mViewPager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ActionBar actionBar = getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setSelectedNavigationItem(1);
                    }
                }
            }, 200);
        } else {
            if (mViewPager.getCurrentItem() == 0) {
                mViewPager.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CommonTools.hideSoftInputKeyboard(MainActivity.this);
                        ActionBar actionBar = getSupportActionBar();
                        if (actionBar != null) {
                            actionBar.setSelectedNavigationItem(1);
                        }
                    }
                }, 200);
            }
        }

        invalidateOptionsMenu();
    }

    @Override
    protected void onStop() {
        super.onStop();
        analyticsHelper.logActivityStop(this);
    }

    private void showWhatsNewDialog() {
        if (whatsNewDialogShown)
            return;
        whatsNewDialogShown = true;
        Dialog dialog = new WhatsNewDialog(this);
        dialog.setTitle(R.string.whats_new_dialog_title);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mPrefHelper.putBoolean(WhatsNewDialog.CONFIG_SHOW_DIALOG_ON_STARTUP, false);
            }
        });
        dialog.show();
    }

    private void openBluetoothSettings() {
        Intent intent =  new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    private void showAboutDialog() {
        AboutDialog dlg = new AboutDialog(this);
        dlg.setTitle(R.string.about_dialog_title);
        dlg.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_settings:
                 analyticsHelper.logEvent("menu_settings_clicked", "Menu Settings clicked");
                 startActivity(new Intent(this, SettingsActivity.class));
                 return true;
            case R.id.menu_bt_settings:
                analyticsHelper.logEvent("menu_bt_settings_clicked", "Menu Bluetooth Settings clicked");
                openBluetoothSettings();
                return true;
            case R.id.menu_feedback:
                analyticsHelper.logEvent("menu_feedback_clicked", "Menu Feedback clicked");
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;
            case R.id.menu_about:
                analyticsHelper.logEvent("menu_about_clicked", "Menu About clicked");
                showAboutDialog();
                return true;
            case R.id.menu_logout:
                analyticsHelper.logEvent("menu_logout_clicked", "Menu Log out clicked");
                logout();
                return true;
            case R.id.menu_change_password:
                analyticsHelper.logEvent("menu_change_password_clicked", "Menu Change password clicked");
                showChangePasswordDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    private void logout() {
        if (NewMeasurementFragment.inProgress) {
            Toast.makeText(this, "Stop recording first.", Toast.LENGTH_SHORT).show();
        }
        Intent loginIntent = new Intent(this, LoginActivity.class);
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            // facebook logout
            Session session = Session.getActiveSession();
            if (session != null) {
                if (!session.isClosed()) {
                    session.closeAndClearTokenInformation();
                }
            } else {
                session = new Session(this);
                Session.setActiveSession(session);
                session.closeAndClearTokenInformation();
            }

            // logout
            ParseUser.logOut();
            loginIntent.putExtra(LoginActivity.EXTRA_EMAIL, currentUser.getUsername());
        }

        analyticsHelper.logEvent("user_log_out", "Log out");
        analyticsHelper.setUserId(null);

        startActivity(loginIntent);
        finish();
    }

    private void showChangePasswordDialog() {
        if (NewMeasurementFragment.inProgress) {
            Toast.makeText(this, "Stop recording first.", Toast.LENGTH_SHORT).show();
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        final EditText newPassword = (EditText) dialogView.findViewById(R.id.new_password);
        final EditText confirmNewPassword = (EditText) dialogView.findViewById(R.id.confirm_new_password);

        newPassword.setText(null);
        confirmNewPassword.setText(null);

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setView(dialogView)
                .setPositiveButton("Change Password", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String p1 = newPassword.getText().toString();
                        String p2 = confirmNewPassword.getText().toString();
                        if (!TextUtils.isEmpty(p1) && p1.equals(p2) && p1.length() >= 4) {
                            ParseUser user = ParseUser.getCurrentUser();
                            changePassword(user, p1);
                        } else {
                            Toast.makeText(MainActivity.this, "Passwords must match!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setTitle("Change Password...")
                .create()
                .show();
    }

    private void changePassword(ParseUser user, String newPassword) {
        pDialog = new ProgressDialog(this);
        pDialog.setIndeterminate(true);
        pDialog.setMessage("Trying to set new password...");
        pDialog.show();

        user.setPassword(newPassword);
        user.saveInBackground().continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(Task<Void> task) throws Exception {
                if (!isFinishing()) {
                    if (task.isFaulted()) {
                        Timber.w(task.getError(), "unable to update password");
                        Toast.makeText(MainActivity.this, "Unable to update password.", Toast.LENGTH_SHORT)
                                .show();
                    } else if (task.isCompleted()) {
                        Toast.makeText(MainActivity.this, "Password has been updated.",
                                Toast.LENGTH_SHORT).show();
                        logout();
                    }
                    if (pDialog != null && pDialog.isShowing()) {
                        pDialog.dismiss();
                    }
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0: return createProfileFragment();
                case 1: return new NewMeasurementFragment();
                case 2: return new HistoryFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }

        private Fragment createProfileFragment() {
            EditParseUserFragment fragment = new EditParseUserFragment();
            fragment.setCallback(new EditParseUserFragment.Callback() {
                @Override
                public void onSave() {

                }

                @Override
                public void onSync() {
                    ParseUser user = ParseUser.getCurrentUser();
                    mPrefHelper.putString(
                            ConfigurationConstants.PREFERRED_MEASUREMENT_SYSTEM,
                            user.has("unitSystem") ? user.getString("unitSystem")
                                    : "METRIC"
                    );
                    mPrefHelper.putBoolean(ConfigurationConstants.SYNC_DISABLE_REAL_TIME, !user.getBoolean("realTimeMonitoring"));
                }
            });
            return fragment;
        }
    }

    @Override
    public void onBackPressed() {
        if (this.lastBackPressTime < System.currentTimeMillis() - 4000) {
            toast = Toast.makeText(this, getString(R.string.press_back_to_close), Toast.LENGTH_SHORT);
            toast.show();
            this.lastBackPressTime = System.currentTimeMillis();
        }
        else
        {
            if (toast != null)
            {
                toast.cancel();
            }
            finish();
        }
        //super.onBackPressed();
    }
}
