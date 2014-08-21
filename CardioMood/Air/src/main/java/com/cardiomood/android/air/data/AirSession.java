package com.cardiomood.android.air.data;

import com.parse.ParseClassName;
import com.parse.ParseObject;

/**
 * Created by danon on 14.08.2014.
 */
@ParseClassName("AirSession")
public class AirSession extends ParseObject {

    public String getAircraftId() {
        return getString("aircraftId");
    }

    public void setAircraftId(String aircraftId) {
        put("aircraftId", aircraftId);
    }

    public String getUserId() {
        return getString("userId");
    }

    public void setUserId(String userId) {
        put("userId", userId);
    }

    public Long getEndDate() {
        return has("endDate") ? getLong("aircraftId") : null;
    }

    public void setEndDate(Long endDate) {
        put("endDate", endDate);
    }

}
