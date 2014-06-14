package com.cardiomood.android;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.components.CustomViewPager;
import com.cardiomood.android.db.DatabaseHelper;
import com.cardiomood.android.db.entity.ContinuousSessionEntity;
import com.cardiomood.android.db.entity.RRIntervalEntity;
import com.cardiomood.android.db.entity.SessionStatus;
import com.cardiomood.android.fragments.details.AbstractSessionReportFragment;
import com.cardiomood.android.fragments.details.HistogramReportFragment;
import com.cardiomood.android.fragments.details.OrganizationAReportFragment;
import com.cardiomood.android.fragments.details.OveralSessionReportFragment;
import com.cardiomood.android.fragments.details.ScatterogramReportFragment;
import com.cardiomood.android.fragments.details.SpectralAnalysisReportFragment;
import com.cardiomood.android.fragments.details.StressIndexReportFragment;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.data.CardioMoodServer;
import com.cardiomood.data.DataServiceHelper;
import com.cardiomood.data.async.ServerResponseCallbackRetry;
import com.cardiomood.data.json.CardioSession;
import com.cardiomood.data.json.JSONError;
import com.flurry.android.FlurryAgent;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SessionDetailsActivity extends ActionBarActivity implements ActionBar.TabListener {

    private static final String TAG = "CardioMood.SessionDetailsActivity";

    public static final String SESSION_ID_EXTRA = "com.cardiomood.android.SessionDetailsActivity.SESSION_ID";
    public static final String POST_RENDER_ACTION_EXTRA = "com.cardiomood.android.SessionDetailsAcrivity.POST_RENDER_ACTION";

    public static final int DO_NOTHING_ACTION = 0;
    public static final int RENAME_ACTION = 1;

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

    private long sessionId = 0;
    private int postRenderAction;
    private RuntimeExceptionDao<ContinuousSessionEntity, Long> sessionDAO;
    private RuntimeExceptionDao<RRIntervalEntity, Long> hrDAO;
    private PreferenceHelper pHelper;
    private DataServiceHelper dataServiceHelper;
    private Set<AbstractSessionReportFragment> reportFragments = new HashSet<AbstractSessionReportFragment>();
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);

        postRenderAction = getIntent().getIntExtra(POST_RENDER_ACTION_EXTRA, DO_NOTHING_ACTION);
        sessionId = getIntent().getLongExtra(SESSION_ID_EXTRA, 0);
        if (sessionId == 0) {
            Toast.makeText(this, getText(R.string.nothing_to_view), Toast.LENGTH_SHORT).show();
            finish();
        }

        sessionDAO = getHelper().getRuntimeExceptionDao(ContinuousSessionEntity.class);
        if (sessionDAO.queryForId(sessionId) == null) {
            Toast.makeText(this, MessageFormat.format(getText(R.string.session_doesnt_exist).toString(), sessionId), Toast.LENGTH_SHORT).show();
            finish();
        }

        long count = 0;
        try {
            hrDAO = getHelper().getRuntimeExceptionDao(RRIntervalEntity.class);
            count = hrDAO.queryBuilder().where().eq("session_id", sessionId).countOf();
        } catch (SQLException ex) {
            Log.w(TAG, "onCreate() SQLException", ex);
        }

        if (count < 30) {
            Toast.makeText(this, R.string.measurement_contains_too_few_data, Toast.LENGTH_SHORT).show();
            finish();
        }

        PreferenceHelper prefHelper = new PreferenceHelper(this, true);
        dataServiceHelper = new DataServiceHelper(CardioMoodServer.INSTANCE.getService(), prefHelper);

        pHelper = new PreferenceHelper(this);

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am.getLargeMemoryClass() < 10) {
            Toast.makeText(this, "Low available memory. Possible application crash...", Toast.LENGTH_SHORT).show();
        }

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.addFragment(OveralSessionReportFragment.class, "General Info");
        mSectionsPagerAdapter.addFragment(StressIndexReportFragment.class, "Stress Index");
        mSectionsPagerAdapter.addFragment(OrganizationAReportFragment.class, "Organization \"A\"");
        mSectionsPagerAdapter.addFragment(SpectralAnalysisReportFragment.class, "Spectral Analysis");
        mSectionsPagerAdapter.addFragment(HistogramReportFragment.class, "Histogram");
        mSectionsPagerAdapter.addFragment(ScatterogramReportFragment.class, "Scatterogram");

        // Set up the ViewPager with the sections adapter.
        mViewPager = (CustomViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(6);
        mViewPager.setPagingEnabled(true);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);

                // hide soft input keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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
                            .setTabListener(this)
            );
        }
        actionBar.setSelectedNavigationItem(0);

        //Toast.makeText(this, getString(R.string.loading_data_for_measurement) + sessionId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, ConfigurationConstants.FLURRY_API_KEY);
        executePostRenderAction();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
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


    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final List<AbstractSessionReportFragment> fragments = new ArrayList<AbstractSessionReportFragment>();
        private final List<String> titles = new ArrayList<String>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);

//            // getItem is called to instantiate the fragment for the given page.
//            switch (position) {
//                case 0:
//                    return AbstractSessionReportFragment.newInstance(OveralSessionReportFragment.class, sessionId);
//                case 1:
//                    return AbstractSessionReportFragment.newInstance(StressIndexReportFragment.class, sessionId);
//                case 2:
//                    return AbstractSessionReportFragment.newInstance(OrganizationAReportFragment.class, sessionId);
//                case 3:
//                    return AbstractSessionReportFragment.newInstance(SpectralAnalysisReportFragment.class, sessionId);
//                case 4:
//                    return AbstractSessionReportFragment.newInstance(HistogramReportFragment.class, sessionId);
//                case 5:
//                    return AbstractSessionReportFragment.newInstance(ScatterogramReportFragment.class, sessionId);
//            }
//            return null;
        }

        @Override
        public int getCount() {
            // Show 6 total pages.
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            return titles.get(position).toUpperCase(l);

//            switch (position) {
//                case 0:
//                    return "General Info".toUpperCase(l);
//                case 1:
//                    return "Stress Index".toUpperCase(l);
//                case 2:
//                    return "\"A\" Organization".toUpperCase(l);
//                case 3:
//                    return "Spectral Analysis".toUpperCase(l);
//                case 4:
//                    return "Histogram".toUpperCase(l);
//                case 5:
//                    return "Scatterogram".toUpperCase(l);
//            }
//            return null;
        }

        public void addFragment(Class<? extends AbstractSessionReportFragment> clazz, String title) {
            fragments.add(AbstractSessionReportFragment.newInstance(clazz, sessionId));
            titles.add(title);
        }

        public void clear() {
            fragments.clear();
            titles.clear();
        }
    }

    private void executePostRenderAction() {
        switch (postRenderAction) {
            case DO_NOTHING_ACTION: {
                FlurryAgent.logEvent("old_session_opened");
                return;
            }
            case RENAME_ACTION: {
                FlurryAgent.logEvent("new_session_opened");
                showRenameSessionDialog();
            }
        }
        postRenderAction = DO_NOTHING_ACTION;
    }

    private void showRenameSessionDialog() {

        getIntent().putExtra(POST_RENDER_ACTION_EXTRA, DO_NOTHING_ACTION);

        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_input_text, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        userInput.setText(sessionDAO.queryForId(sessionId).getName());

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                final ContinuousSessionEntity session = sessionDAO.queryForId(sessionId);
                                String newName = userInput.getText() == null ? "" : userInput.getText().toString();
                                newName = newName.trim();
                                if (newName.isEmpty())
                                    newName = null;
                                session.setName(newName);
                                session.setLastModified(System.currentTimeMillis());
                                if (session.getStatus() == SessionStatus.SYNCHRONIZED)
                                    session.setStatus(SessionStatus.COMPLETED);
                                sessionDAO.update(session);
                                Toast.makeText(SessionDetailsActivity.this, R.string.session_renamed, Toast.LENGTH_SHORT).show();
                                if (!"SYNC_ON_DEMAND".equals(pHelper.getString(ConfigurationConstants.SYNC_STRATEGY, "SYNC_WHEN_MODIFIED"))) {
                                    if (session.getExternalId() != null) {
                                        dataServiceHelper.updateSessionInfo(session.getExternalId(), session.getName(), session.getDescription(), new ServerResponseCallbackRetry<CardioSession>() {
                                            @Override
                                            public void retry() {
                                                dataServiceHelper.updateSessionInfo(sessionId, session.getName(), session.getDescription(), this);
                                            }

                                            @Override
                                            public void onResult(CardioSession result) {
                                                session.setStatus(SessionStatus.SYNCHRONIZED);
                                                session.setName(result.getName());
                                                session.setDescription(result.getDescription());
                                                sessionDAO.update(session);
                                            }

                                            @Override
                                            public void onError(JSONError error) {
                                                Log.d(TAG, "updateSessionInfo failed, error=" + error);
                                            }
                                        });
                                    }
                                }
                                String name = session.getName();
                                if (name == null || name.isEmpty()) {
                                    name = getText(R.string.dafault_measurement_name) + "# " + sessionId;
                                }
                                TextView tv = (TextView) findViewById(R.id.session_title);
                                if (tv != null)
                                    tv.setText(name);
                                FlurryAgent.logEvent("session_renamed");
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }
                )
                .setTitle(R.string.rename_session);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.session_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menu_rename:
                FlurryAgent.logEvent("menu_rename_clicked");
                showRenameSessionDialog();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_remove_artifacts:
                removeArtifacts();
                return true;
            case R.id.menu_undo_remove_artifacts:
                undoRemoveArtifacts();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_rename);
        item.setEnabled(!isPredefinedSession(sessionId));
        return super.onPrepareOptionsMenu(menu);
    }

    public void registerFragment(AbstractSessionReportFragment fragment) {
        reportFragments.add(fragment);
    }

    public void unregisterFragment(AbstractSessionReportFragment fragment) {
        reportFragments.remove(fragment);
    }

    private DatabaseHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return databaseHelper;
    }

    private void removeArtifacts() {
        for (AbstractSessionReportFragment fragment: reportFragments) {
            fragment.removeArtifacts();
        }
    }

    private void undoRemoveArtifacts() {
        for (AbstractSessionReportFragment fragment: reportFragments) {
            fragment.undoRemoveArtifacts();
        }
    }

    private boolean isPredefinedSession(long sessionId) {
        List<Long> ids = Arrays.asList(
                pHelper.getLong(ConfigurationConstants.DB_GOOD_SESSION_ID),
                pHelper.getLong(ConfigurationConstants.DB_BAD_SESSION_ID),
                pHelper.getLong(ConfigurationConstants.DB_ATHLETE_SESSION_ID),
                pHelper.getLong(ConfigurationConstants.DB_STRESSED_SESSION_ID)
        );
        return ids.contains(sessionId);
    }
}
