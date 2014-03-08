package com.cardiomood.data.test;

import com.cardiomood.data.CardioMoodDataService;
import com.cardiomood.data.json.AccountStatusEnum;
import com.cardiomood.data.json.ApiToken;
import com.cardiomood.data.json.CardioSession;
import com.cardiomood.data.json.CardioSessionWithData;
import com.cardiomood.data.json.JsonError;
import com.cardiomood.data.json.JsonResponse;
import com.cardiomood.data.json.UserProfile;
import com.cardiomood.data.json.UserRoleEnum;
import com.cardiomood.data.json.UserStatusEnum;

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
    public JsonResponse<UserProfile> register(@FormParam("email") String email, @FormParam("password") String password) {
        sleep(500L);
        if ((email == null || email.isEmpty())) {
            return new JsonResponse<UserProfile>(new JsonError("Email is empty", JsonError.REGISTRATION_FAILED_ERROR));
        }
        if ((password == null || password.isEmpty())) {
            return new JsonResponse<UserProfile>(new JsonError("Password is empty", JsonError.REGISTRATION_FAILED_ERROR));
        }
        if (EMAIL.equalsIgnoreCase(email) && PASSWORD.equals(password))
            return new JsonResponse<UserProfile>(getUser());
        else
            return new JsonResponse<UserProfile>(new JsonError(email+" already exists in the system.", JsonError.REGISTRATION_FAILED_ERROR));
    }

    @Override
    public JsonResponse<ApiToken> login(@FormParam("email") String email, @FormParam("password") String password) {
        sleep(300L);
        if ((email == null || email.isEmpty())) {
            return new JsonResponse<ApiToken>(new JsonError("Email is empty", JsonError.LOGIN_FAILED_ERROR));
        }
        if ((password == null || password.isEmpty())) {
            return new JsonResponse<ApiToken>(new JsonError("Password is empty", JsonError.LOGIN_FAILED_ERROR));
        }
        if (EMAIL.equalsIgnoreCase(email) && PASSWORD.equals(password))
            return new JsonResponse<ApiToken>(generateApiToken());
        else
            return new JsonResponse<ApiToken>(new JsonError("Incorrect login and/or password.", JsonError.LOGIN_FAILED_ERROR));
    }

    @Override
    public JsonResponse<CardioSession> createSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serverId") Long serverId) {
        return null;
    }

    @Override
    public JsonResponse<List<CardioSession>> getSessionsOfUser(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serverId") Long serverId) {
        return null;
    }

    @Override
    public JsonResponse<CardioSession> updateSessionInfo(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId, @FormParam("name") String name, @FormParam("description") String description) {
        return null;
    }

    @Override
    public JsonResponse<CardioSessionWithData> getSessionData(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId) {
        return null;
    }

    @Override
    public JsonResponse<String> deleteSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId) {
        return null;
    }

    @Override
    public JsonResponse<String> appendDataToSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serializedData") String serializedData) {
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
        user.setAccountStatus(AccountStatusEnum.FREE);
        user.setUserStatus(UserStatusEnum.ACTIVE);
        user.setUserRole(UserRoleEnum.USER);
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
