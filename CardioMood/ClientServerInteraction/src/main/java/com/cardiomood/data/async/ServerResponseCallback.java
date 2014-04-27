package com.cardiomood.data.async;

import com.cardiomood.data.json.JSONError;

/**
 * Created by danon on 08.03.14.
 */
public interface ServerResponseCallback<T> {

    void onResult(T result);

    void onError(JSONError error);

}
