package com.cardiomood.data.json;

import java.io.Serializable;

/**
 *
 * @author Shaykhlislamov Sabir (email: sha-sabir@yandex.ru)
 */
public class JsonResponse<T> implements Serializable {

    public static final Integer RESPONSE_OK = 1;
    public static final Integer RESPONSE_ERROR = 0;

    private Integer responseCode;
    private JsonError error;
    private T data;

    public JsonResponse(Integer responseCode, JsonError error, T object) {
        this.responseCode = responseCode;
        this.error = error;
        this.data = object;
    }

    public JsonResponse(Integer responseCode, T data) {
        this.responseCode = responseCode;
        this.data = data;
    }

    public JsonResponse(JsonError error) {
        this.responseCode = RESPONSE_ERROR;
        this.error = error;
    }

    public JsonResponse(T data) {
        this.responseCode = RESPONSE_OK;
        this.data = data;
    }

    public JsonResponse() {
    }

    public JsonError getError() {
        return error;
    }

    public void setError(JsonError error) {
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
}
