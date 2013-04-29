package com.cardiomood.android;

import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.config.ConfigurationManager;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebMainActivity extends Activity {

	private Toast toast;
	private long lastBackPressTime = 0;
	private WebView webView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_main);
		
		initWebView();
	}
	
	@SuppressLint("SetJavaScriptEnabled")
	private void initWebView() {
		webView = (WebView) findViewById(R.id.webView1);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
		webView.setWebViewClient(new WebViewClient());
		webView.setWebChromeClient(new WebChromeClient());
		webView.loadUrl("file:///android_asset/www/main.html");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (!isLoggedIn()) {
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == null) {
			return true;
		}
		switch (item.getItemId()) {
		case R.id.menu_logout:
			performLogOut();
			return true;
		case R.id.menu_settings:
			startActivity(new Intent(this, UserSettingsActivity.class));
		default: return true;
		}
	}
	
	protected boolean isLoggedIn() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		return sharedPref.getBoolean("loggedIn", false);
	}
	
	protected void performLogOut() {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean("loggedIn", false);
		editor.apply();
		// start LoginActivity
		Intent loginIntent = new Intent(this, LoginActivity.class);
		loginIntent.putExtra(LoginActivity.EXTRA_EMAIL, 
				ConfigurationManager.getInstance().getString(ConfigurationConstants.USER_EMAIL_KEY));
		startActivity(loginIntent);
	}
	
	@Override
	public void onBackPressed() {
        if (this.lastBackPressTime < System.currentTimeMillis() - 4000) {
            toast = Toast.makeText(this, "Press BACK once more to close this application", Toast.LENGTH_SHORT);
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
	
	public class JavaScriptInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        JavaScriptInterface(Context c) {
            mContext = c;
        }

        public void showToast(String msg) {
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
        
        public void startActivity(String className) {
        	try {
        		mContext.startActivity(new Intent(WebMainActivity.this, Class.forName(className)));
        	} catch (Exception ex) {
        		showToast("Unexpected exception: " + ex);
        	}
        }
        
        public String getConfigValue(String key, String defaultValue) {
        	return ConfigurationManager.getInstance().getString(key, defaultValue);
        }
        
        public void logOut() {
        	performLogOut();
        }
        
    }
}
