package com.cardiomood.android;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.cardiomood.android.bluetooth.HeartRateMonitor;
import com.cardiomood.android.config.ConfigurationManager;
import com.cardiomood.android.db.model.HeartRateDataItem;
import com.cardiomood.android.tools.HeartRateDataHandler;

public class WebMonitorActivity extends Activity {

	private static final String TAG = "CardioMood.WebMonitorActivity";
	
	// Bluetooth Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;
	
	// context params
	private WebView webView;
	private Context context;
//	private long lastBackPressTime = 0;
//    private Toast toast = null;
	
	private HeartRateMonitor hrMonitor;
	private BlockingQueue<HeartRateDataItem> heartRateDataQueue;
	private Thread heartRateDataHandler;
	
	@SuppressLint("SetJavaScriptEnabled")
	private void initWebView() {
		webView = (WebView) findViewById(R.id.webView1);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
		webView.setWebViewClient(new WebViewClient());
		webView.setWebChromeClient(new WebChromeClient());
		webView.loadUrl("file:///android_asset/www/monitor.html");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_monitor);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		initWebView();
		
		this.context = getApplicationContext();
		this.hrMonitor = new HeartRateMonitor(this);
		this.hrMonitor.setCallback(new HeartRateMonitor.Callback() {
			
			@Override
			public void onSensorLocationChange(int oldLocation, int newLocation) {
				webView.loadUrl("javascript:updateDeviceLocation(" + oldLocation + "," + newLocation +")");
				//mUIUpdateHandler.sendEmptyMessage(0);
			}
			
			@Override
			public void onHRDataRecieved(int heartBeatsPerMinute, int energyExpended, short[] rrIntervals) {
				saveHeartRateData(heartBeatsPerMinute, energyExpended, rrIntervals);
				//mUIUpdateHandler.sendEmptyMessage(0);
				// TODO: send data to webView
				
			}
			
			@Override
			public void onConnectionStateChange(int oldState, int newState) {
				webView.loadUrl("javascript:updateConnectionStatus(" + oldState + "," + newState +")");
				//mUIUpdateHandler.sendEmptyMessage(0);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_web_monitor, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	    Log.i(TAG, "onStart()");

		try {
			hrMonitor.initBLEService();
			heartRateDataQueue = new SynchronousQueue<HeartRateDataItem>(true);
			
			heartRateDataHandler = new HeartRateDataHandler(context, heartRateDataQueue);
			heartRateDataHandler.start();
		} catch (Exception ex) {
			Toast.makeText(this, "Cannot start Bluetooth.", Toast.LENGTH_SHORT);
			finish();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        Log.i(TAG, "onResume()");

		// TODO: If BT HR Monitor is setup but not started, then start it.
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause()");
	}

	@Override
	protected void onStop() {
		super.onStop();
	    Log.i(TAG, "onStop()");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy()");

		hrMonitor.disconnect();
		hrMonitor.cleanup();
		if (heartRateDataHandler != null && !heartRateDataHandler.isInterrupted()) {
			heartRateDataHandler.interrupt();
			heartRateDataHandler = null;
		}
		if (heartRateDataQueue != null) {
			heartRateDataQueue = null;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    Log.i(TAG, "onActivityResult() code: " + resultCode);

		switch (requestCode) {

		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				// TODO: Set up BT HR Monitor
			} else {
				Log.e(TAG, "onActivityResult(): BT not enabled");
				Toast.makeText(this, "Bluetooth is not enabled. Leaving...",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		}
	}
	
	private void saveHeartRateData(int heartBeatsPerSecond, int energyExpended, short[] rrIntervals) {
		try {
			// TODO: send data to webView
			for (short rr: rrIntervals) {
				heartRateDataQueue.put(new HeartRateDataItem(heartBeatsPerSecond, (int) (rr*(1.0/1024*1000))));
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	

	public class JavaScriptInterface {
        Context mContext;
        HashMap<String, JSONObject> mObjectsFromJS = new HashMap<String, JSONObject>();
        
        /** Instantiate the interface and set the context */
        JavaScriptInterface(Context c) {
            mContext = c;
        }

        public void showToast(String msg) {
            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        }
        
        public void startActivity(String className) {
        	try {
        		mContext.startActivity(new Intent(WebMonitorActivity.this, Class.forName(className)));
        	} catch (Exception ex) {
        		showToast("Unexpected exception: " + ex);
        	}
        }
        
        public String getConfigValue(String key, String defaultValue) {
        	return ConfigurationManager.getInstance().getString(key, defaultValue);
        }
        
        public String getCurrentDeviceName() {
        	return hrMonitor.getDevice() == null ? null : hrMonitor.getDevice().getName();
        }
        
        public String getCurrentDeviceLocation() {
        	return HeartRateMonitor.getStringSensorLocation(hrMonitor.getSensorLocation());
        }
        
        public String getCurrentDeviceChargeLevel() {
        	// TODO: this should return a valid value
        	return "Charge Level: " + hrMonitor.getEnergyExpended();
        }
        
        public String getCurrentHeartRate() {
        	return hrMonitor.getHeartBeatsPerMinute() + "";
        }
        
        public String getCurrentDeviceStatus() {
        	return HeartRateMonitor.getStatusAsText(hrMonitor.getConnectionStatus());
        }
        
        public void initBluetooth() {
        	hrMonitor.initBLEService();
        }
        
        public void disconnect() {
			hrMonitor.disconnect();
        }
        
        public void disconnectAndCleanup() {
        	hrMonitor.disconnect();
        	hrMonitor.cleanup();
        }
        
        public void connect() {
        	try {
        		hrMonitor.attemptConnection();
        	} catch (Exception ex) {
        		Log.d(TAG, "connect(): "+ex);
        		ex.printStackTrace();
        	}
        }
        
        public void navigateUp() {
        	NavUtils.navigateUpFromSameTask(WebMonitorActivity.this);
        }
        
        public void passObject(String name, String json) {
            try {
            	mObjectsFromJS.put(name, new JSONObject(json));
            } catch (JSONException ex) {
            	showToast("Unexpected exception: " + ex);
            }
        }
        
    }

}
