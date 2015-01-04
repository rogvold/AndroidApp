package com.cardiomood.android;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.expert.R;

import java.io.File;


public class ReportPreviewActivity extends ActionBarActivity {

    private static final String TAG = ReportPreviewActivity.class.getSimpleName();

    public static final String EXTRA_FILE_PATH = "com.cardiomood.android.extra.REPORT_FILE_PATH";

    private TextView tvPath;
    private WebView webView;

    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_preview);

        //getActionBar().setDisplayHomeAsUpEnabled(true);

        filePath = getIntent().getStringExtra(EXTRA_FILE_PATH);
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, R.string.file_doesnt_exist, Toast.LENGTH_SHORT);
            finish();
        }

        if (!file.canRead()) {
            Toast.makeText(this, R.string.file_cannot_be_read, Toast.LENGTH_SHORT);
            finish();
        }

        webView = (WebView) findViewById(R.id.webView);
        tvPath = (TextView) findViewById(R.id.pathToFile);

        tvPath.setText(file.getAbsolutePath());
        WebSettings settings = webView.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        webView.loadUrl("file://" + file.getAbsolutePath());
        webView.getSettings().setDefaultFontSize(12);
        webView.getSettings().setDefaultFixedFontSize(12);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_report_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_delete:
                deleteAndClose();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAndClose() {
        try {
            if (new File(filePath).delete()) {
                Toast.makeText(this, R.string.file_has_been_deleted, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.unable_to_delete_file, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            Toast.makeText(this, R.string.unable_to_delete_file, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "deleteAndClose() failed", ex);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
