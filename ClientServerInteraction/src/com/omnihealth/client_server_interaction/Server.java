package com.omnihealth.client_server_interaction;

import com.omnihealth.client_server_interaction.async.HttpAsyncTask;

import java.util.ArrayList;
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
    private static final String AUTHORIZATION = "SecureAuth/";
    private static final String TOKEN = "token/";
    private static final String SESSIONS = "SecureSessions/";
    private static final String INDICATORS = "SecureIndicators/";
    private static final String RATES = "SecureRatesUploading/";


    public static void logIn(String email, String password, String deviceId, ServerResponseCallback<AccessToken> callback) {
        Map content = new HashMap();
        content.put("email", email);
        content.put("password", password);
        content.put("deviceId", deviceId);
        baseRequest(TOKEN + "authorize", content, null, callback, ClassIds.ACCESS_TOKEN_ID);
    }

    public static void validateEmail(String email, ServerResponseCallback<Integer> callback) {
        Map content = new HashMap();
        content.put("email", email);
        baseRequest(AUTHORIZATION + "check_existence", content, null, callback, ClassIds.INT_ID);
    }

    public static void register(String email, String password, ServerResponseCallback<Integer> callback) {
        Map content = new HashMap();
        content.put("email", email);
        content.put("password", password);
        baseRequest(AUTHORIZATION + "register", content, null, callback, ClassIds.INT_ID);
    }

    public static void checkData(String email, String password, ServerResponseCallback<Integer> callback) {
        Map content = new HashMap();
        content.put("email", email);
        content.put("password", password);
        baseRequest(AUTHORIZATION + "check_data", content, null, callback, ClassIds.INT_ID);
    }

    public static void getInfo(String accessToken, ServerResponseCallback<User> callback) {
        Map content = new HashMap();
        content.put("token", accessToken);
        baseRequest(AUTHORIZATION + "info", content, null, callback, ClassIds.USER_ID);
    }

    public static void updateInfo(String accessToken, User user, ServerResponseCallback<Integer> callback) {
        Map content = new HashMap();
        content.put("token", accessToken);
        baseRequest(AUTHORIZATION + "update_info", content, user, callback, ClassIds.INT_ID);
    }

    public static void upload(String accessToken, long start, List<Integer> rates,
                              boolean create, ServerResponseCallback<Integer> callback) {
        Map content = new HashMap();
        content.put("token", accessToken);
        Map json = new HashMap();
        json.put("start", start);
        json.put("rates", rates);
        json.put("create", create ? 1 : 0);
        baseRequest(RATES + "upload", content, json, callback, ClassIds.INT_ID);
    }

    public static void sync(String accessToken, long start, List<Integer> rates,
                              boolean create, ServerResponseCallback<Integer> callback) {
        Map content = new HashMap();
        content.put("token", accessToken);
        Map json = new HashMap();
        json.put("start", start);
        json.put("rates", rates);
        json.put("create", create ? 1 : 0);
        baseRequest(RATES + "sync", content, json, callback, ClassIds.INT_ID);
    }

    public static void getAllSessions(String accessToken, ServerResponseCallback<List<Session>> callback)
    {
        Map content = new HashMap();
        content.put("token", accessToken);
        baseRequest(SESSIONS + "all", content, null, callback, ClassIds.LIST_OF_SESSONS_ID);
    }

    public static void getTension(String accessToken, long sessionId, ServerResponseCallback<List<double[]>> callback)
    {
        Map content = new HashMap();
        content.put("token", accessToken);
        baseRequest(INDICATORS + sessionId + "/tension", content, null, callback, ClassIds.TENSION_ID);
    }

    public static void getRates(String accessToken, long sessionId, ServerResponseCallback<List<Integer>> callback)
    {
        Map content = new HashMap();
        content.put("token", accessToken);
        content.put("sessionId", sessionId);
        baseRequest(SESSIONS + "rates", content, null, callback, ClassIds.RATES_ID);
    }

    private static <T> void baseRequest(String urlSuffix, Map queryContent, Object json, ServerResponseCallback<T> callback,
                                        int classId) {
        String url = SERVER_BASE + RESOURCES + urlSuffix;
        String jsonString = json == null ? null : Serializer.serialize(json);
        if (queryContent == null)
            queryContent = new HashMap();
        if (jsonString != null)
            queryContent.put("json", jsonString);
        String content = Serializer.makeQueryString(queryContent);
        new HttpAsyncTask<T>(callback, classId).execute(url, content);
    }
}
