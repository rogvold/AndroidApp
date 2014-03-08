package com.cardiomood.data.async;

import android.os.AsyncTask;
import android.util.Log;

import com.cardiomood.data.json.JsonError;
import com.cardiomood.data.json.JsonResponse;

public abstract class ServiceTask<T> extends AsyncTask<Object, Object, JsonResponse<T>> {

    private static final String TAG = ServiceTask.class.getSimpleName();

    private final ServerResponseCallback<T> callback;

    public ServiceTask(ServerResponseCallback<T> callback) {
        this.callback = callback;
    }

    protected ServiceTask(ServerResponseCallback<T> callback, Object... params) {
        this.callback = callback;
    }

    @Override
    protected void onPostExecute(JsonResponse<T> response) {
        if (callback == null) {
            Log.d(TAG, "onPostExecute(): callback = null, response = " + response);
            return;
        }
        if (response == null) {
            Log.d(TAG, "onPostExecute(): response is null");
            callback.onError(new JsonError("Network error: empty response", JsonError.EMPTY_RESPONSE_ERROR));
            return;
        }
        if (JsonResponse.RESPONSE_ERROR.equals(response.getResponseCode())) {
            Log.d(TAG, "onPostExecute(): response error code -> " + response.getResponseCode());
            callback.onError(response.getError());
            return;
        }
        if (JsonResponse.RESPONSE_OK.equals(response.getResponseCode())) {
            callback.onResult(response.getData());
            return;
        }
        Log.d(TAG, "onPostExecute(): invalid response code -> " + response.getResponseCode());
        callback.onError(new JsonError("Network error: Invalid response code " + response.getResponseCode(), JsonError.INVALID_RESPONSE_CODE_ERROR));
    }

    abstract protected JsonResponse<T> doInBackground(Object... params);
}