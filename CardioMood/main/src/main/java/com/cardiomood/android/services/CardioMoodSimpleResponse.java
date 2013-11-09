package com.cardiomood.android.services;

public class CardioMoodSimpleResponse {
	private Integer response;
	private String error;
	
	public CardioMoodSimpleResponse(Integer response, String error) {
		super();
		this.response = response;
		this.error = error;
	}
	
	public CardioMoodSimpleResponse() {
		this(0, null);
	}

	public Integer getResponse() {
		return response;
	}

	public void setResponse(Integer response) {
		this.response = response;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return "CardioMoodSimpleResponse [response=" + response + ", error="
				+ error + "]";
	}
	
}
