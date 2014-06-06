package com.cardiomood.data;

import com.cardiomood.data.json.ApiToken;
import com.cardiomood.data.json.CardioSession;
import com.cardiomood.data.json.CardioSessionWithData;
import com.cardiomood.data.json.JSONResponse;
import com.cardiomood.data.json.UserProfile;

import org.codegist.crest.annotate.Consumes;
import org.codegist.crest.annotate.EndPoint;
import org.codegist.crest.annotate.FormParam;
import org.codegist.crest.annotate.POST;
import org.codegist.crest.annotate.Path;
import org.codegist.crest.annotate.Produces;

import java.util.List;

/**
 * Created by danon on 08.03.14.
 */

@EndPoint("{app.service_protocol}://{app.service_host}:{app.service_port}")
@Path("{app.service_path}")
@Consumes("application/json")
@Produces("application/json")
public interface CardioMoodDataService {

    @POST @Path("auth/registerUserByEmailAndPassword")
    JSONResponse<UserProfile> register(@FormParam("email") String email, @FormParam("password") String password);

    @POST @Path("auth/loginByEmailAndPassword")
    JSONResponse<ApiToken> login(@FormParam("email") String email, @FormParam("password") String password);

    @POST @Path("v2/CardioMoodSession/createCardioMoodSession")
    JSONResponse<CardioSession> createSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serverId") Long serverId,  @FormParam("className") String className);

    @POST @Path("v2/CardioMoodSession/getCardioMoodSessionsOfUser")
    JSONResponse<List<CardioSession>> getSessionsOfUser(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serverId") Long serverId);

    @POST @Path("v2/CardioMoodSession/updateCardioMoodSessionInfo")
    JSONResponse<CardioSession> updateSessionInfo(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId, @FormParam("name") String name, @FormParam("description") String description);

    @POST @Path("v2/CardioMoodSession/getCardioMoodSessionData")
    JSONResponse<CardioSessionWithData> getSessionData(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId);

    @POST @Path("v2/CardioMoodSession/deleteCardioMoodSession")
    JSONResponse<String> deleteSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId);

    @POST @Path("v2/CardioMoodSession/appendDataToCardioMoodSession")
    JSONResponse<String> appendDataToSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serializedData") String serializedData);

    @POST @Path("v2/CardioMoodSession/rewriteCardioMoodSessionData")
    JSONResponse<String> rewriteCardioSessionData(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serializedData") String serializedData);

    @POST @Path("v2/CardioMoodSession/finishCardioMoodSession")
    JSONResponse<String> finishSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId, @FormParam("endTimestamp") Long endTimestamp);

    @POST @Path("auth/updateUserInfo")
    JSONResponse<String> updateUserInfo(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("firstName") String firstName, @FormParam("lastName") String lastName);

    @POST @Path("auth/updateUserProfile")
    JSONResponse<String> updateUserProfile(@FormParam("token") String token, @FormParam("serializedUser") String serializedUser);
}
