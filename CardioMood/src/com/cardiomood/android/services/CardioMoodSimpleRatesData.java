package com.cardiomood.android.services;

import java.io.Serializable;
import java.util.List;


public class CardioMoodSimpleRatesData implements Serializable {

	private static final long serialVersionUID = -1211936695645363802L;
	
	public static final String DATE_MASK = "yyyy-MM-dd HH:mm:ss.SSS";
    
	private String deviceId;
    private String email;
    private String password;
    private List<Integer> rates;
    private Long start;
    private int create;

    public CardioMoodSimpleRatesData(String deviceId, String email, String password, List<Integer> rates, Long start, int create) {
        this.deviceId = deviceId;
        this.email = email;
        this.password = password;
        this.rates = rates;
        this.start = start;
        this.create = create;
    }

    public CardioMoodSimpleRatesData() {
        super();
    }

    public int getCreate() {
        return create;
    }

    public void setCreate(int create) {
        this.create = create;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Integer> getRates() {
        return rates;
    }

    public void setRates(List<Integer> rates) {
        this.rates = rates;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    @Override
    public String toString() {
        return " \n{\n  email = " + email + "\n"
                + " password = " + password + "\n"
                + " rates = " + rates + "\n"
                + " create = " + create + "\n"
                + " start = " + start + "\n}";
    }
}
