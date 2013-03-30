package com.omnihealth.client_server_interaction;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 30.03.13
 * Time: 23:56
 * To change this template use File | Settings | File Templates.
 */
public interface ServerResponseCallback<T> {

    public void onResponse(T result, Exception exp);
}
