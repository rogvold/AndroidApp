package com.cardiomood.data.json;

import com.google.gson.Gson;

import java.io.Serializable;

public class JsonRRInterval implements Serializable {

    private static final Gson GSON = new Gson();

    private Integer r;

    public Integer getR() {
        return r;
    }

    public void setR(Integer r) {
        this.r = r;
    }

    public String toString() {
        return GSON.toJson(this);
    }
}