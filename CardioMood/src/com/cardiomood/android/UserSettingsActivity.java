package com.cardiomood.android;

import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.tools.PreferenceActivityBase;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;


public class UserSettingsActivity extends PreferenceActivityBase<UserSettingsActivity.UserSettingsFragment> {
	
	private UserSettingsFragment fragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		fragment = new UserSettingsFragment();
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, fragment).commit();
	}
	
	@Override
	public UserSettingsFragment createMainFragment() {
		return new UserSettingsFragment();
	}



	public static class UserSettingsFragment extends PreferenceActivityBase.AbstractMainFragment
											 implements ConfigurationConstants {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.user_settings);
			
			refreshSummaries();
		}
		
		private void refreshSummaries() {
			// no preferences to refresh
			updatePrefSummary(new Preference[]{});
		}
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			refreshSummaries();
		}
		
	}
}
