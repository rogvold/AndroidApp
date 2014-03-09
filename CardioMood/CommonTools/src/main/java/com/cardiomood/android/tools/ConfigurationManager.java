package com.cardiomood.android.tools;

import android.text.TextUtils;
import android.util.Log;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class ConfigurationManager {

    private static final String TAG = "CardioMood.CommonTools.ConfigurationManager";

	private final Map<String, Object> config;
	
	private ConfigurationManager() {
		config = Collections.synchronizedMap(new HashMap<String, Object>());
		loadProperties("application.properties");
	}

    private static class Holder {
		private static final ConfigurationManager instance = new ConfigurationManager();
	}
	
	public static ConfigurationManager getInstance() {
		return Holder.instance;
	}
	
	public final void loadProperties(String resourceName) {
        Properties props = new Properties();
		try {
            // Property file should be in 'com.cardiomood.android.tools' package.
            InputStream in = ConfigurationManager.class.getResourceAsStream(resourceName);
			if (in != null) {
				props.load(in);
				for(Entry<Object, Object> entry: props.entrySet()) {
					config.put(entry.getKey().toString(), entry.getValue());
				}
                in.close();
			}
		} catch (Exception ex) {
            Log.e(TAG, "Failed to load property file: " + resourceName, ex);
		}
	}
	
	public String getString(String key) {
	    Object v = get(key);
        return (v == null ? null : v.toString());
	}
	
	public String getString(String key, String defaultValue) {
		String value = getString(key);
		return TextUtils.isEmpty(value) ? defaultValue : value;
	}

    public void set(String key, Object value) {
        config.put(key, value);
    }

    public <T> T get(String key) {
        return (T) config.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        T v = get(key);
        return v == null ? defaultValue : v;
    }
	
	public void setString(String key, String value) {
		set(key, value);
	}

	public Integer getInteger(String key) {
		String value = getString(key, null);
		if (value!= null && TextUtils.isDigitsOnly(value)) {
			return Integer.valueOf(value);
		}
		return null;
	}
	
	public Integer getInteger(String key, Integer defaultValue) {
		Integer value = getInteger(key);
		return value == null ? defaultValue : value;
	}

    public int getInt(String key, int defValue) {
        return getInteger(key, defValue);
    }

    public Float getFloat(String key, Float defValue) {
        Object v = get(key);
        if (v instanceof String) {
            try {
                return Float.valueOf((String) v);
            } catch (Exception ex) {
                Log.w(TAG, "getFloat(): failed to parse parameter value for key = " + key, ex);
                return defValue;
            }
        } else if (v instanceof Float) {
            return (Float) v;
        }
        return defValue;
    }

    public boolean getBoolean(String key, boolean defValue) {
        Object v = get(key);
        if (v instanceof String) {
            try {
                return Boolean.valueOf((String) v);
            } catch (Exception ex) {
                Log.w(TAG, "getBoolean(): failed to parse parameter value for key = " + key, ex);
                return defValue;
            }
        } else if (v instanceof Boolean) {
            return (Boolean) v;
        }
        return defValue;
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        String value = getString(key, null);
        if (value!= null && TextUtils.isDigitsOnly(value)) {
            return Long.valueOf(value);
        }
        return defaultValue;
    }
	
	
}
