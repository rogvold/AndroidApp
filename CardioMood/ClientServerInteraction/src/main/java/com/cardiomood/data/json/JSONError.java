package com.cardiomood.data.json;

import java.io.Serializable;

/**
 *
 * @author Shaykhlislamov Sabir (email: sha-sabir@yandex.ru)
 */
public class JsonError implements Serializable {

    public static final Integer NORMAL_ERROR = 20;
    public static final Integer INVALID_TOKEN_ERROR = 21;
    public static final Integer LOGIN_FAILED_ERROR = 22;
    public static final Integer NOT_AUTHORIZED_ERROR = 23;
    public static final Integer ACCESS_DENIED_ERROR = 24;
    public static final Integer REGISTRATION_FAILED_ERROR = 25;
    public static final Integer EMPTY_RESPONSE_ERROR = -1;
    public static final Integer INVALID_RESPONSE_CODE_ERROR = -2;
    public static final Integer SERVICE_ERROR = -3;

    private String message;
    private Integer code;

    public JsonError(String message) {
        this.message = message;
        this.code = NORMAL_ERROR;
    }

    public JsonError(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public JsonError() {
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

    @Override
    public String toString() {
        return "JsonError{" +
                "message='" + message + '\'' +
                ", code=" + code +
                '}';
    }
}
