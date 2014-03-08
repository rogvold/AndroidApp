package com.cardiomood.data;

/**
 * Created by danon on 08.03.14.
 */
public interface ServerConstants {

    Long CARDIOMOOD_CLINET_ID       = 51L;

    // service
    String SERVICE_PROTOCOL			= "app.service_protocol";
    String SERVICE_HOST				= "app.service_host";
    String SERVICE_PORT				= "app.service_port";
    String SERVICE_PATH				= "app.service_path";

    String DEFAULT_SERVICE_PATH		= "/CardioDataWeb/resources";
    String DEFAULT_SERVICE_PROTOCOL	= "http";
    String DEFAULT_SERVICE_HOST		= "data.cardiomood.com";
    String DEFAULT_SERVICE_PORT		= "80";

}
