package com.cardiomood.data.test;

import com.cardiomood.data.CardioMoodDataService;
import com.cardiomood.data.json.ApiToken;
import com.cardiomood.data.json.CardioSession;
import com.cardiomood.data.json.CardioSessionWithData;
import com.cardiomood.data.json.JSONError;
import com.cardiomood.data.json.JSONResponse;
import com.cardiomood.data.json.UserProfile;

import org.codegist.crest.annotate.FormParam;

import java.util.List;
import java.util.Random;

/**
 * Created by danon on 08.03.14.
 */
public class ServiceStub implements CardioMoodDataService {

    private final String EMAIL = "test@cardiomood.com";
    private final String PASSWORD = "test";
    private final long USER_ID = 777L;
    private final long TOKEN_LIFE_TIME = 5*1000*60; // 5 min

    private final Random random = new Random(System.currentTimeMillis());

    @Override
    public JSONResponse<UserProfile> register(@FormParam("email") String email, @FormParam("password") String password) {
        sleep(500L);
        if ((email == null || email.isEmpty())) {
            return new JSONResponse<UserProfile>(new JSONError("Email is empty", JSONError.REGISTRATION_FAILED_ERROR));
        }
        if ((password == null || password.isEmpty())) {
            return new JSONResponse<UserProfile>(new JSONError("Password is empty", JSONError.REGISTRATION_FAILED_ERROR));
        }
        if (EMAIL.equalsIgnoreCase(email) && PASSWORD.equals(password))
            return new JSONResponse<UserProfile>(getUser());
        else
            return new JSONResponse<UserProfile>(new JSONError(email+" already exists in the system.", JSONError.REGISTRATION_FAILED_ERROR));
    }

    @Override
    public JSONResponse<ApiToken> login(@FormParam("email") String email, @FormParam("password") String password) {
        sleep(300L);
        if ((email == null || email.isEmpty())) {
            return new JSONResponse<ApiToken>(new JSONError("Email is empty", JSONError.LOGIN_FAILED_ERROR));
        }
        if ((password == null || password.isEmpty())) {
            return new JSONResponse<ApiToken>(new JSONError("Password is empty", JSONError.LOGIN_FAILED_ERROR));
        }
        if (EMAIL.equalsIgnoreCase(email) && PASSWORD.equals(password))
            return new JSONResponse<ApiToken>(generateApiToken());
        else
            return new JSONResponse<ApiToken>(new JSONError("Incorrect login and/or password.", JSONError.LOGIN_FAILED_ERROR));
    }

    @Override
    public JSONResponse<CardioSession> createSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serverId") Long serverId, @FormParam("className") String className) {
        return null;
    }

    @Override
    public JSONResponse<List<CardioSession>> getSessionsOfUser(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serverId") Long serverId) {
        return null;
    }

    @Override
    public JSONResponse<CardioSession> updateSessionInfo(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId, @FormParam("name") String name, @FormParam("description") String description) {
        return null;
    }

    @Override
    public JSONResponse<CardioSessionWithData> getSessionData(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId) {
        return null;
    }

    @Override
    public JSONResponse<String> deleteSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId) {
        return null;
    }

    @Override
    public JSONResponse<String> appendDataToSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serializedData") String serializedData) {
        return null;
    }

    @Override
    public JSONResponse<String> rewriteCardioSessionData(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serializedData") String serializedData) {
        return null;
    }

    @Override
    public JSONResponse<String> finishSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId, @FormParam("endTimestamp") Long endTimestamp) {
        return null;
    }

    @Override
    public JSONResponse<String> updateUserInfo(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("firstName") String firstName, @FormParam("lastName") String lastName) {
        return null;
    }

    @Override
    public JSONResponse<String> updateUserProfile(@FormParam("token") String token, @FormParam("serializedUser") String serializedUser) {
        return null;
    }

    @Override
    public JSONResponse<UserProfile> getUserProfileByToken(@FormParam("token") String token) {
        return null;
    }

    private ApiToken generateApiToken() {
        final ApiToken token = new ApiToken(USER_ID, "this_is_a_test_token_"+random.nextInt(), System.currentTimeMillis()+TOKEN_LIFE_TIME);
        token.setId(random.nextLong());
        return token;
    }

    private UserProfile getUser() {
        UserProfile user = new UserProfile();
        user.setId(USER_ID);
        user.setAccountStatus(UserProfile.AccountStatus.FREE);
        user.setUserStatus(UserProfile.Status.ACTIVE);
        user.setUserRole(UserProfile.Role.USER);
        return user;
    }

    private void sleep(long duration) {
        try {
            Thread.currentThread().sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
