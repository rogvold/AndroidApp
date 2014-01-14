package com.cardiomood.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import com.cardiomood.android.db.dao.HeartRateDataItemDAO;
import com.cardiomood.android.db.dao.HeartRateSessionDAO;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.db.model.HeartRateSession;
import com.cardiomood.android.dialogs.SaveAsDialog;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.List;

public class SessionDetailsActivity extends Activity {

    private static final String TAG = "CardioMood.SessionDetailsActivity";

    public static final String SESSION_ID_EXTRA = "com.cardiomood.android.SessionDetailsActivity.SESSION_ID";
    public static final String POST_RENDER_ACTION_EXTRA = "com.cardiomood.android.SessionDetailsAcrivity.POST_RENDER_ACTION";

    public static final int DO_NOTHING_ACTION = 0;
    public static final int RENAME_ACTION = 1;

    private long sessionId = 0;
    private int postRenderAction;
    private WebView webView;
    private HeartRateSessionDAO sessionDAO;
    private HeartRateDataItemDAO hrDAO;
    private ProgressDialog pDialog;
    private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
    private boolean savingInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);

        sessionId = getIntent().getLongExtra(SESSION_ID_EXTRA, 0);
        postRenderAction = getIntent().getIntExtra(POST_RENDER_ACTION_EXTRA, DO_NOTHING_ACTION);
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

        webView = (WebView) findViewById(R.id.webView);

        refreshData();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveAsItem = menu.findItem(R.id.menu_save_as);
        saveAsItem.setEnabled(!savingInProgress);

        return super.onPrepareOptionsMenu(menu);
    }

    private void refreshData() {
        initWebView();
    }

    private void initWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.endsWith("details.html")) {
                    new DataLoadingTask().execute(sessionId);
                }
            }
        });
        webView.loadUrl(getString(R.string.asset_details_html));
    }

    private void executePostRenderAction() {
        if (postRenderAction == DO_NOTHING_ACTION)
            return;
        if (postRenderAction == RENAME_ACTION) {
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
                                dao.update(session);
                                Toast.makeText(SessionDetailsActivity.this, R.string.session_renamed, Toast.LENGTH_SHORT).show();

                                String name = session.getName();
                                if (name == null || name.isEmpty()) {
                                    name = getText(R.string.dafault_measurement_name) + "#" + sessionId;
                                }
                                name = name.replace("\\", "\\\\").replace("\"", "&quote;").replace("<", "&lt;").replace(">", "&gt;");
                                execJS("setTitle(\"" + name + "\", \"" + dateFormat.format(session.getDateStarted()) + "\")");
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
        new AsyncTask<Void, Void, Void>(){
            @Override
            public Void doInBackground(Void... params) {
                SystemClock.sleep(4000);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                try {
                    if (pDialog != null) {
                        pDialog.dismiss();
                        pDialog = null;
                    }

                    executePostRenderAction();
                } catch (Exception e) {
                    Log.e(TAG, "removeProgressDialog.onPostExecute() - exception", e);
                }
            }
        }.execute();
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
            case R.id.menu_refresh:
                refreshData();
                return true;
            case R.id.menu_rename:
                showRenameSessionDialog();
                return true;
            case R.id.menu_save_as:
                showSaveAsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSaveAsDialog() {
        SaveAsDialog dlg = new SaveAsDialog(this, sessionId, webView);
        dlg.setTitle(R.string.save_as_dlg_title);
        dlg.setSavingCallback(new SaveAsDialog.SavingCallback() {

            @Override
            public void onBeginSave() {
               savingInProgress = true;
               invalidateOptionsMenu();
            }

            @Override
            public void onEndSave() {
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

    private class DataLoadingTask extends AsyncTask<Long, Void, String> {

        @Override
        protected void onPreExecute() {
            Toast.makeText(SessionDetailsActivity.this, getString(R.string.loading_data_for_measurement) + sessionId, Toast.LENGTH_SHORT).show();
            showProgressDialog();
        }

        @Override
        protected String doInBackground(Long... params) {
            List<HeartRateDataItem> items = hrDAO.getItemsBySessionId(sessionId);
            StringBuffer sb = new StringBuffer("$(document).ready(function(){");
            HeartRateSession session = sessionDAO.findById(sessionId);
            String name = session.getName();
            if (name == null || name.isEmpty()) {
                name = getText(R.string.dafault_measurement_name) + " #" + sessionId;
            }
            name = name.replace("\\", "\\\\").replace("\"", "&quote;").replace("<", "&lt;").replace(">", "&gt;");
            sb.append("setTitle(\""+name+"\",\"" + dateFormat.format(session.getDateStarted()) + "\");");
            sb.append("initDetails(new Array(");
            sb.append(items.get(0).getRrTime());
            for (int i=1; i<items.size(); i++) {
                sb.append(",").append(items.get(i).getRrTime());
            }
            sb.append("));})");
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            execJS(s);
            Log.d(TAG, "execJS: " + s);
            removeProgressDialog();
        }
    }

    private void execJS(final String js) {
        runOnUiThread(new Runnable() {
            @Override
            @SuppressWarnings("NewApi")
            public void run() {
                Log.d(TAG, "execJS(): js = " + js);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(js, null);
                } else {
                    webView.loadUrl("javascript:" + js);
                }
            }
        });
    }

}
