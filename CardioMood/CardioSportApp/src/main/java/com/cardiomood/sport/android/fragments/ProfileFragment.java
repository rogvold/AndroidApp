package com.cardiomood.sport.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.sport.android.R;
import com.cardiomood.sport.android.tools.config.ConfigurationConstants;

/**
 * Project: CardioMood
 * User: danon
 * Date: 15.06.13
 * Time: 18:36
 */
public class ProfileFragment extends Fragment implements ConfigurationConstants {

    private PreferenceHelper prefHelper;

    private EditText emailView;
    private EditText firstNameView;
    private EditText lastNameView;
    private EditText phoneNumberView;
    private Spinner genderView;
    private EditText birthdayView;
    private EditText weightView;

    private Button saveButton;

    private volatile boolean refreshing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        prefHelper = new PreferenceHelper(getActivity().getApplicationContext());
        prefHelper.setPersistent(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_refresh);
        if (refreshing) {
            item.setEnabled(false);
            item.setTitle(R.string.refreshing);
        } else {
            item.setEnabled(true);
            item.setTitle(R.string.refresh);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        emailView = (EditText) v.findViewById(R.id.editTextEmail);
        emailView.setText(prefHelper.getString(USER_EMAIL_KEY));

        firstNameView = (EditText) v.findViewById(R.id.editTextFirstName);
        firstNameView.setText(prefHelper.getString(USER_FIRST_NAME_KEY));

        lastNameView = (EditText) v.findViewById(R.id.editTextLastName);
        lastNameView.setText(prefHelper.getString(USER_LAST_NAME_KEY));

        phoneNumberView = (EditText) v.findViewById(R.id.editTextPhoneNumber);
        phoneNumberView.setText(prefHelper.getString(USER_PHONE_NUMBER_KEY));

        genderView = (Spinner) v.findViewById(R.id.gender);
        genderView.setSelection(prefHelper.getInt(USER_SEX_KEY, 2));

        birthdayView = (EditText) v.findViewById(R.id.birth_date);
        birthdayView.setText(prefHelper.getString(USER_BIRTH_DATE_KEY));

        weightView = (EditText) v.findViewById(R.id.weight);
        weightView.setText(prefHelper.getString(USER_WEIGHT_KEY));

        saveButton = (Button) v.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void save() {
        prefHelper.putString(USER_FIRST_NAME_KEY, firstNameView.getText().toString());
        prefHelper.putString(USER_LAST_NAME_KEY, lastNameView.getText().toString());
        prefHelper.putString(USER_PHONE_NUMBER_KEY, phoneNumberView.getText().toString());
        prefHelper.putInt(USER_SEX_KEY, genderView.getSelectedItemPosition());
        prefHelper.putString(USER_BIRTH_DATE_KEY, birthdayView.getText().toString());
        prefHelper.putString(USER_WEIGHT_KEY, weightView.getText().toString());
        prefHelper.putString(USER_EMAIL_KEY, emailView.getText().toString());

        Toast.makeText(getActivity(), R.string.profile_saved_locally, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_profile, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
               // refresh();
                Toast.makeText(this.getActivity(), R.string.unsupported_operation, Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
