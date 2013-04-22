package com.omnihealth.client_server_interaction;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 21.04.13
 * Time: 15:09
 * To change this template use File | Settings | File Templates.
 */
public class ServerResponse<T> {

    public static final int OK = 0;
    public static final int Error = 1;
    public static final int ServerError = 2;

    private T response;
    private Exception error;
    private ServerResponseError serverError;
    private int responseCode;

    public ServerResponse(int responseCode, Object response) {
        switch (responseCode) {
            case OK:
                this.response = (T)response;
                break;
            case Error:
                error = (Exception)response;
                break;
            case ServerError:
                serverError = (ServerResponseError)response;
                break;
        }
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public T getResponse() {
        return response;
    }

    public Exception getError() {
        return error;
    }

    public ServerResponseError getServerError() {
        return serverError;
    }

}
