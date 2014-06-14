package com.cardiomood.data.json;

import java.io.Serializable;

/**
 *
 * @author Shaykhlislamov Sabir (email: sha-sabir@yandex.ru)
 */
public class JSONResponse<T> implements Serializable {

    public static final Integer RESPONSE_OK = 1;
    public static final Integer RESPONSE_ERROR = 0;

    private Integer responseCode;
    private JSONError error;
    private T data;
    private long serverTime;

    public JSONResponse(Integer responseCode, JSONError error, T object) {
        this.responseCode = responseCode;
        this.error = error;
        this.data = object;
    }

    public JSONResponse(Integer responseCode, T data) {
        this.responseCode = responseCode;
        this.data = data;
    }

    public JSONResponse(JSONError error) {
        this.responseCode = RESPONSE_ERROR;
        this.error = error;
    }

    public JSONResponse(T data) {
        this.responseCode = RESPONSE_OK;
        this.data = data;
    }

    public JSONResponse() {
    }

    public JSONError getError() {
        return error;
    }

    public void setError(JSONError error) {
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public void setData(T object) {
        this.data = object;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public boolean isOk() {
        return RESPONSE_OK.equals(responseCode);
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    @Override
    public String toString() {
        return "JsonResponse{" +
                "responseCode=" + responseCode +
                ", error=" + error +
                ", data=" + (data == null ? "null" : "<some data>") +
                '}';
    }
}
