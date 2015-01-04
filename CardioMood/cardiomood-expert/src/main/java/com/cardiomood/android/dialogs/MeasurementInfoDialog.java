package com.cardiomood.android.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import com.cardiomood.android.expert.R;
import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.parse.ParseUser;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Anton Danshin on 30/12/14.
 */
public class MeasurementInfoDialog extends DialogFragment implements TextWatcher {

    private static final String ARG_NAME = "name";
    private static final String ARG_DESCRIPTION = "description";

    public static final String DEFAULT_NAME = "Measurement $date$";

    @InjectView(R.id.measurement_name) EditText nameView;
    @InjectView(R.id.measurement_description) EditText descriptionView;

    private String userId = null;
    private PreferenceHelper prefHelper;
    private Handler mHandler;

    private String name;
    private String description;
    private boolean dirty = false;
    private Callback callback;

    public static MeasurementInfoDialog newInstance(String name, String description) {
        final MeasurementInfoDialog fragment = new MeasurementInfoDialog();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_DESCRIPTION, description);
        fragment.setArguments(args);
        return fragment;
    }

    public MeasurementInfoDialog() {
        // required no-arg constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefHelper = new PreferenceHelper(getActivity(), true);
        if (ParseUser.getCurrentUser() != null) {
            userId = ParseUser.getCurrentUser().getObjectId();
        }
        mHandler = new Handler();
        if (getArguments() != null) {
            Bundle args = getArguments();
            name = args.getString(ARG_NAME, DEFAULT_NAME);
            description = args.getString(ARG_DESCRIPTION, null);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // inflate dialog layout
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup dialogView = (ViewGroup) inflater.inflate(R.layout.dialog_measurement_info, null);

        // setup view
        ButterKnife.inject(this, dialogView);
        nameView.setText(name);
        descriptionView.setText(description);
        nameView.addTextChangedListener(this);
        descriptionView.addTextChangedListener(this);

        // create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);
        builder.setTitle("Measurement Info");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onPositiveButtonClicked();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onNegativeButtonClicked();
            }
        });

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        nameView.clearFocus();
        descriptionView.clearFocus();
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

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void onPositiveButtonClicked() {
        if (! dirty) {
            return;
        }

        this.name = nameView.getText().toString().trim();
        this.description = descriptionView.getText().toString().trim();

        // notify client
        if (callback != null) {
            callback.onInfoUpdated(name, description);
        }
        dirty = false;
    }

    private void onNegativeButtonClicked() {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        dirty = true;
    }

    public static interface Callback {

        void onInfoUpdated(String name, String description);

    }
}
