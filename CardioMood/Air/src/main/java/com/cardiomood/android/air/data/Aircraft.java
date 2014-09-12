package com.cardiomood.android.air.data;

import com.parse.ParseClassName;
import com.parse.ParseObject;

/**
 * Created by danon on 14.08.2014.
 */
@ParseClassName("Aircraft")
public class Aircraft extends ParseObject {

    public String getName() {
        return getString("name");
    }

    public String getAircraftId() {
        return getString("aircraftId");
    }

    public String getAircraftType() {
        return getString("aircraftType");
    }

    public String getCallName() {
        return getString("callName");
    }

}
