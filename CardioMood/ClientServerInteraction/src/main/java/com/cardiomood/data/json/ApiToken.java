package com.cardiomood.data.json;

import java.io.Serializable;

public class ApiToken implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String token;
    private Long expirationDate;

    public ApiToken() {
    }

    public ApiToken(Long userId, String token, Long expirationDate) {
        this.userId = userId;
        this.token = token;
        this.expirationDate = expirationDate;
    }

    public Long getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Long expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return token;
    }
}
