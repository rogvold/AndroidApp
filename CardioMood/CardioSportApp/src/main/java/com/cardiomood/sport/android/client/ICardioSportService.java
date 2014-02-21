package com.cardiomood.sport.android.client;

import com.cardiomood.sport.android.client.json.JsonResponse;
import com.cardiomood.sport.android.client.json.JsonTrainee;
import com.cardiomood.sport.android.client.json.JsonWorkout;

import org.codegist.crest.annotate.Consumes;
import org.codegist.crest.annotate.EndPoint;
import org.codegist.crest.annotate.FormParam;
import org.codegist.crest.annotate.POST;
import org.codegist.crest.annotate.Path;

/**
 * Project: CardioSport
 * User: danon
 * Date: 10.06.13
 * Time: 1:24
 */

@EndPoint("{app.service_protocol}://{app.service_host}:{app.service_port}")
@Path("{app.service_path}")
@Consumes("application/json")
public interface ICardioSportService {

    @POST
    @Path("auth/check_data")
    JsonResponse<JsonTrainee> checkUserAuthorisationData(@FormParam("email") String email, @FormParam("password") String password);

    @POST
    @Path("workout/get_current")
    JsonResponse<JsonWorkout> getCurrentWorkout(@FormParam("email") String email, @FormParam("password") String password);

    @POST
    @Path("workout/send_data")
    JsonResponse sendWorkoutData(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("workoutId") long workoutId,
            @FormParam("data") String data
    );

    @POST
    @Path("workout/start_workout")
    JsonResponse<Long> startWorkout(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("workoutId") long workoutId
    );

    @POST
    @Path("workout/start_activity")
    JsonResponse<Long> startActivity(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("workoutId") long workoutId,
            @FormParam("activityId") long activityId
    );

    @POST
    @Path("workout/stop_activity")
    JsonResponse stopActivity(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("workoutId") long workoutId,
            @FormParam("activityId") long activityId,
            @FormParam("duration") long duration
    );

    @POST
    @Path("workout/stop_workout")
    JsonResponse stopWorkout(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("workoutId") long workoutId
    );

    @POST
    @Path("workout/pause_activity")
    JsonResponse<Long> pauseActivity(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("workoutId") long workoutId,
            @FormParam("activityId") long activityId
    );

    @POST
    @Path("workout/resume_activity")
    JsonResponse resumeActivity(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("workoutId") long workoutId,
            @FormParam("duration") long duration
    );

    @POST
    @Path("workout/switch_activity")
    JsonResponse<Long> switchActivity(
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("workoutId") long workoutId,
            @FormParam("firstActivityId") Long firstActivityId,
            @FormParam("secondActivityId") Long secondActivityId
    );

    @POST
    @Path("workout/get_metronome_rate")
    public JsonResponse<Double> getMetronomeRate(
            @FormParam("email") String email,
            @FormParam("password") String password
    );

}
