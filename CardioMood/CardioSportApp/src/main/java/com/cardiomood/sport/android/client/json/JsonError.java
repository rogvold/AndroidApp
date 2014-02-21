package com.cardiomood.sport.android.client.json;

/**
 * Project: CardioSport
 * User: danon
 * Date: 15.06.13
 * Time: 13:47
 */
public class JsonError {

    public static final Integer INVALID_TOKEN_CODE = 2;
    public static final Integer ANY_ERROR_CODE = 3;

    private String message;
    private Integer code;

    public JsonError() {
    }

    public JsonError(String message, Integer code) {
        this.message = message;
        this.code = code;
    }


    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}
