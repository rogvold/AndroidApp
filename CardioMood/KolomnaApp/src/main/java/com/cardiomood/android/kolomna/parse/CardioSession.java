package com.cardiomood.android.kolomna.parse;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import org.json.JSONArray;

/**
 * Created by antondanhsin on 20/10/14.
 */

@ParseClassName("CardioSession")
public class CardioSession extends ParseObject {

    public String getName() {
        return getString("name");
    }

    public String getUserId() {
        return getString("userId");
    }

    public JSONArray getRrs() {
        return getJSONArray("rrs");
    }

    public JSONArray getT() {
        return getJSONArray("times");
    }

    public long getStartTimestamp() {
        return getLong("startTimestamp");
    }

    public long getEndTimestamp() {
        return getLong("endTimestamp");
    }
}
