package com.cardiomood.data;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.data.async.ServerResponseCallback;
import com.cardiomood.data.async.ServerResponseCallbackRetry;
import com.cardiomood.data.async.ServiceTask;
import com.cardiomood.data.json.ApiToken;
import com.cardiomood.data.json.CardioSession;
import com.cardiomood.data.json.CardioSessionWithData;
import com.cardiomood.data.json.JsonError;
import com.cardiomood.data.json.JsonResponse;
import com.cardiomood.data.json.UserProfile;
import com.google.gson.Gson;

import java.util.List;

/**
 * Created by danon on 08.03.14.
 */
public class DataServiceHelper {

    private static final String TAG = DataServiceHelper.class.getSimpleName();

    private static final Gson GSON = new Gson();

    private static final String OFFLINE_MODE_KEY         = "app.offline_mode";

    // user profile
    private static final String USER_EXTERNAL_ID		 = "user.external_id";
    private static final String USER_EMAIL_KEY			 = "user.email";
    private static final String USER_PASSWORD_KEY		 = "user.password";
    private static final String USER_ACCESS_TOKEN_KEY    = "user.access_token";

    private CardioMoodDataService mService;
    private ApiToken mToken = null;

    private boolean signedIn = false;
    private String email = null;
    private String password = null;
    private boolean offlineMode;

    public DataServiceHelper(CardioMoodDataService service) {
        this.mService = service;
    }

    public DataServiceHelper(CardioMoodDataService service, PreferenceHelper pHelper) {
        this.mService = service;
        this.mToken = new ApiToken(pHelper.getLong(USER_EXTERNAL_ID), pHelper.getString(USER_ACCESS_TOKEN_KEY), System.currentTimeMillis()+100000);
        this.email = pHelper.getString(USER_EMAIL_KEY);
        this.password = pHelper.getString(USER_PASSWORD_KEY);
        this.offlineMode = pHelper.getBoolean(OFFLINE_MODE_KEY);
        this.signedIn = true;
    }

    public boolean isSignedIn() {
        return signedIn;
    }

    public void setSignedIn(boolean signedIn) {
        this.signedIn = signedIn;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    public ApiToken getToken() {
        return mToken;
    }

    public Long getUserId() {
        return (mToken == null ? null : mToken.getUserId());
    }

    public String getTokenString() {
        return (getToken() == null ? "" : getToken().getToken());
    }

    public void setToken(ApiToken token) {
        mToken = token;
    }

    public synchronized void logout() {
        signedIn = false;
        password = null;
        email = null;
    }

    public JsonResponse<ApiToken> login(String email, String password) {
        try {
            return mService.login(email, password);
        } catch (Exception ex) {
            Log.w(TAG, "login() -> failed with an exception", ex);
            return new JsonResponse<ApiToken>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void login(String email, String password, ServerResponseCallback<ApiToken> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<ApiToken>(new LoginCallback(email, password, callback)) {

            @Override
            protected JsonResponse<ApiToken> doInBackground(Object... params) {
                    Log.d(TAG, "email = " + params[0] + "; password = " + params[1]);
                    return login((String) params[0], (String) params[1]);
            }
        }.execute(email, password);
    }

    public JsonResponse<UserProfile> register(String email, String password) {
        try {
            return mService.register(email, password);
        } catch (Exception ex) {
            Log.w(TAG, "register() -> failed with an exception", ex);
            return new JsonResponse<UserProfile>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void register(String email, String password, ServerResponseCallback<UserProfile> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<UserProfile>(callback) {

            @Override
            protected JsonResponse<UserProfile> doInBackground(Object... params) {
                Log.d(TAG, "email = " + params[0] + "; password = " + params[1]);
                return register((String) params[0], (String) params[1]);
            }
        }.execute(email, password);
    }

    public JsonResponse<CardioSession> createSession() {
        try {
            if (isSignedIn()) {
                String token = getTokenString();
                Long userId = getUserId();
                return mService.createSession(token, userId, ServerConstants.CARDIOMOOD_CLINET_ID);
            } else {
                throw new IllegalStateException("Not signed in.");
            }
        } catch (Exception ex) {
            Log.w(TAG, "createSession() -> failed with an exception", ex);
            return new JsonResponse<CardioSession>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void createSession(ServerResponseCallbackRetry<CardioSession> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<CardioSession>(new HandleTokenExpiredCallback<CardioSession>(callback)) {

            @Override
            protected JsonResponse<CardioSession> doInBackground(Object... params) {
                return createSession();
            }
        }.execute();
    }

    public JsonResponse<List<CardioSession>> getSessions() {
        try {
            if (isSignedIn()) {
                String token = getTokenString();
                Long userId = getUserId();
                return mService.getSessionsOfUser(token, userId, ServerConstants.CARDIOMOOD_CLINET_ID);
            } else {
                throw new IllegalStateException("Not signed in.");
            }
        } catch (Exception ex) {
            Log.w(TAG, "getSessions() -> failed with an exception", ex);
            return new JsonResponse<List<CardioSession>>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void getSessions(ServerResponseCallbackRetry<List<CardioSession>> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<List<CardioSession>>(new HandleTokenExpiredCallback<List<CardioSession>>(callback)) {

            @Override
            protected JsonResponse<List<CardioSession>> doInBackground(Object... params) {
                return getSessions();
            }
        }.execute();
    }

    public JsonResponse<CardioSession> updateSessionInfo(Long sessionId, String name, String description) {
        try {
            if (isSignedIn()) {
                String token = getTokenString();
                Long userId = getUserId();
                return mService.updateSessionInfo(token, userId, sessionId, name, description);
            } else {
                throw new IllegalStateException("Not signed in.");
            }
        } catch (Exception ex) {
            Log.w(TAG, "updateSessionInfo() -> failed with an exception", ex);
            return new JsonResponse<CardioSession>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void updateSessionInfo(Long sessionId, String name, String description, ServerResponseCallbackRetry<CardioSession> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<CardioSession>(new HandleTokenExpiredCallback<CardioSession>(callback)) {

            @Override
            protected JsonResponse<CardioSession> doInBackground(Object... params) {
                return updateSessionInfo((Long) params[0], (String) params[1], (String) params[2]);
            }
        }.execute(sessionId, name, description);
    }

    public JsonResponse<CardioSessionWithData> getSessionData(Long sessionId) {
        try {
            if (isSignedIn()) {
                String token = getTokenString();
                Long userId = getUserId();
                return mService.getSessionData(token, userId, sessionId);
            } else {
                throw new IllegalStateException("Not signed in.");
            }
        } catch (Exception ex) {
            Log.w(TAG, "getSessionData() -> failed with an exception", ex);
            return new JsonResponse<CardioSessionWithData>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void getSessionData(Long sessionId, ServerResponseCallbackRetry<CardioSessionWithData> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<CardioSessionWithData>(new HandleTokenExpiredCallback<CardioSessionWithData>(callback)) {

            @Override
            protected JsonResponse<CardioSessionWithData> doInBackground(Object... params) {
                return getSessionData((Long) params[0]);
            }
        }.execute(sessionId);
    }


    public JsonResponse<String> deleteSession(Long sessionId) {
        try {
            if (isSignedIn()) {
                String token = getTokenString();
                Long userId = getUserId();
                return mService.deleteSession(token, userId, sessionId);
            } else {
                throw new IllegalStateException("Not signed in.");
            }
        } catch (Exception ex) {
            Log.w(TAG, "deleteSession() -> failed with an exception", ex);
            return new JsonResponse<String>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void deleteSession(Long sessionId, ServerResponseCallbackRetry<String> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<String>(new HandleTokenExpiredCallback<String>(callback)) {

            @Override
            protected JsonResponse<String> doInBackground(Object... params) {
                return deleteSession((Long) params[0]);
            }
        }.execute(sessionId);
    }

    public JsonResponse<String> rewriteCardioSessionData(CardioSessionWithData serializedData) {
        try {
            if (isSignedIn()) {
                String token = getTokenString();
                Long userId = getUserId();
                return mService.rewriteCardioSessionData(token, userId, GSON.toJson(serializedData));
            } else {
                throw new IllegalStateException("Not signed in.");
            }
        } catch (Exception ex) {
            Log.w(TAG, "rewriteCardioSessionData() -> failed with an exception", ex);
            return new JsonResponse<String>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void rewriteCardioSessionData(CardioSessionWithData serializedData, ServerResponseCallbackRetry<String> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<String>(new HandleTokenExpiredCallback<String>(callback)) {

            @Override
            protected JsonResponse<String> doInBackground(Object... params) {
                return rewriteCardioSessionData((CardioSessionWithData) params[0]);
            }
        }.execute(serializedData);
    }

    public JsonResponse<String> appendDataToSession(CardioSessionWithData serializedData) {
        try {
            if (isSignedIn()) {
                String token = getTokenString();
                Long userId = getUserId();
                return mService.appendDataToSession(token, userId, GSON.toJson(serializedData));
            } else {
                throw new IllegalStateException("Not signed in.");
            }
        } catch (Exception ex) {
            Log.w(TAG, "appendDataToSession() -> failed with an exception", ex);
            return new JsonResponse<String>(new JsonError("Service error: " + ex.getLocalizedMessage(), JsonError.SERVICE_ERROR));
        }
    }

    public void appendDataToSession(CardioSessionWithData serializedData, ServerResponseCallbackRetry<String> callback) {
        if (isOfflineMode())
            return;
        new ServiceTask<String>(new HandleTokenExpiredCallback<String>(callback)) {

            @Override
            protected JsonResponse<String> doInBackground(Object... params) {
                return appendDataToSession((CardioSessionWithData) params[0]);
            }
        }.execute(serializedData);
    }

    public void refreshToken() {
        if (!isSignedIn())
            return;
        String login = getEmail();
        String password = getPassword();
        login(login, password, null);
    }

    public void checkInternetAvailable(Context context, final ServerResponseCallback<Boolean> callback) {
        if (callback == null) {
            // nothing to do
            return;
        }
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                return CommonTools.isNetworkAvailable((Context) params[0]);
            }

            @Override
            protected void onPostExecute(Object o) {
                if (callback != null) {
                    if (o != null)
                        callback.onResult((Boolean) o);
                    else callback.onResult(false);
                }
            }
        }.execute(context);
    }


    private class HandleTokenExpiredCallback<T> implements ServerResponseCallback {

        private ServerResponseCallbackRetry<T> externalCallback;

        public HandleTokenExpiredCallback(ServerResponseCallbackRetry<T> externalCallback) {
            this.externalCallback = externalCallback;
        }

        @Override
        public void onResult(Object result) {
            if (externalCallback != null)
                externalCallback.onResult((T) result);
        }

        @Override
        public void onError(JsonError error) {
            if (error != null && JsonError.INVALID_TOKEN_ERROR.equals(error.getCode())) {
                String email = getEmail();
                String password = getPassword();
                logout();
                login(email, password, new ServerResponseCallback<ApiToken>() {
                    @Override
                    public void onResult(ApiToken result) {
                        if (externalCallback != null && externalCallback.isRetryRequired()) {
                            externalCallback.setRetryRequired(false);
                            externalCallback.retry();
                        }
                    }

                    @Override
                    public void onError(JsonError error) {
                        if (externalCallback != null)
                            externalCallback.onError(error);
                    }
                });
            } else {
                if (externalCallback != null)
                    externalCallback.onError(error);
            }
        }
    }

    private class LoginCallback implements ServerResponseCallback<ApiToken> {

        private ServerResponseCallback<ApiToken> externalCallback;
        private String login;
        private String password;

        public LoginCallback(String login, String password, ServerResponseCallback<ApiToken> externalCallback) {
            this.externalCallback = externalCallback;
            this.login = login;
            this.password = password;
        }

        @Override
        public void onResult(ApiToken result) {
            setToken(result);

            if (result != null) {
                setEmail(this.login);
                setPassword(this.password);
                setSignedIn(true);
            }

            if (externalCallback != null) {
                externalCallback.onResult(result);
            }
        }

        @Override
        public void onError(JsonError error) {
            if (externalCallback != null) {
                externalCallback.onError(error);
            }
        }

    }

}
