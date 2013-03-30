package com.omnihealth.client_server_interaction;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 31.03.13
 * Time: 0:11
 * To change this template use File | Settings | File Templates.
 */
public class ServerResponseError extends Exception {

    public ServerResponseError(String message) {
        super(message);
    }
}
