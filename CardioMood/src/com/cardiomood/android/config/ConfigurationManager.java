package com.cardiomood.android.config;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import android.text.TextUtils;

public class ConfigurationManager implements ConfigurationConstants {
	
	private final Map<String, Object> config;
	
	private ConfigurationManager() {
		config = new HashMap<String, Object>();
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
			InputStream in = ConfigurationManager.class.getResourceAsStream(resourceName);
			if (in != null) {
				props.load(in);
				for(Entry<Object, Object> entry: props.entrySet()) {
					config.put(entry.getKey().toString(), entry.getValue());
				}
			}
			in.close();
		} catch (Exception ex) {
			// shit happens
		}
	}
	
	public String getString(String key) {
		return (String) config.get(key);
	}
	
	public String getString(String key, String defaultValue) {
		String value = getString(key);
		return TextUtils.isEmpty(value) ? defaultValue : value;
	}
	
	public void setString(String key, String value) {
		config.put(key,  value);
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
	
	
}
