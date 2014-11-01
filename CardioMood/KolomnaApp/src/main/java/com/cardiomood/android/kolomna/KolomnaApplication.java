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
    private static final String PARSE_APP_ID = "KNYnAGgkTVXhSXGzccX33w7ayISaEZBTYd01Qr8X";
    private static final String PARSE_CLIENT_KEY = "OyULWCmLxnor6DPJjEWJrHU1p2F5jsVSVozsdo4f";

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
