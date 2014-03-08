package com.cardiomood.data.json;

import java.io.Serializable;

public class CardioDataItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String dataItem;
    private Long sessionId;
    private Long number;
    private Long creationTimestamp;

    public CardioDataItem() {
    }

    public CardioDataItem(String dataItem, Long sessionId, Long number, Long creationTimestamp) {
        this.dataItem = dataItem;
        this.sessionId = sessionId;
        this.number = number;
        this.creationTimestamp = creationTimestamp;
    }

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getDataItem() {
        return dataItem;
    }

    public void setDataItem(String dataItem) {
        this.dataItem = dataItem;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}