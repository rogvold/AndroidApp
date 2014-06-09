package com.cardiomood.android;

import android.app.Dialog;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.cardiomood.android.components.CustomViewPager;
import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.entity.UserEntity;
import com.cardiomood.android.db.entity.UserStatus;
import com.cardiomood.android.dialogs.AboutDialog;
import com.cardiomood.android.dialogs.WhatsNewDialog;
import com.cardiomood.android.fragments.ConnectionFragment;
import com.cardiomood.android.fragments.HistoryFragment;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.tools.fragments.ProfileFragment;
import com.cardiomood.data.CardioMoodServer;
import com.cardiomood.data.DataServiceHelper;
import com.cardiomood.data.async.ServerResponseCallbackRetry;
import com.cardiomood.data.json.JSONError;
import com.cardiomood.data.json.UserProfile;
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.util.Locale;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, ConfigurationConstants {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toast toast;
    private long lastBackPressTime = 0;
    private boolean whatsNewDialogShown = false;

    private PreferenceHelper mPrefHelper;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    CustomViewPager mViewPager;

    private DatabaseHelper databaseHelper;
    private DataServiceHelper dataServiceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefHelper = new PreferenceHelper(this, true);
        dataServiceHelper = new DataServiceHelper(CardioMoodServer.INSTANCE.getService(), mPrefHelper);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

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

        mViewPager.post(new Runnable() {
            @Override
            public void run() {
                CommonTools.hideSoftInputKeyboard(MainActivity.this);
                actionBar.setSelectedNavigationItem(1);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, ConfigurationConstants.FLURRY_API_KEY);
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showWhatsNewDialog();
                }
            });
        }

        invalidateOptionsMenu();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
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
                 FlurryAgent.logEvent("menu_settings_clicked");
                 startActivity(new Intent(this, SettingsActivity.class));
                 return true;
            case R.id.menu_bt_settings:
                FlurryAgent.logEvent("menu_bt_settings_clicked");
                openBluetoothSettings();
                return true;
            case R.id.menu_feedback:
                FlurryAgent.logEvent("menu_feedback_clicked");
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;
            case R.id.menu_about:
                FlurryAgent.logEvent("menu_about_clicked");
                showAboutDialog();
                return true;
            case R.id.menu_logout:
                FlurryAgent.logEvent("menu_logout_clicked");
                logout();
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
        mPrefHelper.putBoolean(USER_LOGGED_IN, false);
        mPrefHelper.putString(USER_EMAIL_KEY, mPrefHelper.getString(USER_EMAIL_KEY));
        mPrefHelper.remove(USER_PASSWORD_KEY);
        mPrefHelper.remove(USER_ACCESS_TOKEN_KEY);
        mPrefHelper.remove(USER_EXTERNAL_ID);
        mPrefHelper.remove(USER_SEX_KEY);
        mPrefHelper.remove(USER_ID);
        mPrefHelper.remove(USER_WEIGHT_KEY);
        mPrefHelper.remove(USER_HEIGHT_KEY);
        mPrefHelper.remove(USER_PHONE_NUMBER_KEY);
        mPrefHelper.remove(USER_FIRST_NAME_KEY);
        mPrefHelper.remove(USER_LAST_NAME_KEY);
        mPrefHelper.remove(USER_BIRTH_DATE_KEY);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
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
                case 1: return new ConnectionFragment();
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

        private UserEntity saveProfileLocally() {
            UserEntity user = new UserEntity();
            user.setId(mPrefHelper.getLong(USER_ID));
            user.setExternalId(mPrefHelper.getLong(USER_EXTERNAL_ID));
            user.setEmail(mPrefHelper.getString(USER_EMAIL_KEY));
            user.setPassword(CommonTools.SHA256(mPrefHelper.getString(USER_PASSWORD_KEY)));
            user.setFirstName(mPrefHelper.getString(USER_FIRST_NAME_KEY));
            user.setLastName(mPrefHelper.getString(USER_LAST_NAME_KEY));
            user.setBirthDate(mPrefHelper.getLong(USER_BIRTH_DATE_KEY));
            user.setWeight(mPrefHelper.getFloat(USER_WEIGHT_KEY));
            user.setHeight(mPrefHelper.getFloat(USER_HEIGHT_KEY));
            user.setPhoneNumber(mPrefHelper.getString(USER_PHONE_NUMBER_KEY));
            user.setGender(mPrefHelper.getString(USER_SEX_KEY, "UNSPECIFIED"));
            user.setLastModified(System.currentTimeMillis());
            user.setStatus(UserStatus.NEW);

            // save user data locally
            RuntimeExceptionDao<UserEntity, Long> userDAO = getHelper().getRuntimeExceptionDao(UserEntity.class);
            userDAO.update(user);

            Log.w(TAG, "saveProfileLocally(): User has been updated: user = " + user);

            return user;
        }

        private void saveProfileRemotely(UserEntity user) {
            final UserProfile profile = new UserProfile();
            profile.setId(user.getExternalId());
            profile.setFirstName(user.getFirstName());
            profile.setLastName(user.getLastName());
            profile.setLastModificationDate(user.getLastModified());
            profile.setBirthTimestamp(user.getBirthDate());
            profile.setGender(UserProfile.Gender.valueOf(user.getGender() == null ? "UNSPECIFIED" : user.getGender()));
            profile.setWeight(user.getWeight() == null ? null : user.getWeight().doubleValue());
            profile.setHeight(user.getHeight() == null ? null : user.getHeight().doubleValue());
            profile.setPhoneNumber(user.getPhoneNumber());

            dataServiceHelper.updateUserProfile(profile, new ServerResponseCallbackRetry<String>() {
                @Override
                public void retry() {
                    dataServiceHelper.updateUserProfile(profile, this);
                }

                @Override
                public void onResult(String result) {
                    Log.w(TAG, "saveProfileRemotely(): result="+result);
                    Toast.makeText(MainActivity.this, "Profile successfully updated", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(JSONError error) {
                    Log.w(TAG, "saveProfileRemotely() failed: error = " + error);
                    Toast.makeText(MainActivity.this, "Failed to sync profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private ProfileFragment createProfileFragment() {
            final ProfileFragment fragment = new ProfileFragment();
            fragment.setCallback(new ProfileFragment.Callback() {
                @Override
                public void onSave() {
                    UserEntity user = saveProfileLocally();
                    //saveProfileRemotely(user);
                }

                @Override
                public void onSync() {
                    dataServiceHelper.getUserProfile(new ServerResponseCallbackRetry<UserProfile>() {
                        @Override
                        public void retry() {
                            dataServiceHelper.getUserProfile(this);
                        }

                        @Override
                        public void onResult(UserProfile result) {
                            RuntimeExceptionDao<UserEntity, Long> userDAO = getHelper().getRuntimeExceptionDao(UserEntity.class);
                            UserEntity user = userDAO.queryForId(mPrefHelper.getLong(USER_ID));
                            if (user != null) {
                                long lastModified = user.getLastModified();
                                if (result.getLastModificationDate() != null && lastModified < result.getLastModificationDate()) {
                                    // updated remotely
                                    mPrefHelper.putString(USER_FIRST_NAME_KEY, result.getFirstName());
                                    mPrefHelper.putString(USER_LAST_NAME_KEY, result.getLastName());
                                    mPrefHelper.putFloat(USER_WEIGHT_KEY, result.getWeight() == null ? null : result.getWeight().floatValue());
                                    mPrefHelper.putFloat(USER_HEIGHT_KEY, result.getHeight() == null ? null : result.getHeight().floatValue());
                                    mPrefHelper.putLong(USER_BIRTH_DATE_KEY, result.getBirthTimestamp());
                                    mPrefHelper.putString(USER_SEX_KEY, result.getGender() == null ? null : result.getGender().toString());
                                    mPrefHelper.putString(USER_PHONE_NUMBER_KEY, result.getPhoneNumber());
                                    fragment.reloadData();
                                    saveProfileLocally();
                                    Toast.makeText(MainActivity.this, "Profile successfully updated", Toast.LENGTH_SHORT).show();
                                } else {
                                    // updated locally
                                    saveProfileRemotely(user);
                                }
                            }
                        }

                        @Override
                        public void onError(JSONError error) {
                            Toast.makeText(MainActivity.this, "Failed to sync profile. " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
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
    }
}
