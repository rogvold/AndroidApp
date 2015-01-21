package com.cardiomood.framework.fragment;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
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

import com.cardiomood.framework.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Anton Danshin on 08/01/15.
 */
public abstract class AbstractEditUserProfileFragment extends Fragment implements TextWatcher {

    private static final String TAG = AbstractEditUserProfileFragment.class.getSimpleName();
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(DateFormat.SHORT);

    protected EditText emailView;
    protected EditText firstNameView;
    protected EditText lastNameView;
    protected EditText phoneNumberView;
    protected Spinner genderView;
    protected EditText birthdayView;
    protected EditText weightMajorView;
    protected EditText weightMinorView;
    protected EditText heightMajorView;
    protected EditText heightMinorView;
    protected TextView weightMajorUnitsView;
    protected TextView heightMajorUnitsView;
    protected TextView weightMinorUnitsView;
    protected TextView heightMinorUnitsView;
    protected EditText restingHeartRateView;
    protected EditText aerobicThresholdView;
    protected EditText anaerobicThresholdView;

    private Callback callback;
    private boolean emailEditable = false;
    private boolean modified = false;
    private boolean initialized = false;
    private boolean firstStart = true;

    private Timer mTimer = new Timer("save_profile_timer");
    private TimerTask saveProfileTask = null;
    private Handler mHandler;

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

        mHandler = new Handler();
        initialized = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_user_profile, container, false);

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
                        AbstractEditUserProfileFragment.this.getActivity(),
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

        // refresh hints (due to possible settings modifications)
        final UnitSystem unitSystem = getPreferredUnitSystem();
        if (unitSystem == UnitSystem.IMPERIAL) {
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

        reloadData();
    }

    public abstract UnitSystem getPreferredUnitSystem();
    protected abstract void updateViews();
    protected abstract void performSave();
    protected abstract void performSync();

    public void reloadData() {
        initialized = false;

        removeListeners();
        updateViews();

        modified = false;
        initialized = true;
        restoreListeners();

        if (firstStart) {
            firstStart = false;
            sync();
        }
    }

    protected void restoreListeners() {
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

    protected void removeListeners() {
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
    public void onDetach() {
        if (modified)
            save();
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_edit_user_profile, menu);
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

    public final void save() {
        if (!initialized || !modified)
            return;

        new AsyncTask<Object, Object, Exception>() {
            @Override
            protected Exception doInBackground(Object... params) {
                try {
                    performSave();
                } catch (Exception ex) {
                    return ex;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception e) {
                if (e == null && callback != null) {
                    callback.onSave();
                } else {
                    Log.w(TAG, "save() failed with exception", e);
                }
            }
        }.execute();
    }

    public final void sync() {
        new AsyncTask<Object, Object, Exception>() {
            @Override
            protected Exception doInBackground(Object... params) {
                try {
                    performSync();
                } catch (Exception ex) {
                    return ex;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception e) {
                if (e == null && callback != null) {
                    callback.onSync();
                } else {
                    Log.w(TAG, "sync failed", e);
                }
            }
        }.execute();
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
        // do nothing
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // do nothing
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (initialized) {
            modified = true;
        }
        if (modified) {
            onModified();
        }
    }

    protected void onModified() {
        if (!initialized)
            return;

        // cancel pending task
        if (saveProfileTask != null) {
            saveProfileTask.cancel();
            saveProfileTask = null;
            mTimer.purge();
        }

        // schedule new task
        saveProfileTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        save();
                    }
                });
            }
        };
        mTimer.schedule(saveProfileTask, 1000);
    }

    public static interface Callback {
        void onSave();
        void onSync();
    }

    public static enum UnitSystem {
        METRIC, IMPERIAL;
    }
}