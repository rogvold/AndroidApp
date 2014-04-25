package com.cardiomood.heartrate.android.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.heartrate.android.R;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Project: CardioMood
 * User: danon
 * Date: 15.06.13
 * Time: 18:36
 */
public class ProfileFragment extends Fragment implements ConfigurationConstants, View.OnKeyListener {

    private static final String TAG = ProfileFragment.class.getSimpleName();
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
    private TextView weightUnitsView;
    private TextView heightUnitsView;

    private Callback callback;

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
        emailView.setOnKeyListener(this);

        firstNameView = (EditText) v.findViewById(R.id.editTextFirstName);
        firstNameView.setOnKeyListener(this);

        lastNameView = (EditText) v.findViewById(R.id.editTextLastName);
        lastNameView.setOnKeyListener(this);

        phoneNumberView = (EditText) v.findViewById(R.id.editTextPhoneNumber);
        phoneNumberView.setOnKeyListener(this);

        genderView = (Spinner) v.findViewById(R.id.gender);
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
        weightView.setOnKeyListener(this);

        heightView = (EditText) v.findViewById(R.id.height);
        heightView.setOnKeyListener(this);

        weightUnitsView = (TextView) v.findViewById(R.id.weight_units);
        heightUnitsView = (TextView) v.findViewById(R.id.height_units);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        // refresh hints (due to possible settings modifications)
        final String unitSystem = prefHelper.getString(PREFERRED_MEASUREMENT_SYSTEM, "METRIC");
        if ("IMPERIAL".equalsIgnoreCase(unitSystem)) {
            heightView.setHint(R.string.your_height_imperial);
            weightView.setHint(R.string.your_weight_imperial);
            heightUnitsView.setText(R.string.height_units_ft);
            weightUnitsView.setText(R.string.weight_units_lb);
        } else {
            heightView.setHint(R.string.your_height);
            weightView.setHint(R.string.your_weight);
            heightUnitsView.setText(R.string.height_units_cm);
            weightUnitsView.setText(R.string.weight_units_kg);
        }

        emailView.setText(prefHelper.getString(USER_EMAIL_KEY));
        firstNameView.setText(prefHelper.getString(USER_FIRST_NAME_KEY));
        lastNameView.setText(prefHelper.getString(USER_LAST_NAME_KEY));
        phoneNumberView.setText(prefHelper.getString(USER_PHONE_NUMBER_KEY));

        String gender = prefHelper.getString(USER_SEX_KEY, "UNSPECIFIED");
        if ("MALE".equalsIgnoreCase(gender))
            genderView.setSelection(0);
        else if ("FEMALE".equalsIgnoreCase(gender))
            genderView.setSelection(1);
        else
            genderView.setSelection(2);

        long birthDate = prefHelper.getLong(USER_BIRTH_DATE_KEY, 0L);
        if (birthDate > 0)
            birthdayView.setText(DATE_FORMAT.format(new Date(birthDate)));
        else birthdayView.setText(null);

        float weight = prefHelper.getFloat(USER_WEIGHT_KEY, 0.0f);
        if ("IMPERIAL".equalsIgnoreCase(unitSystem))
            weight *= 2.20462;
        if (weight > 0.0f)
            weightView.setText(String.valueOf(weight));

        float height = prefHelper.getFloat(USER_HEIGHT_KEY, 0.0f);
        if ("IMPERIAL".equalsIgnoreCase(unitSystem))
            height *= 0.0328084;
        if (height > 0.0f)
            heightView.setText(String.valueOf(height));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void save() {
        try {
            final String unitSystem = prefHelper.getString(PREFERRED_MEASUREMENT_SYSTEM, "METRIC");

            prefHelper.putString(USER_EMAIL_KEY, emailView.getText().toString());
            prefHelper.putString(USER_FIRST_NAME_KEY, firstNameView.getText().toString());
            prefHelper.putString(USER_LAST_NAME_KEY, lastNameView.getText().toString());
            prefHelper.putString(USER_PHONE_NUMBER_KEY, phoneNumberView.getText().toString());

            int gender = genderView.getSelectedItemPosition();
            if (gender == 0) {
                prefHelper.putString(USER_SEX_KEY, "MALE");
            } else if (gender == 1) {
                prefHelper.putString(USER_SEX_KEY, "FEMALE");
            } else
                prefHelper.remove(USER_SEX_KEY);

            if (!TextUtils.isEmpty(weightView.getText().toString())) {
                float weight = Float.parseFloat(weightView.getText().toString());
                if ("IMPERIAL".equalsIgnoreCase(unitSystem))
                    weight /= 2.20462;
                prefHelper.putFloat(USER_WEIGHT_KEY, weight);
            } else prefHelper.remove(USER_WEIGHT_KEY);

            if (!TextUtils.isEmpty(heightView.getText().toString())) {
                float height = Float.parseFloat(heightView.getText().toString());
                if ("IMPERIAL".equalsIgnoreCase(unitSystem))
                    height /= 0.0328084;
                prefHelper.putFloat(USER_HEIGHT_KEY, height);
            } else prefHelper.remove(USER_HEIGHT_KEY);

            if (!TextUtils.isEmpty(birthdayView.getText().toString()))
                prefHelper.putLong(USER_BIRTH_DATE_KEY, DATE_FORMAT.parse(birthdayView.getText().toString()).getTime());
            else prefHelper.remove(USER_BIRTH_DATE_KEY);
        } catch (Exception ex) {
            Log.w(TAG, "save() failed with exception", ex);
        }
        if (callback != null)
            callback.onSave();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        save();
        super.onDetach();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        save();
        return false;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public static interface Callback {
        void onSave();
    }
}
