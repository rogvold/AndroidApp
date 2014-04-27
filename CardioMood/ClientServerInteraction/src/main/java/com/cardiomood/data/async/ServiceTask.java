package com.cardiomood.data.async;

import android.os.AsyncTask;
import android.util.Log;

import com.cardiomood.data.json.JSONError;
import com.cardiomood.data.json.JSONResponse;

public abstract class ServiceTask<T> extends AsyncTask<Object, Object, JSONResponse<T>> {

    private static final String TAG = ServiceTask.class.getSimpleName();

    private final ServerResponseCallback<T> callback;

    public ServiceTask(ServerResponseCallback<T> callback) {
        this.callback = callback;
    }

    protected ServiceTask(ServerResponseCallback<T> callback, Object... params) {
        this.callback = callback;
    }

    @Override
    protected void onPostExecute(JSONResponse<T> response) {
        if (callback == null) {
            Log.d(TAG, "onPostExecute(): callback = null, response = " + response);
            return;
        }
        if (response == null) {
            Log.d(TAG, "onPostExecute(): response is null");
            callback.onError(new JSONError("Network error: empty response", JSONError.EMPTY_RESPONSE_ERROR));
            return;
        }
        if (JSONResponse.RESPONSE_ERROR.equals(response.getResponseCode())) {
            Log.d(TAG, "onPostExecute(): response error code -> " + response.getResponseCode());
            callback.onError(response.getError());
            return;
        }
        if (JSONResponse.RESPONSE_OK.equals(response.getResponseCode())) {
            callback.onResult(response.getData());
            return;
        }
        Log.d(TAG, "onPostExecute(): invalid response code -> " + response.getResponseCode());
        callback.onError(new JSONError("Network error: Invalid response code " + response.getResponseCode(), JSONError.INVALID_RESPONSE_CODE_ERROR));
    }

    abstract protected JSONResponse<T> doInBackground(Object... params);
}