package com.cardiomood.sport.android;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.sport.android.fragments.CurrentWorkoutInfoFragment;
import com.cardiomood.sport.android.fragments.ProfileFragment;
import com.cardiomood.sport.android.tools.config.ConfigurationConstants;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements ConfigurationConstants {

    private TabsAdapter mAdapter;
    private ViewPager mPager;

    private Toast toast;
    private long lastBackPressTime = 0;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mAdapter = new TabsAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {

            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                mPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // ignore
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // ignore
            }
        };

        // Add 3 tabs, specifying the tab's text and TabListener
        for (int i = 0; i < mAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAdapter.getItemTitle(i))
                            .setTabListener(tabListener));
        }

        mPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setSelectedNavigationItem(position);
                    }
                });

        // Check if enabled and if not send user to the GPS settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings
//        if (!enabled) {
//            Toast.makeText(this, "Please, allow access to the GPS location provider.", Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            startActivity(intent);
//        }
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
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                logout();
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, ServiceSettingsActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void logout() {
        PreferenceHelper pref = new PreferenceHelper(this.getApplicationContext());
        pref.setPersistent(true);
        pref.putBoolean(USER_LOGGED_IN, false);
        pref.putString(USER_PASSWORD_KEY, null);
        pref.putString(USER_FIRST_NAME_KEY, null);
        pref.putString(USER_LAST_NAME_KEY, null);
        pref.putString(USER_PHONE_NUMBER_KEY, null);
        pref.putString(USER_EXTERNAL_ID, null);
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class TabsAdapter extends FragmentPagerAdapter {

        public static final int NUM_ITEMS = 4;

        public TabsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            return TabFragments.newInstance(position);
        }

        public int getItemTitle(int position) {
            return TabFragments.getTitle(position);
        }
    }

    public static class TabFragments {

        private static final List<Class<? extends Fragment>> fragments = new ArrayList<Class<? extends Fragment>>() {
            {
                add(ProfileFragment.class);
                add(CurrentWorkoutInfoFragment.class);
                add(Fragment.class);
                add(Fragment.class);
            }
        };

        private static final List<Integer> tabNames = new ArrayList<Integer>() {
            {
                add(R.string.tab_profile);
                add(R.string.tab_current_workout);
                add(R.string.tab_history);
                add(R.string.tab_about);
            }
        };

        public static Fragment newInstance(int num) {
            try {
                if (num >= 0 && num < fragments.size())
                    return fragments.get(num).newInstance();
            } catch (Exception ex) {
                return null;
            }
            return null;
        }

        public static int getTitle(int num) {
            if (num >= 0 && num < fragments.size())
                return tabNames.get(num);
            return R.string.empty_string;
        }

    }
}
