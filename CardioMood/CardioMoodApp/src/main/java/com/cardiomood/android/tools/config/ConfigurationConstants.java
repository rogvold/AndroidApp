package com.cardiomood.android.tools.config;

public interface ConfigurationConstants {

    String FLURRY_API_KEY           = "KC9R5WHFTBYZQNKNDJX3";
    String SHINOBI_CHARTS_API_KEY   =  "DVV28uA7FXRiD+UMjAxNDAzMjRpbmZvQHNoaW5vYmljb250cm9scy5jb20=dBojhaYWgRWi56ysuwnVEVqU6zNFtZi5IGwx6DSxf/UUCepFZ68RECBCVtRODYWIOKFoc0WaXDfGKf+813zC5ipQ6SFSoyGWQGZCEtP4YMZyLbaEORdC5qx3Tqxd2i3DyCScq+TTvsgMjnZMtV5cZNgDNW1E=BQxSUisl3BaWf/7myRmmlIjRnMU2cA7q+/03ZX9wdj30RzapYANf51ee3Pi8m2rVW6aD7t6Hi4Qy5vv9xpaQYXF5T7XzsafhzS3hbBokp36BoJZg8IrceBj742nQajYyV7trx5GIw9jy/V6r0bvctKYwTim7Kzq+YPWGMtqtQoU=PFJTQUtleVZhbHVlPjxNb2R1bHVzPnh6YlRrc2dYWWJvQUh5VGR6dkNzQXUrUVAxQnM5b2VrZUxxZVdacnRFbUx3OHZlWStBK3pteXg4NGpJbFkzT2hGdlNYbHZDSjlKVGZQTTF4S2ZweWZBVXBGeXgxRnVBMThOcDNETUxXR1JJbTJ6WXA3a1YyMEdYZGU3RnJyTHZjdGhIbW1BZ21PTTdwMFBsNWlSKzNVMDg5M1N4b2hCZlJ5RHdEeE9vdDNlMD08L01vZHVsdXM+PEV4cG9uZW50PkFRQUI8L0V4cG9uZW50PjwvUlNBS2V5VmFsdWU+";

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

    // measurement settings
    String MEASUREMENT_UNLIMITED_LENGTH = "measurement.unlimited_length";

    // connection settings
    String CONNECTION_DISABLE_BT_ON_CLOSE = "connection.disable_bluetooth_on_close";
	
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
