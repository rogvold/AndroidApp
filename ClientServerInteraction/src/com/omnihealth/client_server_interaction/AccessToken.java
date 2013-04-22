package com.omnihealth.client_server_interaction;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 21.04.13
 * Time: 15:47
 * To change this template use File | Settings | File Templates.
 */
public class AccessToken {

    private String token;
    private long expiredDate;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(long expiredDate) {
        this.expiredDate = expiredDate;
    }
}
