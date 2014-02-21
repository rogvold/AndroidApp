package com.cardiomood.sport.android.client.json;

/**
 * Project: CardioSport
 * User: danon
 * Date: 15.06.13
 * Time: 13:46
 */
public class JsonResponse<T> {
    private Integer responseCode;
    private JsonError error;
    private T data;

    public JsonResponse() {

    }

    public JsonResponse(Integer responseCode, JsonError error, T data) {
        this.responseCode = responseCode;
        this.error = error;
        this.data = data;
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

    public void setData(T data) {
        this.data = data;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }



}
