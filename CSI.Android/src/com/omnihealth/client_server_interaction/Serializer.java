package com.omnihealth.client_server_interaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private static String SECRET = "h7a7RaRtvAVwnMGq5BV6";

    public static String makeQueryString(Map params) {
        String queryString = "?secret=" + SECRET;
        if (params != null) {
            for (Object key : params.keySet()) {
                queryString += String.format("&%s=%s", key, params.get(key));
            }
        }
        return queryString;
    }

    public static User deserializeUser(JSONObject json) {
        User user = new User();
        user.setAbout(json.optString("about"));
        user.setAge(json.optInt("age"));
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

    public static String serializeUser(User user) throws JSONException {
        JSONObject json = new JSONObject();
        if (user.getAbout() != null)
            json.putOpt("about", user.getAbout());
        json.putOpt("age", user.getAge());
        if (user.getDepartment() != null)
            json.putOpt("department", user.getDepartment());
        if (user.getDescription() != null)
            json.putOpt("description", user.getDescription());
        if (user.getFirstName() != null)
            json.putOpt("firstName", user.getFirstName());
        if (user.getLastName() != null)
            json.putOpt("lastName", user.getLastName());
        if (user.getDiagnosis() != null)
            json.putOpt("diagnosis", user.getDiagnosis());
        if (user.getEmail() != null)
            json.putOpt("email", user.getEmail());
        if (user.getPassword() != null)
            json.putOpt("password", user.getPassword());
        json.putOpt("weight", user.getWeight());
        json.putOpt("height", user.getHeight());
        return json.toString();
    }

    public static String serializeSession(String email, String password, List<Integer> rates, long start) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("email", email);
        json.put("password", password);
        JSONArray jArr = new JSONArray();
        for (int rate : rates) {
            jArr.put(rate);
        }
        json.put("rates", jArr);
        json.put("start", start);
        json.put("create", 1);
        return json.toString();
    }
}
