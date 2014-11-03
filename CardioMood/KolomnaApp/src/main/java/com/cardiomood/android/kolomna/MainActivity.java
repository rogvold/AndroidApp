package com.cardiomood.android.kolomna;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cardiomood.android.kolomna.fragments.AboutFragment;
import com.cardiomood.android.kolomna.fragments.EditParseUserFragment;
import com.cardiomood.android.kolomna.fragments.HistoryFragment;
import com.cardiomood.android.kolomna.fragments.NavigationDrawerFragment;
import com.cardiomood.android.kolomna.fragments.NewMeasurementFragment;
import com.cardiomood.android.kolomna.tools.Constants;
import com.cardiomood.android.tools.PreferenceHelper;
import com.parse.ParseUser;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private PreferenceHelper pHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if signed in
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            performLogout();
            return;
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        pHelper = new PreferenceHelper(this, true);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section0);
                break;
            case 2:
                mTitle = getString(R.string.title_section1);
                break;
            case 3:
                mTitle = getString(R.string.title_section2);
                break;
            case 4:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.activity_main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.menu_logout) {
            performLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void performLogout() {
        pHelper.putLong(Constants.APP_LAST_SYNC_TIMESTAMP, 0L);
        ParseUser.logOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of fragment for the given section number.
         */
        public static Fragment newInstance(int sectionNumber) {
            switch (sectionNumber) {
                case 1:
                    return new EditParseUserFragment();
                case 2:
                    return new NewMeasurementFragment();
                case 3:
                    return new HistoryFragment();
                case 4:
                    return new AboutFragment();
                default:
                    PlaceholderFragment fragment = new PlaceholderFragment();
                    Bundle args = new Bundle();
                    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
                    fragment.setArguments(args);
                    return fragment;
            }

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}
