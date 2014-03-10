package com.cardiomood.data.json;

import java.util.ArrayList;
import java.util.List;

public class CardioSessionWithData extends CardioSession {

    protected List<CardioDataItem> dataItems;

    public CardioSessionWithData(List<CardioDataItem> dataItems, Long id, String name, String description, Long serverId, Long userId, Long creationTimestamp, String dataClassName) {
        super(id, name, description, serverId, userId, creationTimestamp, dataClassName);
        this.dataItems = dataItems;
    }

    public CardioSessionWithData() {
        dataItems = new ArrayList<CardioDataItem>();
    }

    public CardioSessionWithData(CardioSession session) {
        super(session.id, session.name, session.description, session.serverId, session.userId, session.creationTimestamp, session.dataClassName);
        dataItems = new ArrayList<CardioDataItem>();
    }

    public List<CardioDataItem> getDataItems() {
        return dataItems;
    }

    public void setDataItems(List<CardioDataItem> dataItems) {
        this.dataItems = dataItems;
    }
}