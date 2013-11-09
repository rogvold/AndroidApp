package com.cardiomood.android.services;

import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.cardiomood.android.tools.config.ConfigurationManager;

import org.codegist.crest.CRestBuilder;

public abstract class CardioMoodService implements ConfigurationConstants {
	
	private static volatile boolean rebuildRequired = false;
	
	public static ICardioMoodService getInstance() {
		if (rebuildRequired) {
			rebuildService();
		}
		return Holder.instance;
	}
	
	private static abstract class Holder {
		private static volatile ICardioMoodService instance = getInstance();
		
		private static ICardioMoodService getInstance() {
			rebuildRequired = false;
			ConfigurationManager conf = ConfigurationManager.getInstance();
			CRestBuilder builder = new CRestBuilder();
			builder.placeholder(SERVICE_PROTOCOL, conf.getString(SERVICE_PROTOCOL, DEFAULT_SERVICE_PROTOCOL));
			builder.placeholder(SERVICE_HOST, conf.getString(SERVICE_HOST, DEFAULT_SERVICE_HOST));
			builder.placeholder(SERVICE_PORT, conf.getString(SERVICE_PORT, DEFAULT_SERVICE_PORT));
			builder.placeholder(SERVICE_PATH, conf.getString(SERVICE_PATH, DEFAULT_SERVICE_PATH));
			return builder.build().build(ICardioMoodService.class);
		}
	}
	
	public static void rebuildService() {
		Holder.instance = Holder.getInstance();
	}
	
	public static void markRebuildRequired() {
		rebuildRequired = true;
	}
	
}
