package com.omnihealth.client_server_interaction.async;

import android.util.Pair;
import com.omnihealth.client_server_interaction.ServerResponseCallback;
import com.omnihealth.client_server_interaction.ServerResponseError;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 31.03.13
 * Time: 0:14
 * To change this template use File | Settings | File Templates.
 */
public class DefaultBooleanHttpAsyncTask extends HttpAsyncTask<Boolean> {

    public DefaultBooleanHttpAsyncTask(ServerResponseCallback<Boolean> callback) {
        super(callback);
    }

    @Override
    protected Pair<Boolean, Exception> processResponse(Pair<JSONObject, Exception> response) {
        JSONObject jResponse = response.first;
        if (jResponse == null)
            return new Pair<Boolean, Exception>(null, response.second);
        String error = jResponse.optString("error");
        if (error != null && !error.isEmpty())
            return new Pair<Boolean, Exception>(null, new ServerResponseError(error));
        return new Pair<Boolean, Exception>(Integer.decode(jResponse.optString("response")) == 1, null);
    }
}
