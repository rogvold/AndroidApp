package com.omnihealth.client_server_interaction.async;

import android.util.Pair;
import com.omnihealth.client_server_interaction.Serializer;
import com.omnihealth.client_server_interaction.ServerResponseCallback;
import com.omnihealth.client_server_interaction.ServerResponseError;
import com.omnihealth.client_server_interaction.User;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 31.03.13
 * Time: 0:30
 * To change this template use File | Settings | File Templates.
 */
public class DefaultUserHttpAsyncTask extends HttpAsyncTask<User> {

    public DefaultUserHttpAsyncTask(ServerResponseCallback<User> callback) {
        super(callback);
    }

    @Override
    protected Pair<User, Exception> processResponse(Pair<JSONObject, Exception> response) {
        JSONObject jResponse = response.first;
        if (jResponse == null)
            return new Pair<User, Exception>(null, response.second);
        String error = jResponse.optString("error");
        if (error != null && !error.isEmpty())
            return new Pair<User, Exception>(null, new ServerResponseError(error));
        return new Pair<User, Exception>(Serializer.deserializeUser(jResponse), null);
    }
}
