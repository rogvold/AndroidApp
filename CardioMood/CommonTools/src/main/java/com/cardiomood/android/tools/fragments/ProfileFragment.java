package com.cardiomood.android.tools.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.R;

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
public class ProfileFragment extends Fragment implements View.OnKeyListener {

    private static final String TAG = ProfileFragment.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(DateFormat.SHORT);

    // user profile editable parameters
    public static final String USER_EMAIL_KEY			= "user.email";
    public static final String USER_FIRST_NAME_KEY		= "user.first_name";
    public static final String USER_LAST_NAME_KEY		= "user.last_name";
    public static final String USER_ABOUT_KEY			= "user.about";
    public static final String USER_DESCRIPTION_KEY		= "user.description";
    public static final String USER_DIAGNOSIS_KEY		= "user.diagnosis";
    public static final String USER_STATUS_KEY			= "user.status";
    public static final String USER_DEPARTMENT_KEY		= "user.department";
    public static final String USER_WEIGHT_KEY			= "user.weight";
    public static final String USER_HEIGHT_KEY			= "user.height";
    public static final String USER_BIRTH_DATE_KEY      = "user.age";
    public static final String USER_SEX_KEY				= "user.sex";
    public static final String USER_PHONE_NUMBER_KEY    = "user.phone_number";

    public static final String PREFERRED_MEASUREMENT_SYSTEM = "app.preferred_measurement_system";

    private PreferenceHelper prefHelper;

    private EditText emailView;
    private EditText firstNameView;
    private EditText lastNameView;
    private EditText phoneNumberView;
    private Spinner genderView;
    private EditText birthdayView;
    private EditText weightMajorView;
    private EditText weightMinorView;
    private EditText heightMajorView;
    private EditText heightMinorView;
    private TextView weightMajorUnitsView;
    private TextView heightMajorUnitsView;
    private TextView weightMinorUnitsView;
    private TextView heightMinorUnitsView;

    private Callback callback;

    private final Calendar myCalendar = Calendar.getInstance();

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

        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                save();
                return false;
            }
        });

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
        birthdayView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.callOnClick();
                }
            }
        });

        weightMajorView = (EditText) v.findViewById(R.id.weight_major);
        weightMajorView.setOnKeyListener(this);

        weightMinorView = (EditText) v.findViewById(R.id.weight_minor);
        weightMinorView.setOnKeyListener(this);

        heightMajorView = (EditText) v.findViewById(R.id.height_major);
        heightMajorView.setOnKeyListener(this);

        heightMinorView = (EditText) v.findViewById(R.id.height_minor);
        heightMinorView.setOnKeyListener(this);

        weightMajorUnitsView = (TextView) v.findViewById(R.id.weight_major_units);
        weightMinorUnitsView = (TextView) v.findViewById(R.id.weight_minor_units);
        heightMajorUnitsView = (TextView) v.findViewById(R.id.height_major_units);
        heightMinorUnitsView = (TextView) v.findViewById(R.id.height_minor_units);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // refresh hints (due to possible settings modifications)
        final String unitSystem = prefHelper.getString(PREFERRED_MEASUREMENT_SYSTEM, "METRIC");
        if ("IMPERIAL".equalsIgnoreCase(unitSystem)) {
            heightMajorUnitsView.setText(R.string.height_units_ft);
            weightMajorUnitsView.setText(R.string.weight_units_lb);
            heightMinorUnitsView.setText(R.string.height_units_in);
            weightMinorUnitsView.setText(R.string.weight_units_oz);
        } else {
            heightMajorUnitsView.setText(R.string.height_units_m);
            weightMajorUnitsView.setText(R.string.weight_units_kg);
            heightMinorUnitsView.setText(R.string.height_units_cm);
            weightMinorUnitsView.setText(R.string.weight_units_g);
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
        if (birthDate > 0) {
            birthdayView.setText(DATE_FORMAT.format(new Date(birthDate)));
            myCalendar.setTime(new Date(birthDate));
        }
        else birthdayView.setText(null);

        float weight = prefHelper.getFloat(USER_WEIGHT_KEY, 0.0f);
        if ("IMPERIAL".equalsIgnoreCase(unitSystem)) {
            weight *= 2.20462;
            if (weight > 0.0f) {
                weightMajorView.setText(String.valueOf(Math.round(Math.floor(weight))));
                weightMinorView.setText(String.valueOf(Math.round((weight - Math.floor(weight))*16)));
            } else {
                weightMajorView.setText(null);
                weightMinorView.setText(null);
            }
        } else {
            if (weight > 0.0f) {
                weightMajorView.setText(String.valueOf(Math.round(Math.floor(weight))));
                weightMinorView.setText(String.valueOf(Math.round((weight - Math.floor(weight))*1000)));
            } else {
                weightMajorView.setText(null);
                weightMinorView.setText(null);
            }
        }


        float height = prefHelper.getFloat(USER_HEIGHT_KEY, 0.0f);
        if ("IMPERIAL".equalsIgnoreCase(unitSystem)) {
            height *= 3.28084;
            if (height > 0.0f) {
                heightMajorView.setText(String.valueOf(Math.round(Math.floor(height))));
                heightMinorView.setText(String.valueOf(Math.round((height - Math.floor(height))*12)));
            } else {
                heightMajorView.setText(null);
                heightMinorView.setText(null);
            }
        } else {
            if (height > 0.0f) {
                heightMajorView.setText(String.valueOf(Math.round(Math.floor(height))));
                heightMinorView.setText(String.valueOf(Math.round((height - Math.floor(height)) * 100)));
            } else {
                heightMajorView.setText(null);
                heightMinorView.setText(null);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        save();
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

            if ("IMPERIAL".equalsIgnoreCase(unitSystem)) {
                float lb = 0;
                float oz = 0;
                if (!TextUtils.isEmpty(weightMajorView.getText().toString())) {
                    lb = Float.parseFloat(weightMajorView.getText().toString());
                }
                if (!TextUtils.isEmpty(weightMinorView.getText().toString())) {
                    oz = Float.parseFloat(weightMinorView.getText().toString());
                }
                prefHelper.putFloat(USER_WEIGHT_KEY, lb/2.20462f + oz/16f/2.20462f);
            } else {
                float kg = 0;
                float g = 0;
                if (!TextUtils.isEmpty(weightMajorView.getText().toString())) {
                    kg = Float.parseFloat(weightMajorView.getText().toString());
                }
                if (!TextUtils.isEmpty(weightMinorView.getText().toString())) {
                    g = Float.parseFloat(weightMinorView.getText().toString());
                }
                prefHelper.putFloat(USER_WEIGHT_KEY, kg + g/1000);
            }

            if ("IMPERIAL".equalsIgnoreCase(unitSystem)) {
                float ft = 0;
                float in = 0;
                if (!TextUtils.isEmpty(heightMajorView.getText().toString())) {
                    ft = Float.parseFloat(heightMajorView.getText().toString());
                }
                if (!TextUtils.isEmpty(heightMinorView.getText().toString())) {
                    in = Float.parseFloat(heightMinorView.getText().toString());
                }
                prefHelper.putFloat(USER_HEIGHT_KEY, ft*3.28084f + in*3.28084f/12);
            } else {
                float m = 0;
                float cm = 0;
                if (!TextUtils.isEmpty(heightMajorView.getText().toString())) {
                    m = Float.parseFloat(heightMajorView.getText().toString());
                }
                if (!TextUtils.isEmpty(heightMinorView.getText().toString())) {
                    cm = Float.parseFloat(heightMinorView.getText().toString());
                }
                prefHelper.putFloat(USER_HEIGHT_KEY, m + cm/100);
            }

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
