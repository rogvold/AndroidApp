package com.cardiomood.android.tools.analytics;

import android.app.Activity;
import android.content.Intent;

import com.cardiomood.android.Application;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.parse.ParseAnalytics;

import bolts.Continuation;
import bolts.Task;
import timber.log.Timber;

/**
 * Created by Anton Danshin on 21/01/15.
 */
public class AnalyticsHelper {

    private Tracker globalTracker;
    private Tracker appTracker;
    private boolean disableAnalytics;
    PreferenceHelper preferenceHelper;

    public AnalyticsHelper(Activity activity) {
        Application app = (Application) activity.getApplication();
        globalTracker = app.getTracker(Application.TrackerName.GLOBAL_TRACKER);
        appTracker = app.getTracker(Application.TrackerName.APP_TRACKER);

        preferenceHelper = new PreferenceHelper(app, true);
        disableAnalytics = preferenceHelper.getBoolean(ConfigurationConstants.CONFIG_DISABLE_ANALYTICS);
        GoogleAnalytics.getInstance(app).setAppOptOut(disableAnalytics);
    }

    public static Tracker getGoogleAnalyticsTracker(Activity activity, Application.TrackerName trackerName) {
        Application app = (Application) activity.getApplication();
        return app.getTracker(trackerName);
    }

    public void setUserId(String userId) {
        appTracker.set("&uid", userId);
        FlurryAgent.setUserId(userId);
    }

    public void logAppOpened(Intent intent) {
        logEvent("app_opened", "App opened");
        ParseAnalytics.trackAppOpenedInBackground(intent);
    }

    public void logEvent(String action, String label) {
        globalTracker.send(
                new HitBuilders.EventBuilder()
                        .setAction(action)
                        .setLabel(label)
                        .setCategory("unspecified")
                        .build()
        );
        appTracker.send(
                new HitBuilders.EventBuilder()
                        .setAction(action)
                        .setLabel(label)
                        .setCategory("unspecified")
                        .build()
        );
        if (! disableAnalytics
                || "app_opened".equals(action)
                || "user_login".equals(action)
                || "user_signup".equals(action)) {
            FlurryAgent.logEvent(action);
            ParseAnalytics.trackEventInBackground(action)
                    .continueWith(new Continuation<Void, Object>() {
                        @Override
                        public Object then(Task<Void> task) throws Exception {
                            if (task.isFaulted()) {
                                Timber.d(task.getError(), "Failed to log event via ParseAnalytics");
                            }
                            return null;
                        }
                    });
        }
    }

    public void logUserSignIn(String userId) {
        setUserId(userId);
        logEvent("user_login", "User logged in");
    }

    public void logUserSignUp(String userId) {
        setUserId(userId);
        logEvent("user_signup", "User signed up");
    }

    public void logActivityStart(Activity context) {
        disableAnalytics = preferenceHelper.getBoolean(ConfigurationConstants.CONFIG_DISABLE_ANALYTICS);

        // Google Analytics sessions are handled automatically
        // GoogleAnalytics.getInstance(context).reportActivityStart(context);
        if (disableAnalytics) {
            FlurryAgent.onStartSession(context);
            ParseAnalytics.trackEventInBackground("on_start/" + context.getLocalClassName());
        }
    }

    public void logActivityStop(Activity context) {
        // Google Analytics sessions are handled automatically
        // GoogleAnalytics.getInstance(context).reportActivityStop(context);
        if (disableAnalytics) {
            FlurryAgent.onEndSession(context);
            ParseAnalytics.trackEventInBackground("on_stop/" + context.getLocalClassName());
        }
    }

}
