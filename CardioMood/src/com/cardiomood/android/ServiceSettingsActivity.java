package com.cardiomood.android;

import com.cardiomood.android.ServiceSettingsActivity.ServiceSettingsFragment;
import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.tools.PreferenceActivityBase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;


public class ServiceSettingsActivity extends PreferenceActivityBase<ServiceSettingsFragment> {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onBackPressed() {
		startActivity(new Intent(this, LoginActivity.class));
	}
	
	@Override
	public ServiceSettingsFragment createMainFragment() {
		return new ServiceSettingsFragment();
	}

	public static class ServiceSettingsFragment extends PreferenceActivityBase.AbstractMainFragment 
												implements ConfigurationConstants {
		
		private EditTextPreference mProtocolPref;
		private EditTextPreference mHostPref;
		private EditTextPreference mPortPref;
		private EditTextPreference mPathPref;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.service_settings);
			
			mProtocolPref = (EditTextPreference) findPreference(SERVICE_PROTOCOL);
			mHostPref = (EditTextPreference) findPreference(SERVICE_HOST);
			mPortPref = (EditTextPreference) findPreference(SERVICE_PORT);
			mPathPref = (EditTextPreference) findPreference(SERVICE_PATH);
			
			refreshSummaries();
		}
		
		private void refreshSummaries() {
			updatePrefSummary(mProtocolPref, mHostPref, mPortPref, mPathPref);
		}
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			refreshSummaries();
		}
		
	}
}
