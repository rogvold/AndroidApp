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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.db.entity.SessionEntity;
import com.cardiomood.android.fragments.details.AbstractSessionReportFragment;
import com.cardiomood.android.fragments.details.HistogramReportFragment;
import com.cardiomood.android.fragments.details.OrganizationAReportFragment;
import com.cardiomood.android.fragments.details.OveralSessionReportFragment;
import com.cardiomood.android.fragments.details.ScatterogramReportFragment;
import com.cardiomood.android.fragments.details.SpectralAnalysisReportFragment;
import com.cardiomood.android.fragments.details.StressIndexReportFragment;
import com.cardiomood.android.fragments.details.TextReportFragment;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.ui.CustomViewPager;
import com.flurry.android.FlurryAgent;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import timber.log.Timber;

public class SessionDetailsActivity extends ActionBarActivity implements ActionBar.TabListener {

    private static final String TAG = "CardioMood.SessionDetailsActivity";

    public static final String SESSION_ID_EXTRA = "com.cardiomood.android.SessionDetailsActivity.SESSION_ID";
    public static final String POST_RENDER_ACTION_EXTRA = "com.cardiomood.android.SessionDetailsAcrivity.POST_RENDER_ACTION";

    public static final int DO_NOTHING_ACTION = 0;
    public static final int RENAME_ACTION = 1;

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

    private long sessionId = 0;
    private SessionEntity session;
    private int postRenderAction;
    private Set<AbstractSessionReportFragment> reportFragments = new HashSet<AbstractSessionReportFragment>();
    private Bus bus;

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
        mSectionsPagerAdapter.addFragment(TextReportFragment.class, "Numbers");

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

        bus = new Bus(ThreadEnforcer.MAIN);

        //Toast.makeText(this, getString(R.string.loading_data_for_measurement) + sessionId, Toast.LENGTH_SHORT).show();
        loadSession();
    }

    @Override
    protected void onStart() {
        bus.register(this);
        super.onStart();
        FlurryAgent.onStartSession(this, ConfigurationConstants.FLURRY_API_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FlurryAgent.onEndSession(this);
        bus.unregister(this);
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

    private void loadSession() {
        Task.callInBackground(new Callable<SessionEntity>() {
            @Override
            public SessionEntity call() throws Exception {
                return DatabaseHelperFactory.getHelper()
                        .getSessionDao()
                        .queryForId(sessionId);
            }
        }).continueWith(new Continuation<SessionEntity, Object>() {
            @Override
            public Object then(Task<SessionEntity> task) throws Exception {
                if (task.isFaulted()) {
                    Timber.w(task.getError(), "Failed to load session with id = " + sessionId);
                    if (!isFinishing()) {
                        Toast.makeText(SessionDetailsActivity.this, "Failed to load session.",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else if (task.isCompleted()) {
                    validateSession(task.getResult());
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void validateSession(final SessionEntity session) {
        if (session == null) {
            Toast.makeText(
                    this,
                    MessageFormat.format(getText(R.string.session_doesnt_exist).toString(), sessionId),
                    Toast.LENGTH_SHORT
            ).show();
            finish();
        }

        // check session data
        Task.callInBackground(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return DatabaseHelperFactory.getHelper()
                        .getCardioItemDao()
                        .queryBuilder()
                        .where().eq("session_id", sessionId)
                        .countOf();
            }
        }).continueWith(new Continuation<Long, Object>() {
            @Override
            public Object then(Task<Long> task) throws Exception {
                if (task.isFaulted()) {
                    Timber.w(task.getError(), "Failed to load session with id = " + sessionId);
                    if (!isFinishing()) {
                        finish();
                    }
                } else if (task.isCompleted()) {
                    long count = task.getResult();
                    if (count < 30 && !isFinishing()) {
                        Toast.makeText(SessionDetailsActivity.this,
                                R.string.measurement_contains_too_few_data, Toast.LENGTH_SHORT).show();
                        finish();
                        return null;
                    }

                    // post event to the bus
                    bus.post(new SessionLoaded(session));
                }
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    @Produce
    public SessionLoaded produceSessionLoaded() {
        return new SessionLoaded(session);
    }

    @Subscribe
    public void onSessionLoaded(SessionLoaded event) {
        if (event.getSession() != null) {
            session = event.getSession();

            // update menu
            invalidateOptionsMenu();
            executePostRenderAction();
        }
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
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            return titles.get(position).toUpperCase(l);
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
        if (session == null) {
            return;
        }

        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_input_text, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        userInput.setText(session.getName());

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                String newName = userInput.getText() == null ? "" : userInput.getText().toString();
                                newName = newName.trim();
                                if (newName.isEmpty())
                                    newName = null;
                                renameSession(session, newName);
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

    private void renameSession(final SessionEntity session, final String newName) {
        final String oldName = session.getName();
        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                session.setName(newName);
                session.setSyncDate(new Date());
                DatabaseHelperFactory.getHelper().getSessionDao().update(session);
                return null;
            }
        }).continueWith(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {
                if (task.isFaulted()) {
                    Timber.w(task.getError(), "rename session failed");
                    if (!isFinishing()) {
                        Toast.makeText(SessionDetailsActivity.this, R.string.failed_to_rename_session, Toast.LENGTH_SHORT).show();
                    }
                    session.setName(oldName);
                } else if (task.isCompleted()) {
                    if (!isFinishing()) {
                        Toast.makeText(SessionDetailsActivity.this, R.string.session_renamed, Toast.LENGTH_SHORT).show();
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
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.session_details, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem renameItem = menu.findItem(R.id.menu_rename);
        renameItem.setEnabled(session != null);
        return super.onPrepareOptionsMenu(menu);
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

    public void registerFragment(AbstractSessionReportFragment fragment) {
        reportFragments.add(fragment);
    }

    public void unregisterFragment(AbstractSessionReportFragment fragment) {
        reportFragments.remove(fragment);
    }

    public Bus getBus() {
        return bus;
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

    public static class SessionLoaded {
        private final SessionEntity session;

        public SessionLoaded(SessionEntity session) {
            this.session = session;
        }

        public SessionEntity getSession() {
            return session;
        }
    }
}
