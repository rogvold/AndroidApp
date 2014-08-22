package com.cardiomood.android.air;

import android.app.Application;

import com.cardiomood.android.air.data.AirSession;
import com.cardiomood.android.air.data.Aircraft;
import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by danon on 13.08.2014.
 */
public class AirApplication extends Application {

    public static final String PARSE_APPLICATION_ID = "ZhlYHr1uAjC7CJB7l1QVfuzsJIwpp51J5KpQYOco";
    public static final String PARSE_CLIENT_KEY = "YecCXICy1VY4Z4S7cVOS5jtdB0dW2dRsKSINfK9B";

    @Override
    public void onCreate() {
        super.onCreate();

        // initialize Parse
        ParseObject.registerSubclass(Aircraft.class);
        ParseObject.registerSubclass(AirSession.class);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, PARSE_APPLICATION_ID, PARSE_CLIENT_KEY);
    }
}
