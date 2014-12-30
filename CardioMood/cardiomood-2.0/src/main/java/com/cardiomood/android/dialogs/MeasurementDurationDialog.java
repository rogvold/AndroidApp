package com.cardiomood.android.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.cardiomood.android.R;
import com.cardiomood.android.fragments.NewMeasurementFragment;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.parse.ParseUser;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Anton Danshin on 28/12/14.
 */
public class MeasurementDurationDialog extends DialogFragment {

    private static final int LIMIT_TYPE_NONE = 0;
    private static final int LIMIT_TYPE_TIME = 1;
    private static final int LIMIT_TYPE_COUNT = 2;
    private static final int LIMIT_TYPE_CUSTOM = 3;

    @InjectView(R.id.dialog_content) ViewGroup content;
    @InjectView(R.id.time_limit_layout) LinearLayout timeLimitLayout;
    @InjectView(R.id.count_limit_layout) LinearLayout countLimitLayout;
    @InjectView(R.id.custom_limit_layout) LinearLayout customLimitLayout;
    @InjectView(R.id.limit_by) Spinner limitTypeSpinner;
    @InjectView(R.id.time_limit) Spinner timeLimitSpinner;
    @InjectView(R.id.count_limit) Spinner countLimitSpinner;
    @InjectView(R.id.custom_count_limit) EditText customCountLimitTxt;
    @InjectView(R.id.custom_time_limit) EditText customTimeLimitTxt;
    @InjectView(R.id.auto_start_measurement) CheckBox startImmediately;

    private String userId = null;
    private PreferenceHelper prefHelper;
    private Handler mHandler;

    public MeasurementDurationDialog() {
        // required no-arg constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        prefHelper = new PreferenceHelper(getActivity(), true);
        if (ParseUser.getCurrentUser() != null) {
            userId = ParseUser.getCurrentUser().getObjectId();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // inflate dialog layout
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup dialogView = (ViewGroup) inflater.inflate(R.layout.dialog_measurement_duration, null);

        // inject views
        ButterKnife.inject(this, dialogView);
        // update view state
        limitTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case LIMIT_TYPE_TIME:
                        countLimitLayout.setVisibility(View.GONE);
                        timeLimitLayout.setVisibility(View.VISIBLE);
                        customLimitLayout.setVisibility(View.GONE);
                        break;
                    case LIMIT_TYPE_COUNT:
                        countLimitLayout.setVisibility(View.VISIBLE);
                        timeLimitLayout.setVisibility(View.GONE);
                        customLimitLayout.setVisibility(View.GONE);
                        break;
                    case LIMIT_TYPE_CUSTOM:
                        countLimitLayout.setVisibility(View.GONE);
                        timeLimitLayout.setVisibility(View.GONE);
                        customLimitLayout.setVisibility(View.VISIBLE);
                        break;
                    case LIMIT_TYPE_NONE:
                    default:
                        countLimitLayout.setVisibility(View.GONE);
                        timeLimitLayout.setVisibility(View.GONE);
                        customLimitLayout.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                countLimitLayout.setVisibility(View.GONE);
                timeLimitLayout.setVisibility(View.GONE);
                customLimitLayout.setVisibility(View.GONE);
            }
        });
        restoreParameters();
        disableEnableControls(!NewMeasurementFragment.inProgress, content);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Measurement Duration");
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveSelectedParameters();

            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restoreParameters();
            }
        });

        return dialogBuilder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if (activity != null) {
                    CommonTools.hideSoftInputKeyboard(activity);
                }
            }
        }, 100L);
    }

    private void restoreParameters() {
        limitTypeSpinner.setSelection(
                prefHelper.getInt(ConfigurationConstants.MEASUREMENT_LIMIT_TYPE + "_" + userId, LIMIT_TYPE_TIME)
        );
        timeLimitSpinner.setSelection(
                prefHelper.getInt(ConfigurationConstants.MEASUREMENT_TIME_LIMIT + "_" + userId, 0)
        );
        countLimitSpinner.setSelection(
                prefHelper.getInt(ConfigurationConstants.MEASUREMENT_COUNT_LIMIT + "_" + userId, 0)
        );
        customCountLimitTxt.setText(
                prefHelper.getString(ConfigurationConstants.MEASUREMENT_CUSTOM_COUNT_LIMIT + "_" + userId)
        );
        customTimeLimitTxt.setText(
                prefHelper.getString(ConfigurationConstants.MEASUREMENT_CUSTOM_TIME_LIMIT + "_" + userId)
        );
        startImmediately.setChecked(
                prefHelper.getBoolean(ConfigurationConstants.MEASUREMENT_AUTO_START + "_" + userId)
        );
    }

    private void saveSelectedParameters() {
        prefHelper.putInt(ConfigurationConstants.MEASUREMENT_LIMIT_TYPE + "_" + userId ,
                limitTypeSpinner.getSelectedItemPosition());
        prefHelper.putInt(ConfigurationConstants.MEASUREMENT_TIME_LIMIT + "_" + userId,
                timeLimitSpinner.getSelectedItemPosition());
        prefHelper.putInt(ConfigurationConstants.MEASUREMENT_COUNT_LIMIT + "_" + userId,
                countLimitSpinner.getSelectedItemPosition());
        prefHelper.putString(ConfigurationConstants.MEASUREMENT_CUSTOM_COUNT_LIMIT + "_" + userId,
                getValidInt(customCountLimitTxt.getText().toString()));
        prefHelper.putString(ConfigurationConstants.MEASUREMENT_CUSTOM_TIME_LIMIT + "_" + userId,
                getValidInt(customTimeLimitTxt.getText().toString()));
        prefHelper.putBoolean(ConfigurationConstants.MEASUREMENT_AUTO_START+ "_" + userId,
                startImmediately.isChecked());
    }

    private String getValidInt(String s) {
        if (TextUtils.isEmpty(s))
            return null;
        try {
            return Integer.parseInt(s) + "";
        } catch (Exception ex) {
            return null;
        }
    }

    private void disableEnableControls(boolean enable, ViewGroup vg){
        for (int i = 0; i < vg.getChildCount(); i++){
            View child = vg.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup){
                disableEnableControls(enable, (ViewGroup) child);
            }
        }
    }
}
