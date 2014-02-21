package com.cardiomood.android.services;

import org.codegist.crest.annotate.Consumes;
import org.codegist.crest.annotate.EndPoint;
import org.codegist.crest.annotate.FormParam;
import org.codegist.crest.annotate.POST;
import org.codegist.crest.annotate.Path;
import org.codegist.crest.annotate.QueryParam;

@EndPoint("{app.service_protocol}://{app.service_host}:{app.service_port}")
@Path("{app.service_path}")
@Consumes("application/json")
public interface ICardioMoodService {

	@POST
	@Path("auth/check_existence")
	CardioMoodSimpleResponse checkUserExistence(
            @QueryParam("secret") String secret,
            @QueryParam("email") String email);
	
	@POST
	@Path("auth/register")
	CardioMoodSimpleResponse registerUser(
            @QueryParam("secret") String secret,
            @QueryParam("email") String email,
            @QueryParam("password") String password);
	
	@POST
    @Path("auth/update_info")
	CardioMoodSimpleResponse updateInfo(
            @QueryParam("secret") String secret,
            @QueryParam("json") String json);
	
	@POST
    @Path("auth/info")
    User getUserInfo(
            @QueryParam("secret") String secret,
            @QueryParam("email") String email,
            @QueryParam("password") String password);
	
	@POST
    @Path("auth/check_data")
    CardioMoodSimpleResponse checkUserAuthorisationData(
            @QueryParam("secret") String secret,
            @QueryParam("email") String email,
            @QueryParam("password") String password);
	
	@POST
	@Path("rates/upload")
	CardioMoodSimpleResponse uploadRates(@FormParam("json") String json);
	
	@POST
	@Path("rates/sync")
	CardioMoodSimpleResponse syncRates(@FormParam("json") String json);

	
}
