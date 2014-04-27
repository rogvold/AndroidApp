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

    @POST @Path("cardioSession/createCardioSession")
    JSONResponse<CardioSession> createSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serverId") Long serverId);

    @POST @Path("cardioSession/getCardioSessionsOfUser")
    JSONResponse<List<CardioSession>> getSessionsOfUser(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serverId") Long serverId);

    @POST @Path("cardioSession/updateCardioSessionInfo")
    JSONResponse<CardioSession> updateSessionInfo(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId, @FormParam("name") String name, @FormParam("description") String description);

    @POST @Path("cardioSession/getCardioSessionData")
    JSONResponse<CardioSessionWithData> getSessionData(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId);

    @POST @Path("cardioSession/deleteCardioSession")
    JSONResponse<String> deleteSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("sessionId") Long sessionId);

    @POST @Path("cardioSession/appendDataToCardioSession")
    JSONResponse<String> appendDataToSession(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serializedData") String serializedData);

    @POST @Path("cardioSession/rewriteCardioSessionData")
    JSONResponse<String> rewriteCardioSessionData(@FormParam("token") String token, @FormParam("userId") Long userId, @FormParam("serializedData") String serializedData);

}
