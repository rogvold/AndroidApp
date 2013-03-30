package com.omnihealth.client_server_interaction;

import android.util.Log;
import com.omnihealth.client_server_interaction.async.DefaultBooleanHttpAsyncTask;
import com.omnihealth.client_server_interaction.async.DefaultUserHttpAsyncTask;
import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 30.03.13
 * Time: 23:34
 * To change this template use File | Settings | File Templates.
 */
public class Server {
    private static final String SERVER_BASE = "http://www.cardiomood.com/BaseProjectWeb/";
    private static final String RESOURCES = "resources/";
    private static final String AUTHORIZATION = "auth/";
    private static final String RATES = "rates/";

    public static void validateEmail(String email, ServerResponseCallback<Boolean> callback) {
        commonAuthorizationRequest("check_existence", email, null, callback);
    }


    public static void register(String email, String password, ServerResponseCallback<Boolean> callback) {
        commonAuthorizationRequest("register", email, password, callback);
    }


    public static void checkData(String email, String password, ServerResponseCallback<Boolean> callback) {
        commonAuthorizationRequest("check_data", email, password, callback);
    }


    public static void getInfo(String email, String password, ServerResponseCallback<User> callback) {
        String url = SERVER_BASE + RESOURCES + AUTHORIZATION + "info";
        Map<String, String> queryStringMap = new HashMap<String, String>();
        queryStringMap.put("email", email);
        queryStringMap.put("password", password);
        String queryString = Serializer.makeQueryString(queryStringMap);

        new DefaultUserHttpAsyncTask(callback).execute(url, queryString, "");
    }


    public static void updateInfo(User user, ServerResponseCallback<User> callback) {
        String url = SERVER_BASE + RESOURCES + AUTHORIZATION + "update_info";
        String json = "";
        try {
            json = Serializer.serializeUser(user);
        } catch (JSONException e) {
            Log.e("Server", "Error serializing user", e);
        }
        Map<String, String> queryStringMap = new HashMap<String, String>();
        queryStringMap.put("json", json);
        String queryString = Serializer.makeQueryString(queryStringMap);

        new DefaultUserHttpAsyncTask(callback).execute(url, queryString, "");
    }


    public static void upload(String email, String password, List<Integer> rates, long start, ServerResponseCallback<Boolean>  callback) {
        commonUploadRequest("upload", email, password, rates, start, callback);
    }


    public static void sync(String email, String password, List<Integer> rates, long start, ServerResponseCallback<Boolean>  callback) {
        commonUploadRequest("sync", email, password, rates, start, callback);
    }


    private static void commonAuthorizationRequest(String suffix, String email, String password, ServerResponseCallback<Boolean> callback) {
        String url = SERVER_BASE + RESOURCES + AUTHORIZATION + suffix;
        Map<String, String> queryStringMap = new HashMap<String, String>();
        queryStringMap.put("email", email);
        if (password != null)
            queryStringMap.put("password", password);
        String queryString = Serializer.makeQueryString(queryStringMap);

        new DefaultBooleanHttpAsyncTask(callback).execute(url, queryString, "");
    }


    private static void commonUploadRequest(String suffix, String email, String password, List<Integer> rates, long start, ServerResponseCallback<Boolean>  callback) {
        String url = SERVER_BASE + RESOURCES + RATES + suffix;
        String json = "";
        try {
            json = "json=" + Serializer.serializeSession(email, password, rates, start);
        } catch (JSONException e) {
            Log.e("Server", "Error serializing session", e);
        }

        new DefaultBooleanHttpAsyncTask(callback).execute(url, null, json);
    }
}
