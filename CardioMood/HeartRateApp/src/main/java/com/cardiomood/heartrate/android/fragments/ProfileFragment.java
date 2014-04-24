package com.cardiomood.heartrate.android.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.heartrate.android.R;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Project: CardioMood
 * User: danon
 * Date: 15.06.13
 * Time: 18:36
 */
public class ProfileFragment extends Fragment implements ConfigurationConstants, View.OnKeyListener {

    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(java.text.DateFormat.SHORT);

    private PreferenceHelper prefHelper;

    private EditText emailView;
    private EditText firstNameView;
    private EditText lastNameView;
    private EditText phoneNumberView;
    private Spinner genderView;
    private EditText birthdayView;
    private EditText weightView;
    private EditText heightView;

    private Calendar myCalendar = Calendar.getInstance();

    private final DatePickerDialog.OnDateSetListener dateChangeListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            birthdayView.setText(DATE_FORMAT.format(myCalendar.getTime()));
            save();
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        prefHelper = new PreferenceHelper(getActivity().getApplicationContext());
        prefHelper.setPersistent(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        emailView = (EditText) v.findViewById(R.id.editTextEmail);
        emailView.setText(prefHelper.getString(USER_EMAIL_KEY));
        emailView.setOnKeyListener(this);

        firstNameView = (EditText) v.findViewById(R.id.editTextFirstName);
        firstNameView.setText(prefHelper.getString(USER_FIRST_NAME_KEY));
        firstNameView.setOnKeyListener(this);

        lastNameView = (EditText) v.findViewById(R.id.editTextLastName);
        lastNameView.setText(prefHelper.getString(USER_LAST_NAME_KEY));
        lastNameView.setOnKeyListener(this);

        phoneNumberView = (EditText) v.findViewById(R.id.editTextPhoneNumber);
        phoneNumberView.setText(prefHelper.getString(USER_PHONE_NUMBER_KEY));
        phoneNumberView.setOnKeyListener(this);

        genderView = (Spinner) v.findViewById(R.id.gender);
        genderView.setSelection(prefHelper.getInt(USER_SEX_KEY, 2));
        genderView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                save();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                save();
            }
        });

        birthdayView = (EditText) v.findViewById(R.id.birth_date);
        birthdayView.setText(prefHelper.getString(USER_BIRTH_DATE_KEY));
        birthdayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dlg = new DatePickerDialog(
                        ProfileFragment.this.getActivity(),
                        dateChangeListener,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)
                );
                dlg.setTitle(R.string.birth_date_dlg_title);
                dlg.show();
            }
        });

        weightView = (EditText) v.findViewById(R.id.weight);
        weightView.setText(prefHelper.getString(USER_WEIGHT_KEY));
        weightView.setOnKeyListener(this);

        heightView = (EditText) v.findViewById(R.id.height);
        heightView.setText(prefHelper.getString(USER_HEIGHT_KEY));
        heightView.setOnKeyListener(this);

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
        prefHelper.putString(USER_HEIGHT_KEY, heightView.getText().toString());
        prefHelper.putString(USER_EMAIL_KEY, emailView.getText().toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        save();
        return false;
    }
}
