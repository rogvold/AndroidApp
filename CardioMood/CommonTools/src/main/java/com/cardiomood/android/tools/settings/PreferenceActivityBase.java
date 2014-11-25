package com.cardiomood.android.tools.settings;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.cardiomood.android.tools.R;

@SuppressWarnings("NewApi")
public abstract class PreferenceActivityBase<F extends PreferenceActivityBase.AbstractMainFragment> extends PreferenceActivity {

	private F fragment;
    private Toolbar mActionBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            fragment = createMainFragment();

            getFragmentManager().beginTransaction()
                    .replace(R.id.content_wrapper, fragment).commit();
        }

        mActionBar.setTitle(getTitle());
	}
	
	@Override
	protected void onResume() {
		super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Register the listener whenever a key changes
            PreferenceManager.getDefaultSharedPreferences(this)
                    .registerOnSharedPreferenceChangeListener(fragment);
        }
	}
	
	@Override
    protected void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Unregister the listener whenever a key changes
            PreferenceManager.getDefaultSharedPreferences(this)
                    .unregisterOnSharedPreferenceChangeListener(fragment);
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_settings, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.action_bar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }
	
	public F getFragment() {
		return fragment;
	}
	
	public abstract F createMainFragment();
	
	public abstract static class AbstractMainFragment extends PreferenceFragment
													  implements OnSharedPreferenceChangeListener {

        public void updatePrefSummary(String defaultSummary, Preference... prefs) {
			for (Preference pref : prefs) {
				if (pref instanceof EditTextPreference) {
					EditTextPreference etp = (EditTextPreference) pref;
				    if (!TextUtils.isEmpty(etp.getText()))
				    	etp.setSummary(etp.getText());
				    else etp.setText(defaultSummary);
				} else if (pref instanceof ListPreference) {
					ListPreference lp = (ListPreference) pref;
				    if (!TextUtils.isEmpty(lp.getValue()))
				    	lp.setSummary(lp.getEntry());
				    else lp.setSummary(defaultSummary);
				}
			}
		}
		
		public void updatePrefSummary(Preference... prefs) {
			for (Preference pref : prefs) {
				if (pref instanceof EditTextPreference) {
					EditTextPreference etp = (EditTextPreference) pref;
				    etp.setSummary(etp.getText());
				} else if (pref instanceof ListPreference) {
					ListPreference lp = (ListPreference) pref;
				    lp.setSummary(lp.getEntry());
				}
			}
		}
		
	}
}
