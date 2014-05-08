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
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.cardiomood.android.components.CustomViewPager;
import com.cardiomood.android.dialogs.AboutDialog;
import com.cardiomood.android.dialogs.WhatsNewDialog;
import com.cardiomood.android.fragments.ConnectionFragment;
import com.cardiomood.android.fragments.HistoryFragment;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.tools.fragments.ProfileFragment;
import com.flurry.android.FlurryAgent;

import java.util.Locale;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener, ConfigurationConstants {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefHelper = new PreferenceHelper(getApplicationContext());
        mPrefHelper.setPersistent(true);

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
        mPrefHelper.putString(USER_PASSWORD_KEY, null);
        mPrefHelper.putString(USER_ACCESS_TOKEN_KEY, null);
        mPrefHelper.putString(USER_EXTERNAL_ID, null);
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
                case 0: return new ProfileFragment();
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
