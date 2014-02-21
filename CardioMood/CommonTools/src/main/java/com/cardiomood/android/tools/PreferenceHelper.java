package com.cardiomood.android.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceHelper {

    private static final String TAG = "CardioMood.CommonTools.PreferenceHelper";

    private final Context context;
    private boolean persistent;

    public PreferenceHelper(Context context) {
        this.context = context;
    }

    public synchronized boolean isPersistent() {
        return persistent;
    }

    public synchronized void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public Context getContext() {
        return context;
    }

    public synchronized void putString(String key, String value, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(key, value);
            editor.commit();
        }
        ConfigurationManager.getInstance().setString(key, value);
    }

    public synchronized void putString(String key, String value) {
        putString(key, value, isPersistent());
    }

    public synchronized String getString(String key, String defValue, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPref.getString(key, defValue);
        } else {
            return ConfigurationManager.getInstance().getString(key, defValue);
        }
    }

    public synchronized String getString(String key, String defValue) {
        return getString(key, defValue, isPersistent());
    }

    public synchronized String getString(String key) {
        return getString(key, null, isPersistent());
    }

    public synchronized String getString(String key, boolean persistent) {
        return getString(key, null, persistent);
    }

    public synchronized void putFloat(String key, Float value, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putFloat(key, value);
            editor.commit();
        }
        ConfigurationManager.getInstance().set(key, value);
    }

    public synchronized void putFloat(String key, String value) {
        putString(key, value, isPersistent());
    }

    public synchronized Float getFloat(String key, Float defValue, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPref.getFloat(key, defValue);
        } else {
            return ConfigurationManager.getInstance().getFloat(key, defValue);
        }
    }

    public synchronized Float getFloat(String key, Float defValue) {
        return getFloat(key, defValue, isPersistent());
    }

    public synchronized Float getFloat(String key) {
        return getFloat(key, null, isPersistent());
    }

    public synchronized Float getFloat(String key, boolean persistent) {
        return getFloat(key, null, persistent);
    }

    public synchronized void putInt(String key, int value, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(key, value);
            editor.commit();
        }
        ConfigurationManager.getInstance().set(key, value);
    }

    public synchronized void putInt(String key, int value) {
        putInt(key, value, isPersistent());
    }

    public synchronized int getInt(String key, int defValue, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPref.getInt(key, defValue);
        } else {
            return ConfigurationManager.getInstance().getInt(key, defValue);
        }
    }

    public synchronized int getInt(String key, int defValue) {
        return getInt(key, defValue, isPersistent());
    }

    public synchronized int getInt(String key) {
        return getInt(key, 0, isPersistent());
    }

    public synchronized int getInt(String key, boolean persistent) {
        return getInt(key, 0, persistent);
    }

    public synchronized void putLong(String key, long value, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(key, value);
            editor.commit();
        }
        ConfigurationManager.getInstance().set(key, value);
    }

    public synchronized void putLong(String key, long value) {
        putLong(key, value, isPersistent());
    }

    public synchronized long getLong(String key, long defValue, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPref.getLong(key, defValue);
        } else {
            return ConfigurationManager.getInstance().getLong(key, defValue);
        }
    }

    public synchronized long getLong(String key, long defValue) {
        return getLong(key, defValue, isPersistent());
    }

    public synchronized long getLong(String key) {
        return getLong(key, 0, isPersistent());
    }

    public synchronized long getLong(String key, boolean persistent) {
        return getLong(key, 0, persistent);
    }

    public synchronized void putBoolean(String key, boolean value, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(key, value);
            editor.commit();
        }
        ConfigurationManager.getInstance().set(key, value);
    }

    public synchronized void putBoolean(String key, boolean value) {
        putBoolean(key, value, isPersistent());
    }

    public synchronized boolean getBoolean(String key, boolean defValue, boolean persistent) {
        if (persistent) {
            final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPref.getBoolean(key, defValue);
        } else {
            return ConfigurationManager.getInstance().getBoolean(key, defValue);
        }
    }

    public synchronized boolean getBoolean(String key) {
        return getBoolean(key, false, isPersistent());
    }

    public synchronized boolean getBoolean(String key, boolean persistent) {
        return getBoolean(key, false, persistent);
    }

    public synchronized void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.registerOnSharedPreferenceChangeListener(listener);
    }

    public synchronized void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        final SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
