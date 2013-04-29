package com.cardiomood.android;

import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.config.ConfigurationManager;
import com.omnihealth.client_server_interaction.Server;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	private Toast toast;
	private long lastBackPressTime = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		findViewById(R.id.profile_button).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, MyProfileActivity.class));
			}
		});
		findViewById(R.id.my_sessions_button).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, SessionListActivity.class));
			}
		});
		findViewById(R.id.new_session_button).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, MonitorActivity.class));
			}
		});
		findViewById(R.id.click_me_button).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, TestWebActivity.class));
			}
		});
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

}
