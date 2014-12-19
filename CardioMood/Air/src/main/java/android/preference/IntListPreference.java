package android.preference;

import android.content.Context;
import android.util.AttributeSet;

public class IntListPreference extends ListPreference {

    public IntListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntListPreference(Context context) {
        super(context);
    }

    public void setEntryValues(int[] values) {
        if (values == null) {
            setEntryValues((CharSequence[]) null);
            return;
        }

        String[] strValues = new String[values.length];
        for (int i=0; i<values.length; i++)
            strValues[i] = String.valueOf(values[i]);
        setEntryValues(strValues);
    }

    @Override
    protected boolean persistString(String value) {
        if(value == null) {
            return false;
        } else {
            return persistInt(Integer.valueOf(value));
        }
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            int intValue = getPersistedInt(0);
            return String.valueOf(intValue);
        } else {
            return defaultReturnValue;
        }
    }
}