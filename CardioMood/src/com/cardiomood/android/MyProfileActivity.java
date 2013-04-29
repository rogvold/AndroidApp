package com.cardiomood.android;

import com.cardiomood.android.config.ConfigurationConstants;
import com.cardiomood.android.config.ConfigurationManager;
import com.cardiomood.android.services.CardioMoodService;
import com.cardiomood.android.services.CardioMoodSimpleResponse;
import com.cardiomood.android.services.ICardioMoodService;
import com.cardiomood.android.services.User;
import com.cardiomood.android.tools.PreferenceActivityBase;
import com.google.gson.Gson;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;

public class MyProfileActivity extends PreferenceActivityBase<MyProfileActivity.MyProfileFragment> {

	private MyProfileFragment fragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		fragment = getFragment();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_my_profile, menu);
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
			NavUtils.navigateUpTo(this, new Intent(this,
					SessionListActivity.class));
			return true;
		case R.id.menu_refresh:
			fragment.attemptRefresh();
			return true;
		}
		return true;
	}
	
	public static class MyProfileFragment extends PreferenceActivityBase.AbstractMainFragment implements ConfigurationConstants {
		
		private EditTextPreference mEmailPref;
		private EditTextPreference mFirstNamePref;
		private EditTextPreference mLastNamePref;
		private EditTextPreference mAboutPref;
		private EditTextPreference mDescriptionPref;
		private EditTextPreference mDiagnosisPref;
		private EditTextPreference mStatusPref;
		private EditTextPreference mDepartmentPref;
		private EditTextPreference mWeightPref;
		private EditTextPreference mHeightPref;
		private EditTextPreference mAgePref;
		private ListPreference mSexPref;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			addPreferencesFromResource(R.xml.my_profile);
			
			mEmailPref = (EditTextPreference) findPreference(USER_EMAIL_KEY);
			mFirstNamePref = (EditTextPreference) findPreference(USER_FIRST_NAME_KEY);
			mLastNamePref = (EditTextPreference) findPreference(USER_LAST_NAME_KEY);
			mAboutPref = (EditTextPreference) findPreference(USER_ABOUT_KEY);
			mDescriptionPref = (EditTextPreference) findPreference(USER_DESCRIPTION_KEY);
			mDiagnosisPref = (EditTextPreference) findPreference(USER_DIAGNOSIS_KEY);
			mStatusPref = (EditTextPreference) findPreference(USER_STATUS_KEY);
			mDepartmentPref = (EditTextPreference) findPreference(USER_DEPARTMENT_KEY);
			mWeightPref = (EditTextPreference) findPreference(USER_WEIGHT_KEY);
			mHeightPref = (EditTextPreference) findPreference(USER_HEIGHT_KEY);
			mAgePref = (EditTextPreference) findPreference(USER_AGE_KEY);
			mSexPref = (ListPreference) findPreference(USER_SEX_KEY);
			
			attemptRefresh();
			refreshSummaries();
		}
		
		public void attemptRefresh() {
			ConfigurationManager conf = ConfigurationManager.getInstance();
			RefreshUserTask task = new RefreshUserTask();
			task.execute("h7a7RaRtvAVwnMGq5BV6", conf.getString(USER_EMAIL_KEY), conf.getString(USER_PASSWORD_KEY));
		}
		
		public void attemptApplyUserProfile() {
			try {
				ApplyUserProfileTask task = new ApplyUserProfileTask();
				User user = getUser();
				task.execute("h7a7RaRtvAVwnMGq5BV6", new Gson().toJson(user));
			} catch (NumberFormatException ex) {
				Toast.makeText(getActivity(), "Number format error.", Toast.LENGTH_SHORT).show();
			} catch (Exception ex) {
				// suppress this :)
			}
		}
		
		public User getUser() {
			ConfigurationManager conf = ConfigurationManager.getInstance();
			User user = new User();
			user.setEmail(conf.getString(USER_EMAIL_KEY));
			user.setPassword(conf.getString(USER_PASSWORD_KEY));
			user.setFirstName(mFirstNamePref.getText());
			user.setLastName(mLastNamePref.getText());
			user.setAbout(mAboutPref.getText());
			user.setDepartment(mDepartmentPref.getText());
			user.setDescription(mDescriptionPref.getText());
			user.setDiagnosis(mDiagnosisPref.getText());
			user.setStatusMessage(mStatusPref.getText());
			if (!TextUtils.isEmpty(mHeightPref.getText()))
				user.setHeight(Float.valueOf(mHeightPref.getText()));
			if (!TextUtils.isEmpty(mWeightPref.getText()))
				user.setWeight(Float.valueOf(mWeightPref.getText()));
			if (!TextUtils.isEmpty(mHeightPref.getText()))
				user.setHeight(Float.valueOf(mHeightPref.getText()));
			if (!TextUtils.isEmpty(mAgePref.getText()))
				user.setAge(Integer.valueOf(mAgePref.getText()));
			if (!TextUtils.isEmpty(mSexPref.getValue()))
				user.setSex(Integer.valueOf(mSexPref.getValue()));
			return user;
		}
		
		private void refreshSummaries() {
			updatePrefSummary(
					"Unspecified",
					mEmailPref, 
					mFirstNamePref,
					mLastNamePref,
					mAboutPref,
					mDescriptionPref,
					mDiagnosisPref,
					mStatusPref,
					mDepartmentPref,
					mWeightPref,
					mHeightPref,
					mAgePref,
					mSexPref
					);
		}
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			refreshSummaries();
		    attemptApplyUserProfile();
		}
		
		private void populatePreferences(User user) {
			mEmailPref.setText(user.getEmail());
			mFirstNamePref.setText(user.getFirstName());
			mLastNamePref.setText(user.getLastName());
			mAboutPref.setText(user.getAbout());
			mDescriptionPref.setText(user.getDescription());
			mDiagnosisPref.setText(user.getDiagnosis());
			mStatusPref.setText(user.getStatusMessage());
			mDepartmentPref.setText(user.getDepartment());
			mWeightPref.setText(user.getWeight() == null ? null : user.getWeight().toString());
			mHeightPref.setText(user.getHeight() == null ? null : user.getHeight().toString());
			mAgePref.setText(user.getAge() == null ? null : user.getAge().toString());
			mSexPref.setValue(user.getSex() == null ? null : user.getSex().toString());
			refreshSummaries();
		}
		
		public class RefreshUserTask extends AsyncTask<String, Void, Boolean> {

			private User user = null;
			
			@Override
			protected Boolean doInBackground(String... params) {
				try {
					ICardioMoodService service = CardioMoodService.getInstance();
					
					User user = service.getUserInfo(params[0], params[1], params[2]);
					Log.d("CardioMood", "MyProfileActivity.RefreshUserTask.doInBackGround -> user = " + user);
					if (user != null) {
						this.user = user;
						return true;
					}
				} catch (Exception e) {
					Log.d("CardioMood", "MyProfileActivity.RefreshUserTask.doInBackGround -> exception = " + e.getMessage());
					return false;
				}
				return false;
			}
			
			@Override
			protected void onPostExecute(final Boolean success) {
				if (success) {
					populatePreferences(user);
				} else {
					if(getActivity() != null)
						Toast.makeText(getActivity(), "Failed to refresh info.", Toast.LENGTH_SHORT).show();
				}
			}
			
		}
		
		public class ApplyUserProfileTask extends AsyncTask<String, Void, Boolean> {
			
			@Override
			protected Boolean doInBackground(String... params) {
				try {
					ICardioMoodService service = CardioMoodService.getInstance();
					
					CardioMoodSimpleResponse response = service.updateInfo(params[0], params[1]);
					Log.d("CardioMood", "MyProfileActivity.ApplyUserProfileTask.doInBackGround -> response = " + response);
					if (response != null && response.getResponse() == 1) {
						return true;
					}
				} catch (Exception e) {
					Log.d("CardioMood", "MyProfileActivity.ApplyUserProfileTask.doInBackGround -> exception = " + e.getMessage());
					return false;
				}
				return false;
			}
			
			@Override
			protected void onPostExecute(final Boolean success) {
				if (success) {
					if(getActivity() != null)
						Toast.makeText(getActivity(), "Profile has been saved.", Toast.LENGTH_SHORT).show();
				} else {
					if(getActivity() != null)
						Toast.makeText(getActivity(), "Failed to apply user profile changes.", Toast.LENGTH_SHORT).show();
				}
			}
			
		}
		
	}

	@Override
	public MyProfileFragment createMainFragment() {
		return new MyProfileFragment();
	}
}


