package com.cardiomood.android.server;

import android.util.Log;
import com.omnihealth.client_server_interaction.*;

import java.io.Serializable;
import java.util.List;

/**
 * Project: CardioMood for Android
 * User: danon
 * Date: 28.04.13
 * Time: 12:56
 */
public class CardioMood implements Serializable {
    private AccessToken accessToken;
    private String login; // = email
    private String password;
    private String deviceId;
    private boolean signedIn = false;

    private abstract static class Holder {
        private static final CardioMood INSTANCE = new CardioMood();
    }

    public static CardioMood getInstance() {
        return Holder.INSTANCE;
    }

    private CardioMood() {
    }

    public synchronized void logIn(String login, String password, String deviceId, ServerResponseCallback<AccessToken> callback) {
        Server.logIn(login, password, deviceId, new LoginCallback(login, password, callback));
    }

    public synchronized void logOut() {
        signedIn = false;
        password = null;
        login = null;
    }

    public static void register(String login, String password, ServerResponseCallback<Integer> callback) {
        Server.register(login, password, callback);
    }

    public void updateProfile(User user, ServerResponseCallbackRetry<Integer> callback) {
        Server.updateInfo(accessToken.getToken(), user, new HandleTokenExpiredCallback(callback));
    }

    public void getProfile(ServerResponseCallbackRetry<User> callback) {
        Server.getInfo(accessToken.getToken(), new HandleTokenExpiredCallback(callback));
    }

    public void getAllSessions(ServerResponseCallbackRetry<List<Session>> callback) {
        Server.getAllSessions(accessToken.getToken(), new HandleTokenExpiredCallback(callback));
    }

    public synchronized  AccessToken getAccessToken() {
        return accessToken;
    }

    public synchronized void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public synchronized String getLogin() {
        return login;
    }

    public synchronized void setLogin(String login) {
        this.login = login;
    }

    public synchronized String getPassword() {
        return password;
    }

    public synchronized void setPassword(String password) {
        this.password = password;
    }

    public synchronized String getDeviceId() {
        return deviceId;
    }

    public synchronized void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public synchronized boolean isSignedIn() {
        return signedIn;
    }

    private synchronized void setSignedIn(boolean signedIn) {
        this.signedIn = signedIn;
    }

    private class HandleTokenExpiredCallback implements ServerResponseCallback {

        private ServerResponseCallbackRetry externalCallback;

        public HandleTokenExpiredCallback(ServerResponseCallbackRetry externalCallback) {
            this.externalCallback = externalCallback;
        }

        @Override
        public void onResponse(ServerResponse response) {
            if (response.getResponseCode() == ServerResponse.OK) {
                if (externalCallback != null) {
                    externalCallback.onResponse(response);
                }
            } else if (response.getResponseCode() == ServerResponse.ServerError
                    && response.getServerError().getErrorCode() == ServerResponseError.BAD_TOKEN) {
                String login = getLogin();
                String password = getPassword();
                logOut();
                logIn(login, password, deviceId, new ServerResponseCallback<AccessToken>() {
                    @Override
                    public void onResponse(ServerResponse<AccessToken> response) {
                        if (response.getResponseCode() == ServerResponse.OK && externalCallback.isRetryRequired()) {
                            externalCallback.setRetryRequired(false);
                            externalCallback.retry();
                        } else {
                            callExternalCallback(new ServerResponse(ServerResponse.Error, new RuntimeException("Failed to renew the access token.")));
                        }
                    }
                });
            } else {
                callExternalCallback(response);
            }
        }

        private void callExternalCallback(ServerResponse response) {
            if (externalCallback != null) {
                externalCallback.onResponse(response);
            }
        }
    }

    private class LoginCallback implements ServerResponseCallback<AccessToken> {

        private ServerResponseCallback<AccessToken> externalCallback;
        private String login;
        private String password;

        public LoginCallback(String login, String password, ServerResponseCallback<AccessToken> externalCallback) {
            this.externalCallback = externalCallback;
        }

        @Override
        public void onResponse(ServerResponse<AccessToken> response) {
            switch (response.getResponseCode()) {
                case ServerResponse.OK: {
                    // login successful
                    setAccessToken(response.getResponse());
                    setLogin(login);
                    setPassword(password);
                    setSignedIn(true);
                    break;
                }
                case ServerResponse.Error: {
                    // local error
                    Exception ex = response.getError();
                    ex.printStackTrace();
                    break;
                }
                case ServerResponse.ServerError: {
                    // server error
                    ServerResponseError error = response.getServerError();
                    Log.d("CardioMood", "ServerError: errorCode=" + error.getErrorCode() + "; message = " + error.getMessage());
                    break;
                }
            }
            if (externalCallback != null) {
                externalCallback.onResponse(response);
            }
        }
    }
}
