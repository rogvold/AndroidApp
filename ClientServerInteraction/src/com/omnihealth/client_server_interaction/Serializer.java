package com.omnihealth.client_server_interaction;

import android.util.Log;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 30.03.13
 * Time: 23:47
 * To change this template use File | Settings | File Templates.
 */
public class Serializer {

    public static String makeQueryString(Map params) {
        String queryString = "";
        if (params != null) {
            for (Object key : params.keySet()) {
                queryString += String.format(queryString.equals("") ? "%s=%s" : "&%s=%s", key, params.get(key));
            }
        }
        return queryString;
    }

    private static List<Integer> deserializeRates(JSONArray data) throws JSONException {
        List<Integer> rates = new ArrayList<Integer>();
        for (int i = 0, length = data.length(); i < length; i++) {
            rates.add((Integer)data.get(i));
        }
        return rates;
    }

    private static List<double[]> deserializeTension(JSONArray data) throws JSONException {
        List<double[]> tension = new ArrayList<double[]>();
        for (int i = 0, length = data.length(); i < length; i++) {
            JSONArray item = (JSONArray)data.get(i);
            tension.add(new double[] {new Double(item.get(0).toString()), new Double(item.get(1).toString())});

        }
        return tension;
    }

    private static User deserializeUser(JSONObject json) {
        User user = new User();
        user.setAbout(json.optString("about"));
        user.setBirthDate(json.optString("birthDate"));
        user.setDepartment(json.optString("department"));
        user.setDescription(json.optString("description"));
        user.setDiagnosis(json.optString("diagnosis"));
        user.setEmail(json.optString("email"));
        user.setFirstName(json.optString("firstName"));
        user.setLastName(json.optString("lastName"));
        user.setHeight(json.optDouble("height"));
        user.setPassword(json.optString("password"));
        user.setSex(json.optInt("sex"));
        user.setStatusMessage(json.optString("statusMessage"));
        user.setWeight(json.optDouble("weight"));
        return user;
    }

    private static List<Session> deserializeSessionList(JSONArray data) throws JSONException {
        List<Session> sessions = new ArrayList<Session>();

        for (int i = 0, length = data.length(); i < length; i++) {
            sessions.add(deserializeSession((JSONObject)data.get(i)));
        }
        return sessions;
    }

    private static Session deserializeSession(JSONObject data) throws JSONException {
        Session session = new Session();
        session.setSessionId(data.getLong("id"));
        session.setStart(data.getLong("start"));
        session.setEnd(data.getLong("end"));
        return session;
    }

    private static AccessToken deserializeAccessToken(JSONObject json) throws JSONException {
        AccessToken at = new AccessToken();
        at.setToken(json.getString("token"));
        at.setExpiredDate(json.getLong("expiredDate"));
        return at;
    }

    public static String serialize(Object json) {
        Gson gson = new Gson();
        return gson.toJson(json);
    }

    public static Object deserialize(String json, int classId) throws ServerResponseError, JSONException {
        JSONObject jsonObject = new JSONObject(json);
        int responseCode = jsonObject.getInt("responseCode");
        if (responseCode == 1) {
            return deserializeData(jsonObject.get("data"), classId);
        } else {
            JSONObject error = new JSONObject(jsonObject.getString("error"));
            throw new ServerResponseError(error.getString("message"), error.getInt("code"));
        }
    }

    private static Object deserializeData(Object data, int classId) throws JSONException {
        switch (classId) {
            case ClassIds.INT_ID:
                return data;
            case ClassIds.USER_ID:
                return deserializeUser((JSONObject)data);
            case ClassIds.ACCESS_TOKEN_ID:
                return deserializeAccessToken((JSONObject)data);
            case ClassIds.LIST_OF_SESSONS_ID:
                return deserializeSessionList((JSONArray)data);
            case ClassIds.TENSION_ID:
                return deserializeTension((JSONArray)data);
            case ClassIds.RATES_ID:
                return deserializeRates((JSONArray)data);
        }
        return null;
    }





}
