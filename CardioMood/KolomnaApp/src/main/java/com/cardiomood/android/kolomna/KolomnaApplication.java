package com.cardiomood.android.kolomna;

import android.app.Application;

import com.cardiomood.android.kolomna.db.HelperFactory;
import com.cardiomood.android.kolomna.parse.CardioSession;
import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by antondanhsin on 19/10/14.
 */
public class KolomnaApplication extends Application {

    // TODO: change parse.com keys!
    private static final String PARSE_APP_ID = "8BiAfjRaj4S9AvHHKKXWOHX40PnEkDdgBEZlp4VY";
    private static final String PARSE_CLIENT_KEY = "Dmqfv4QW3LcBOLfMhxUzkR2a9NQokjVz5dx0O2fv";

    @Override
    public void onCreate() {
        super.onCreate();

        // initialize Parse
        ParseObject.registerSubclass(CardioSession.class);
        Parse.initialize(this, PARSE_APP_ID, PARSE_CLIENT_KEY);

        // init DB helper
        HelperFactory.setHelper(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        HelperFactory.releaseHelper();
    }
}
