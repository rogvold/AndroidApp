package com.cardiomood.sport.android.client;

import com.cardiomood.sport.android.client.json.JsonActivity;
import com.cardiomood.sport.android.client.json.JsonError;
import com.cardiomood.sport.android.client.json.JsonResponse;
import com.cardiomood.sport.android.client.json.JsonTrainee;
import com.cardiomood.sport.android.client.json.JsonWorkout;
import com.cardiomood.sport.android.client.json.ResponseConstants;

import org.codegist.crest.annotate.FormParam;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Project: CardioSport
 * User: danon
 * Date: 14.06.13
 * Time: 22:10
 */
public class ServiceStub implements ICardioSportService, ResponseConstants {

    private static long workoutId = 1000L + new Random().nextInt(1000);

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException ex) {
            // suppress this exception
        }
    }

    @Override
    public JsonResponse<JsonTrainee> checkUserAuthorisationData(String email, String password) {
        sleep(2000L);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password)) {
            JsonTrainee trainee = new JsonTrainee();
            trainee.setEmail(email);
            trainee.setFirstName("Danon");
            trainee.setLastName("Oren");
            trainee.setCurrentWorkoutId(workoutId);
            trainee.setPhone("+79153532027");
            trainee.setId(13L);
            return new JsonResponse(OK, null, trainee);
        } else {
            return new JsonResponse(OK, null, null);
        }
    }

    @Override
    public JsonResponse<JsonWorkout> getCurrentWorkout(String email, String password) {
        sleep(5000);

        JsonWorkout w = new JsonWorkout();
        w.setStartDate(System.currentTimeMillis() + 20 * 60 * 1000);
        w.setDescription("Did you hear the one about the cannibal who passed his brother in the jungle the other day?");
        w.setId(workoutId++);
        w.setName("Test Workout");

        long r = new Random().nextInt(1000000);

        List<JsonActivity> activities = new LinkedList<JsonActivity>();
        JsonActivity a = new JsonActivity();
        a.setId(10L + r);
        a.setName("Warm up");
        a.setDescription("This is just a warm up. Nothing serious.");
        a.setDuration(1000 * 60 * 1L);
        a.setMinHeartRate(100);
        a.setMaxHeartRate(140);
        a.setOrderNumber(1);
        activities.add(a);

        a = new JsonActivity();
        a.setId(11L + r);
        a.setName("Cardio");
        a.setDuration(1000 * 60 * 2L);
        a.setMinHeartRate(150);
        a.setMaxHeartRate(170);
        a.setDescription("Train your cardiovascular system.");
        a.setOrderNumber(2);
        activities.add(a);

        a = new JsonActivity();
        a.setId(12L + r);
        a.setName("Cool down");
        a.setDuration(1000 * 60 * 1L);
        a.setMinHeartRate(150);
        a.setMaxHeartRate(170);
        a.setDescription("You should cool down at the end.");
        a.setOrderNumber(3);
        activities.add(a);

        w.setActivities(activities);

        return new JsonResponse<JsonWorkout>(OK, null, w);
    }

    @Override
    public JsonResponse sendWorkoutData(String email, String password, long workoutId, String data) {
        sleep(2000);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password) && workoutId > 1000L) {
            return new JsonResponse(OK, null, null);
        } else {
            return new JsonResponse(ERROR, new JsonError("Incorrect login/password.", NORMAL_ERROR_CODE), null);
        }
    }

    @Override
    public JsonResponse<Long> startWorkout(String email, String password, long workoutId) {
        sleep(1000);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password) && workoutId > 1000L) {
            return new JsonResponse(OK, null, workoutId + 10000L);
        } else {
            return new JsonResponse(ERROR, new JsonError("Incorrect login/password.", NORMAL_ERROR_CODE), null);
        }
    }

    @Override
    public JsonResponse<Long> startActivity(String email, String password, long workoutId, long activityId) {
        sleep(2000L);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password) && workoutId > 1000L) {
            return new JsonResponse(OK, null, activityId + 10000L);
        } else {
            return new JsonResponse(ERROR, new JsonError("Incorrect login/password or workoutId.", NORMAL_ERROR_CODE), null);
        }
    }

    @Override
    public JsonResponse stopActivity(String email, String password, long workoutId, long activityId, long duration) {
        sleep(1000L);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password) && workoutId > 1000L && activityId >= 10L && duration > 0) {
            return new JsonResponse(OK, null, null);
        } else {
            return new JsonResponse(ERROR, new JsonError("Incorrect login/password or workoutId/activityId.", NORMAL_ERROR_CODE), null);
        }
    }

    @Override
    public JsonResponse stopWorkout(String email, String password, long workoutId) {
        sleep(500L);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password) && workoutId > 1000L) {
            return new JsonResponse(OK, null, null);
        } else {
            return new JsonResponse(ERROR, new JsonError("Incorrect login/password or workoutId.", NORMAL_ERROR_CODE), null);
        }
    }

    @Override
    public JsonResponse<Long> pauseActivity(String email, String password, long workoutId, long activityId) {
        sleep(2000L);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password) && workoutId > 1000L) {
            return new JsonResponse(OK, null, -activityId);
        } else {
            return new JsonResponse(ERROR, new JsonError("Incorrect login/password or workoutId/activityId.", NORMAL_ERROR_CODE), null);
        }
    }

    @Override
    public JsonResponse resumeActivity(String email, String password, long workoutId, long duration) {
        sleep(1000L);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password) && workoutId > 1000L) {
            return new JsonResponse(OK, null, null);
        } else {
            return new JsonResponse(ERROR, new JsonError("Incorrect login/password or workoutId/activityId.", NORMAL_ERROR_CODE), null);
        }
    }

    @Override
    public JsonResponse<Long> switchActivity(String email, String password, long workoutId, Long firstActivityId, Long secondActivityId) {
        sleep(1500L);
        if ("test@test.ru".equalsIgnoreCase(email) && "test".equals(password) && workoutId > 1000L) {
            if (secondActivityId != null)
                return new JsonResponse(OK, null, secondActivityId + 10000L);
            if (firstActivityId != null)
                return new JsonResponse(OK, null, null);
            return new JsonResponse(ERROR, new JsonError("Cannot switch activities: null -> null.", NORMAL_ERROR_CODE), null);
        } else {
            return new JsonResponse(ERROR, new JsonError("Incorrect login/password or workoutId/activityId.", NORMAL_ERROR_CODE), null);
        }
    }

    @Override
    public JsonResponse<Double> getMetronomeRate(@FormParam("email") String email, @FormParam("password") String password) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
