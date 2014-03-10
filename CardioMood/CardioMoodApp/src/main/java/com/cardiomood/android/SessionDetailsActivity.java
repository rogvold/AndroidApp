package com.cardiomood.android;

import android.app.ActivityManager;
import android.app.AlertDialog;
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
import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.db.model.SessionStatus;
import com.cardiomood.android.dialogs.SaveAsDialog;
import com.cardiomood.android.fragments.details.HistogramReportFragment;
import com.cardiomood.android.fragments.details.OveralSessionReportFragment;
import com.cardiomood.android.fragments.details.ScatterogramReportFragment;
import com.cardiomood.android.fragments.details.SpectralAnalysisReportFragment;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.flurry.android.FlurryAgent;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

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
    private HeartRateSessionDAO sessionDAO;
    private HeartRateDataItemDAO hrDAO;
    private ProgressDialog pDialog;

    private boolean savingInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);

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

        // Set up the ViewPager with the sections adapter.
        mViewPager = (CustomViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);
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
        actionBar.setSelectedNavigationItem(0);

        postRenderAction = getIntent().getIntExtra(POST_RENDER_ACTION_EXTRA, DO_NOTHING_ACTION);
        sessionId = getIntent().getLongExtra(SESSION_ID_EXTRA, 0);
        if (sessionId == 0) {
            Toast.makeText(this, getText(R.string.nothing_to_view), Toast.LENGTH_SHORT).show();
            finish();
        }

        sessionDAO = new HeartRateSessionDAO();

        if (! sessionDAO.exists(sessionId)) {
            Toast.makeText(this, MessageFormat.format(getText(R.string.session_doesnt_exist).toString(), sessionId), Toast.LENGTH_SHORT).show();
            finish();
        }

        hrDAO = new HeartRateDataItemDAO();

        //Toast.makeText(this, getString(R.string.loading_data_for_measurement) + sessionId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FlurryAgent.onStartSession(this, ConfigurationConstants.FLURRY_API_KEY);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveAsItem = menu.findItem(R.id.menu_save_as);
        saveAsItem.setEnabled(!savingInProgress);

        return super.onPrepareOptionsMenu(menu);
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

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0: return OveralSessionReportFragment.newInstance(sessionId);
                case 1: return SpectralAnalysisReportFragment.newInstance(sessionId);
                case 2: return HistogramReportFragment.newInstance(sessionId);
                case 3: return ScatterogramReportFragment.newInstance(sessionId);
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return "General Info".toUpperCase(l);
                case 1:
                    return "Spectral Analysis".toUpperCase(l);
                case 2:
                    return "Histogram".toUpperCase(l);
                case 3:
                    return "Scatterogram".toUpperCase(l);
            }
            return null;
        }
    }

    private void executePostRenderAction() {
        if (postRenderAction == DO_NOTHING_ACTION) {
            FlurryAgent.logEvent("old_session_opened");
            return;
        }
        if (postRenderAction == RENAME_ACTION) {
            FlurryAgent.logEvent("new_session_opened");
            showRenameSessionDialog();
        }
        postRenderAction = DO_NOTHING_ACTION;
    }

    private void showRenameSessionDialog() {
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_input_text, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        userInput.setText(sessionDAO.findById(sessionId).getName());

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                HeartRateSessionDAO dao = new HeartRateSessionDAO();
                                HeartRateSession session = dao.findById(sessionId);
                                String newName = userInput.getText() == null ? "" : userInput.getText().toString();
                                newName = newName.trim();
                                if (newName.isEmpty())
                                    newName = null;
                                session.setName(newName);
                                if (session.getStatus() == SessionStatus.SYNCHRONIZED)
                                    session.setStatus(SessionStatus.COMPLETED);
                                dao.update(session);
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
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        })
                .setTitle(R.string.rename_session);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void showProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pDialog = ProgressDialog.show(SessionDetailsActivity.this, getText(R.string.preparing_report), getString(R.string.please_wait), true, false);
            }
        });
    }

    private void removeProgressDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }

        try {
            executePostRenderAction();
        } catch (Exception e) {
            Log.e(TAG, "removeProgressDialog() - exception", e);
        }
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
            case R.id.menu_save_as:
                FlurryAgent.logEvent("menu_save_clicked");
                showSaveAsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSaveAsDialog() {
        SaveAsDialog dlg = new SaveAsDialog(this, sessionId);
        dlg.setTitle(R.string.save_as_dlg_title);
        dlg.setSavingCallback(new SaveAsDialog.SavingCallback() {

            @Override
            public void onBeginSave() {
                savingInProgress = true;
                invalidateOptionsMenu();
            }

            @Override
            public void onEndSave(String fileName) {
                savingInProgress = false;
                invalidateOptionsMenu();
            }

            @Override
            public void onError() {
                savingInProgress = false;
                invalidateOptionsMenu();
            }
        });
        dlg.show();
    }

}
