package com.cardiomood.android.fragments;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cardiomood.android.expert.R;
import com.cardiomood.android.tools.PreferenceHelper;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import bolts.Continuation;
import bolts.Task;
import timber.log.Timber;

/**
 * Project: CardioMood Kolomna
 * User: danon
 * Date: 15.06.13
 * Time: 18:36
 */
public class EditParseUserFragment extends Fragment implements TextWatcher {

    private static final String TAG = EditParseUserFragment.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(DateFormat.SHORT);

    // user profile editable parameters
    public static final String USER_EMAIL_KEY			= "email";
    public static final String USER_FIRST_NAME_KEY		= "firstName";
    public static final String USER_LAST_NAME_KEY		= "lastName";
    public static final String USER_DEPARTMENT_KEY		= "department";
    public static final String USER_WEIGHT_KEY			= "weight";
    public static final String USER_HEIGHT_KEY			= "height";
    public static final String USER_BIRTH_DATE_KEY      = "birthDate";
    public static final String USER_SEX_KEY				= "gender";
    public static final String USER_PHONE_NUMBER_KEY    = "phone";
    public static final String USER_RESTING_HR_KEY      = "restingHeartRate";
    public static final String USER_AEROBIC_THRESHOLD_KEY = "aerobicThreshold";
    public static final String USER_ANAEROBIC_THRESHOLD_KEY = "anaerobicThreshold";


    public static final String PREFERRED_MEASUREMENT_SYSTEM = "app.preferred_measurement_system";

    private PreferenceHelper prefHelper;
    private ParseUser user;

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
    private EditText restingHeartRateView;
    private EditText aerobicThresholdView;
    private EditText anaerobicThresholdView;

    private Callback callback;
    private boolean emailEditable = false;
    private boolean modified = false;
    private boolean initialized = false;
    private boolean firstStart = true;

    private Date oldDate = new Date();
    private final Calendar myCalendar = Calendar.getInstance();

    private final DatePickerDialog.OnDateSetListener dateChangeListener = new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            oldDate = myCalendar.getTime();
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            birthdayView.setText(DATE_FORMAT.format(myCalendar.getTime()));
            if (initialized)
                modified = true;
            save();
        }

    };
    private int genderLastSelectedPosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        prefHelper = new PreferenceHelper(getActivity(), true);
        initialized = false;
        user = ParseUser.getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_parse_user, container, false);

        emailView = (EditText) v.findViewById(R.id.editTextEmail);
        firstNameView = (EditText) v.findViewById(R.id.editTextFirstName);
        lastNameView = (EditText) v.findViewById(R.id.editTextLastName);
        phoneNumberView = (EditText) v.findViewById(R.id.editTextPhoneNumber);
        genderView = (Spinner) v.findViewById(R.id.gender);
        birthdayView = (EditText) v.findViewById(R.id.birth_date);
        birthdayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dlg = new DatePickerDialog(
                        EditParseUserFragment.this.getActivity(),
                        dateChangeListener,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)
                );
                dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (!myCalendar.getTime().equals(oldDate)) {
                            myCalendar.setTime(oldDate);
                            birthdayView.setText(DATE_FORMAT.format(myCalendar.getTime()));
                            if (initialized)
                                modified = true;
                            save();
                        }
                    }
                });
                dlg.setTitle(R.string.birth_date_dlg_title);
                dlg.show();
            }
        });
        birthdayView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (Build.VERSION.SDK_INT >= 15)
                        v.callOnClick();
                }
            }
        });
        restingHeartRateView = (EditText) v.findViewById(R.id.resting_heart_rate);
        aerobicThresholdView = (EditText) v.findViewById(R.id.aerobic_threshold);
        anaerobicThresholdView = (EditText) v.findViewById(R.id.anaerobic_threshold);

        weightMajorView = (EditText) v.findViewById(R.id.weight_major);
        weightMinorView = (EditText) v.findViewById(R.id.weight_minor);
        heightMajorView = (EditText) v.findViewById(R.id.height_major);
        heightMinorView = (EditText) v.findViewById(R.id.height_minor);

        weightMajorUnitsView = (TextView) v.findViewById(R.id.weight_major_units);
        weightMinorUnitsView = (TextView) v.findViewById(R.id.weight_minor_units);
        heightMajorUnitsView = (TextView) v.findViewById(R.id.height_major_units);
        heightMinorUnitsView = (TextView) v.findViewById(R.id.height_minor_units);

        if (emailEditable) {
            emailView.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            emailView.setFocusable(true);
        } else {
            emailView.setInputType(InputType.TYPE_NULL);
            emailView.setFocusable(false);
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    public void reloadData() {
        initialized = false;
        removeListeners();

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

        emailView.setText(user.getString(USER_EMAIL_KEY));
        firstNameView.setText(user.getString(USER_FIRST_NAME_KEY));
        lastNameView.setText(user.getString(USER_LAST_NAME_KEY));
        phoneNumberView.setText(user.getString(USER_PHONE_NUMBER_KEY));

        if (user.has(USER_RESTING_HR_KEY))
            restingHeartRateView.setText(String.valueOf(user.getInt(USER_RESTING_HR_KEY)));
        else restingHeartRateView.setText(null);

        if (user.has(USER_AEROBIC_THRESHOLD_KEY))
            aerobicThresholdView.setText(String.valueOf(user.getInt(USER_AEROBIC_THRESHOLD_KEY)));
        else aerobicThresholdView.setText(null);

        if (user.has(USER_ANAEROBIC_THRESHOLD_KEY))
            anaerobicThresholdView.setText(String.valueOf(user.getInt(USER_ANAEROBIC_THRESHOLD_KEY)));
        else anaerobicThresholdView.setText(null);

        String gender = user.getString(USER_SEX_KEY);
        if (gender == null) {
            gender = "UNSPECIFIED";
        }
        if ("MALE".equalsIgnoreCase(gender)) {
            genderLastSelectedPosition = 0;
            genderView.setSelection(0);
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            genderLastSelectedPosition = 1;
            genderView.setSelection(1);
        } else {
            genderLastSelectedPosition = 2;
            genderView.setSelection(2);
        }
        long birthDate = user.has(USER_BIRTH_DATE_KEY) ? user.getLong(USER_BIRTH_DATE_KEY) : 0L;
        if (birthDate > 0) {
            birthdayView.setText(DATE_FORMAT.format(new Date(birthDate)));
            myCalendar.setTime(new Date(birthDate));
        }
        else birthdayView.setText(null);

        float weight = user.has(USER_WEIGHT_KEY) ? (float) user.getDouble(USER_WEIGHT_KEY) : 0.0f;
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


        float height = user.has(USER_HEIGHT_KEY) ? (float) user.getDouble(USER_HEIGHT_KEY) : 0.0f;
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
        modified = false;
        initialized = true;
        restoreListeners();

        if (firstStart) {
            firstStart = false;
            sync();
        }
    }

    private void restoreListeners() {
        emailView.addTextChangedListener(this);
        firstNameView.addTextChangedListener(this);
        lastNameView.addTextChangedListener(this);
        phoneNumberView.addTextChangedListener(this);
        weightMajorView.addTextChangedListener(this);
        weightMinorView.addTextChangedListener(this);
        heightMajorView.addTextChangedListener(this);
        heightMinorView.addTextChangedListener(this);
        birthdayView.addTextChangedListener(this);
        restingHeartRateView.addTextChangedListener(this);
        aerobicThresholdView.addTextChangedListener(this);
        anaerobicThresholdView.addTextChangedListener(this);
        genderView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (genderLastSelectedPosition != position) {
                    genderLastSelectedPosition = position;
                    if (initialized)
                        modified = true;
                    save();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (initialized)
                    modified = true;
                save();
            }
        });
    }

    private void removeListeners() {
        emailView.removeTextChangedListener(this);
        firstNameView.removeTextChangedListener(this);
        lastNameView.removeTextChangedListener(this);
        phoneNumberView.removeTextChangedListener(this);
        weightMajorView.removeTextChangedListener(this);
        weightMinorView.removeTextChangedListener(this);
        heightMajorView.removeTextChangedListener(this);
        heightMinorView.removeTextChangedListener(this);
        birthdayView.removeTextChangedListener(this);
        genderView.setOnItemSelectedListener(null);
        restingHeartRateView.removeTextChangedListener(this);
        aerobicThresholdView.removeTextChangedListener(this);
        anaerobicThresholdView.removeTextChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (modified)
            save();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void save() {
        if (!initialized || !modified)
            return;
        try {
            final String unitSystem = prefHelper.getString(PREFERRED_MEASUREMENT_SYSTEM, "METRIC");

            user.put(USER_EMAIL_KEY, emailView.getText().toString());
            user.put(USER_FIRST_NAME_KEY, firstNameView.getText().toString());
            user.put(USER_LAST_NAME_KEY, lastNameView.getText().toString());
            user.put(USER_PHONE_NUMBER_KEY, phoneNumberView.getText().toString());

            if (restingHeartRateView.getText().toString().isEmpty()) {
                user.remove(USER_RESTING_HR_KEY);
            } else {
                user.put(USER_RESTING_HR_KEY, Integer.parseInt(restingHeartRateView.getText().toString()));
            }

            if (aerobicThresholdView.getText().toString().isEmpty()) {
                user.remove(USER_AEROBIC_THRESHOLD_KEY);
            } else {
                user.put(USER_AEROBIC_THRESHOLD_KEY, Integer.parseInt(aerobicThresholdView.getText().toString()));
            }

            if (anaerobicThresholdView.getText().toString().isEmpty()) {
                user.remove(USER_ANAEROBIC_THRESHOLD_KEY);
            } else {
                user.put(USER_ANAEROBIC_THRESHOLD_KEY, Integer.parseInt(anaerobicThresholdView.getText().toString()));
            }

            int gender = genderView.getSelectedItemPosition();
            if (gender == 0) {
                user.put(USER_SEX_KEY, "MALE");
            } else if (gender == 1) {
                user.put(USER_SEX_KEY, "FEMALE");
            } else
                user.remove(USER_SEX_KEY);

            if ("IMPERIAL".equalsIgnoreCase(unitSystem)) {
                float lb = 0;
                float oz = 0;
                if (!TextUtils.isEmpty(weightMajorView.getText().toString())) {
                    lb = Float.parseFloat(weightMajorView.getText().toString());
                }
                if (!TextUtils.isEmpty(weightMinorView.getText().toString())) {
                    oz = Float.parseFloat(weightMinorView.getText().toString());
                }
                user.put(USER_WEIGHT_KEY, lb / 2.20462f + oz / 16f / 2.20462f);
            } else {
                float kg = 0;
                float g = 0;
                if (!TextUtils.isEmpty(weightMajorView.getText().toString())) {
                    kg = Float.parseFloat(weightMajorView.getText().toString());
                }
                if (!TextUtils.isEmpty(weightMinorView.getText().toString())) {
                    g = Float.parseFloat(weightMinorView.getText().toString());
                }
                user.put(USER_WEIGHT_KEY, kg + g / 1000);
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
                user.put(USER_HEIGHT_KEY, ft * 0.3048f + in * 0.0254f);
            } else {
                float m = 0;
                float cm = 0;
                if (!TextUtils.isEmpty(heightMajorView.getText().toString())) {
                    m = Float.parseFloat(heightMajorView.getText().toString());
                }
                if (!TextUtils.isEmpty(heightMinorView.getText().toString())) {
                    cm = Float.parseFloat(heightMinorView.getText().toString());
                }
                user.put(USER_HEIGHT_KEY, m + cm / 100);
            }

            if (!TextUtils.isEmpty(birthdayView.getText().toString()))
                user.put(USER_BIRTH_DATE_KEY, DATE_FORMAT.parse(birthdayView.getText().toString()).getTime());
            else user.remove(USER_BIRTH_DATE_KEY);

            user.saveEventually();

            modified = false;
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
        if (modified)
            save();
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_profile, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_sync) {
            sync();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sync() {
        ParseUser.getCurrentUser()
                .fetchInBackground()
                .continueWith(new Continuation<ParseObject, Object>() {
                    @Override
                    public Object then(Task<ParseObject> task) throws Exception {
                        if (task.isFaulted()) {
                            // failed
                            Timber.w(task.getError(), "ParseUser.fetchInBackground() failed");
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(), "Failed to refresh.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else if (task.isCompleted()) {
                            user = (ParseUser) task.getResult();
                            if (callback != null) {
                                callback.onSync();
                            }
                            reloadData();
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);

    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public boolean isEmailEditable() {
        return emailEditable;
    }

    public void setEmailEditable(boolean emailEditable) {
        this.emailEditable = emailEditable;
        if (emailView != null) {
            emailView.setInputType(emailEditable ? InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS : InputType.TYPE_NULL);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (initialized) {
            modified = true;
        }
        save();
    }

    public static interface Callback {
        void onSave();
        void onSync();
    }
}
