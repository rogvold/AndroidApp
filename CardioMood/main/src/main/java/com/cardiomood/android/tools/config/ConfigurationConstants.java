package com.cardiomood.android.tools.config;

public interface ConfigurationConstants {

    String FLURRY_API_KEY           = "KC9R5WHFTBYZQNKNDJX3";

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
    String USER_EXTERNAL_ID			= "user.external_id";
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
	String USER_BIRTH_DATE_KEY = "user.age";
	String USER_SEX_KEY				= "user.sex";
    String USER_PHONE_NUMBER_KEY    = "user.phone_number";
    String USER_LOGGED_IN           = "user.logged_in";
	
	// analysis
	String WINDOW_DURATION_KEY		= "analysis.window_duration";
	String STEP_DURATION_KEY		= "analysis.step_duration";

    // GPS parameters
    String GPS_COLLECT_LOCATION		= "data.gps.collect_gps_location";
    String GPS_COLLECT_SPEED		= "data.gps.collect_speed";
    String GPS_COLLECT_ALTITUDE		= "data.gps.collect_altitude";

    String GPS_UPDATE_TIME          = "data.gps.update_time";
    String GPS_UPDATE_DISTANCE      = "data.gps.update_distance";

    // General app parameters
    String APP_DATE_FORMAT          = "app.date_format";

    // Special DB constants
    String DB_GOOD_SESSION_ID       = "db.good_session_id";
    String DB_STRESSED_SESSION_ID   = "db.stressed_session_id";
    String DB_ATHLETE_SESSION_ID    = "db.athlete_session_id";
    String DB_BAD_SESSION_ID        = "db.bad_session_id";
}
