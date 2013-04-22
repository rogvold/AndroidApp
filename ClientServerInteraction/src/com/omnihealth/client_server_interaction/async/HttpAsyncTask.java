package com.omnihealth.client_server_interaction.async;

import android.os.AsyncTask;
import android.util.Pair;
import com.omnihealth.client_server_interaction.*;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 30.03.13
 * Time: 23:57
 * To change this template use File | Settings | File Templates.
 */
public class HttpAsyncTask<T> extends AsyncTask<String, Void, ServerResponse<T>> {

    private ServerResponseCallback<T> mCallback;
    private int classId;

    public HttpAsyncTask(ServerResponseCallback<T> callback, int classId) {
        mCallback = callback;
        this.classId = classId;
    }

    @Override
    protected final ServerResponse<T> doInBackground(String... args) {
        JSONObject jObj;
        try {
            jObj = HttpClient.post(args[0], args[1]);
        } catch (Exception e) {
            return new ServerResponse<T>(ServerResponse.Error, e);
        }

        T result;
        try {
            result = (T)Serializer.deserialize(jObj.toString(), classId);
            return new ServerResponse<T>(ServerResponse.OK, result);
        } catch (ServerResponseError sre) {
            return new ServerResponse<T>(ServerResponse.ServerError, sre);
        }  catch (JSONException e) {
            return new ServerResponse<T>(ServerResponse.Error, e);
        }
    }

    @Override
    protected final void onPostExecute(ServerResponse response) {
        mCallback.onResponse(response);
    }

}
