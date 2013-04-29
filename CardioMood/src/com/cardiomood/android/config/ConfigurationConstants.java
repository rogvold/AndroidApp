package com.cardiomood.android.config;

public interface ConfigurationConstants {
	// service
	String SERVICE_PROTOCOL			= "app.service_protocol";
	String SERVICE_HOST				= "app.service_host";
	String SERVICE_PORT				= "app.service_port";
	String SERVICE_PATH				= "app.service_path";
	
	String DEFAULT_SERVICE_PATH		= "/BaseProjectWeb/resources";
	String DEFAULT_SERVICE_PROTOCOL	= "http";
	String DEFAULT_SERVICE_HOST		= "www.cardiomood.com";
	String DEFAULT_SERVICE_PORT		= "80";
	
	// user profile
	String USER_EMAIL_KEY			= "user.email";
	String USER_PASSWORD_KEY		= "user.password";
    String USER_ACCESS_TOKEN_KEY    = "user.access_token";
	String USER_FIRST_NAME_KEY		= "user.first_name";
	String USER_LAST_NAME_KEY		= "user.last_name";
	String USER_ABOUT_KEY			= "user.about";
	String USER_DESCRIPTION_KEY		= "user.description";
	String USER_DIAGNOSIS_KEY		= "user.diagnosis";
	String USER_STATUS_KEY			= "user.status";
	String USER_DEPARTMENT_KEY		= "user.department";
	String USER_WEIGHT_KEY			= "user.weight";
	String USER_HEIGHT_KEY			= "user.height";
	String USER_AGE_KEY				= "user.age";
	String USER_SEX_KEY				= "user.sex";
	
	// analysis
	String WINDOW_DURATION_KEY		= "analysis.window_duration";
	String STEP_DURATION_KEY		= "analysis.step_duration";
}
