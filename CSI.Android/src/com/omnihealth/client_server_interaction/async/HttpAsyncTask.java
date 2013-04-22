package com.omnihealth.client_server_interaction.async;

import android.os.AsyncTask;
import android.util.Pair;
import com.omnihealth.client_server_interaction.HttpClient;
import com.omnihealth.client_server_interaction.ServerResponseCallback;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 30.03.13
 * Time: 23:57
 * To change this template use File | Settings | File Templates.
 */
public abstract class HttpAsyncTask<T> extends AsyncTask<String, Void, Pair<JSONObject, Exception>> {

    private ServerResponseCallback<T> mCallback;

    public HttpAsyncTask(ServerResponseCallback<T> callback) {
        mCallback = callback;
    }

    @Override
    protected final Pair<JSONObject, Exception> doInBackground(String... args) {
        JSONObject jObj = null;
        try {
            jObj = HttpClient.post(args[0], args[1], args[2]);
        } catch (Exception e) {
            return new Pair<JSONObject, Exception>(null, e);
        }
        return new Pair<JSONObject, Exception>(jObj, null);
    }

    protected abstract Pair<T, Exception> processResponse(Pair<JSONObject, Exception> response);

    @Override
    protected final void onPostExecute(Pair<JSONObject, Exception> response) {
        Pair<T, Exception> result = processResponse(response);
        mCallback.onResponse(result.first, result.second);
    }

}
