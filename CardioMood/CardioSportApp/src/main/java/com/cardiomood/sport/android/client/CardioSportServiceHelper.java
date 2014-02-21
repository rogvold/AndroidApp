package com.cardiomood.sport.android.client;

import android.os.AsyncTask;
import android.util.Log;

import com.cardiomood.sport.android.client.json.JsonError;
import com.cardiomood.sport.android.client.json.JsonResponse;
import com.cardiomood.sport.android.client.json.JsonTrainee;
import com.cardiomood.sport.android.client.json.JsonWorkout;
import com.cardiomood.sport.android.client.json.ResponseConstants;

/**
 * Project: CardioSport
 * User: danon
 * Date: 16.06.13
 * Time: 21:15
 */
public class CardioSportServiceHelper {

    private final ICardioSportService service;

    private String email;
    private String password;

    public CardioSportServiceHelper(ICardioSportService service) {
        this.service = service;
    }

    public ICardioSportService getService() {
        return service;
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

    public JsonResponse<JsonTrainee> checkUserAuthorisationData() {
        return service.checkUserAuthorisationData(getEmail(), getPassword());
    }

    public void checkUserAuthorisationData(Callback<JsonTrainee> callback) {
        new ServiceTask<JsonTrainee>(callback) {
            @Override
            protected JsonResponse<JsonTrainee> doInBackground(Object... params) {
                try {
                    Log.d("CardioSporService", "email = " + params[0] + "; password = " + params[1]);
                    return service.checkUserAuthorisationData((String) params[0], (String) params[1]);
                } catch (Exception ex) {
                    return new JsonResponse<JsonTrainee>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword());
    }

    public JsonResponse<JsonWorkout> getCurrentWorkout() {
        return service.getCurrentWorkout(getEmail(), getPassword());
    }

    public void getCurrentWorkout(Callback<JsonWorkout> callback) {
        new ServiceTask<JsonWorkout>(callback) {
            @Override
            protected JsonResponse<JsonWorkout> doInBackground(Object... params) {
                try {
                    return service.getCurrentWorkout((String) params[0], (String) params[1]);
                } catch (Exception ex) {
                    return new JsonResponse<JsonWorkout>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword());
    }

    public JsonResponse sendWorkoutData(long workoutId, String data) {
        return service.sendWorkoutData(getEmail(), getPassword(), workoutId, data);
    }

    public void sendWorkoutData(long workoutId, String data, Callback callback) {
        new ServiceTask(callback) {
            @Override
            protected JsonResponse doInBackground(Object... params) {
                return service.sendWorkoutData((String) params[0], (String) params[1], (Long) params[2], (String) params[3]);
            }
        }.execute(getEmail(), getPassword(), workoutId, data);
    }

    public JsonResponse<Long> startWorkout(long workoutId) {
        return service.startWorkout(getEmail(), getPassword(), workoutId);
    }

    public void startWorkout(long workoutId, Callback<Long> callback) {
        new ServiceTask<Long>(callback) {
            @Override
            protected JsonResponse<Long> doInBackground(Object... params) {
                try {
                    return service.startWorkout((String) params[0], (String) params[1], (Long) params[2]);
                } catch (Exception ex) {
                    return new JsonResponse<Long>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword(), workoutId);
    }

    public JsonResponse<Long> startActivity(long workoutId, long activityId) {
        return service.startActivity(getEmail(), getPassword(), workoutId, activityId);
    }

    public void startActivity(long workoutId, long activityId, Callback<Long> callback) {
        new ServiceTask<Long>(callback) {
            @Override
            protected JsonResponse<Long> doInBackground(Object... params) {
                try {
                    return service.startActivity((String) params[0], (String) params[1], (Long) params[2], (Long) params[3]);
                } catch (Exception ex) {
                    return new JsonResponse<Long>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword(), workoutId, activityId);
    }

    public JsonResponse stopActivity(long workoutId, long activityId, long duration) {
        return service.stopActivity(getEmail(), getPassword(), workoutId, activityId, duration);
    }

    public void stopActivity(long workoutId, long activityId, long duration, Callback callback) {
        new ServiceTask(callback) {
            @Override
            protected JsonResponse doInBackground(Object... params) {
                try {
                    return service.stopActivity((String) params[0], (String) params[1], (Long) params[2], (Long) params[3], (Long) params[4]);
                } catch (Exception ex) {
                    return new JsonResponse<Long>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword(), workoutId, activityId, duration);
    }

    public JsonResponse stopWorkout(long workoutId) {
        return service.stopWorkout(getEmail(), getPassword(), workoutId);
    }

    public void stopWorkout(long workoutId, Callback callback) {
        new ServiceTask(callback) {
            @Override
            protected JsonResponse doInBackground(Object... params) {
                try {
                    return service.stopWorkout((String) params[0], (String) params[1], (Long) params[2]);
                } catch (Exception ex) {
                    return new JsonResponse<Long>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword(), workoutId);
    }

    public JsonResponse<Long> pauseActivity(long workoutId, long activityId) {
        return service.pauseActivity(getEmail(), getPassword(), workoutId, activityId);
    }

    public void pauseActivity(long workoutId, long activityId, Callback<Long> callback) {
        new ServiceTask<Long>(callback) {
            @Override
            protected JsonResponse<Long> doInBackground(Object... params) {
                try {
                    return service.pauseActivity((String) params[0], (String) params[1], (Long) params[2], (Long) params[3]);
                } catch (Exception ex) {
                    return new JsonResponse<Long>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword(), workoutId, activityId);
    }

    public JsonResponse resumeActivity(long workoutId, long duration) {
        return service.resumeActivity(getEmail(), getPassword(), workoutId, duration);
    }

    public void resumeActivity(long workoutId, long duration, Callback callback) {
        new ServiceTask(callback) {
            @Override
            protected JsonResponse doInBackground(Object... params) {
                try {
                    return service.resumeActivity((String) params[0], (String) params[1], (Long) params[2], (Long) params[3]);
                } catch (Exception ex) {
                    return new JsonResponse<Long>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword(), workoutId, duration);
    }

    public JsonResponse<Long> switchActivity(long workoutId, Long firstActivityId, Long secondActivityId) {
        return service.switchActivity(getEmail(), getPassword(), workoutId, firstActivityId, secondActivityId);
    }

    public void switchActivity(long workoutId, Long firstActivityId, Long secondActivityId, Callback<Long> callback) {
        new ServiceTask<Long>(callback) {
            @Override
            protected JsonResponse<Long> doInBackground(Object... params) {
                try {
                    return service.switchActivity((String) params[0], (String) params[1], (Long) params[2], (Long) params[3], (Long) params[4]);
                } catch (Exception ex) {
                    return new JsonResponse<Long>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword(), workoutId, firstActivityId, secondActivityId);
    }

    public JsonResponse<Double> getMetronomeRate() {
        return service.getMetronomeRate(getEmail(), getPassword());
    }

    public void getMetronomeRate(Callback<Double> callback) {
        new ServiceTask<Double>(callback) {
            @Override
            protected JsonResponse<Double> doInBackground(Object... params) {
                try {
                    return service.getMetronomeRate((String) params[0], (String) params[1]);
                } catch (Exception ex) {
                    return new JsonResponse<Double>(ResponseConstants.ERROR, new JsonError("Service error: " + ex.getLocalizedMessage(), ResponseConstants.SERVICE_ERROR_CODE), null);
                }
            }
        }.execute(getEmail(), getPassword());
    }


    public static interface Callback<T> {
        void onResult(T result);
        void onError(JsonError error);
    }

    private abstract class ServiceTask<T> extends AsyncTask<Object, Object, JsonResponse<T>> {

        private final Callback<T> callback;

        public ServiceTask(Callback callback) {
            this.callback = callback;
        }

        protected ServiceTask(Callback callback, Object... params) {
            this.callback = callback;
        }

        abstract protected JsonResponse<T> doInBackground(Object... params);

        @Override
        protected void onPostExecute(JsonResponse<T> response) {
            if (callback == null) {
                Log.d("CardioSport", "ServiceTask.onPostExecute(): callback = null, response = " + response);
                return;
            }
            if (response == null) {
                Log.d("CardioSport", "ServiceTask.onPostExecute(): response is null");
                callback.onError(new JsonError("Network error: empty response", ResponseConstants.ERROR_EMPTY_RESPONSE));
                return;
            }
            if (ResponseConstants.ERROR.equals(response.getResponseCode())) {
                Log.d("CardioSport", "ServiceTask.onPostExecute(): response error code " + response.getResponseCode());
                callback.onError(response.getError());
                return;
            }
            if (ResponseConstants.OK.equals(response.getResponseCode())) {
                callback.onResult(response.getData());
                return;
            }
            callback.onError(new JsonError("Network error: Invalid response code " + response.getResponseCode(), ResponseConstants.ERROR_INVALID_RESPONSE_CODE));
        }
    }
}
