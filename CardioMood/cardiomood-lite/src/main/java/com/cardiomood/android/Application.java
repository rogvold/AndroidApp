package com.cardiomood.android;

import android.support.multidex.MultiDexApplication;

import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.lite.R;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.parse.Parse;
import com.parse.ParseConfig;

import java.util.HashMap;

import bolts.Continuation;
import bolts.Task;
import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by Anton Danshin on 19/12/14.
 */
public class Application extends MultiDexApplication {

    private static final String PARSE_APP_ID = "SSzU4YxI6Z6SwvfNc2vkZhYQYl86CvBpd3P2wHF1";
    private static final String PARSE_CLIENT_KEY = "erjFId3717uDBia7ZOUObyaYudbIB8TYUXIW3Pnt";

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
    }

    private HashMap<TrackerName, Tracker> mTrackers = new HashMap<>();

    @Override @DebugLog
    public void onCreate() {
        super.onCreate();

        // initialize Timber for logging
        Timber.plant(new Timber.DebugTree());

        // configure Flurry
        FlurryAgent.setLogEnabled(false);
        FlurryAgent.init(this, ConfigurationConstants.FLURRY_API_KEY);

        // initialize Parse
        Parse.initialize(this.getApplicationContext(), PARSE_APP_ID, PARSE_CLIENT_KEY);

        // initialize DB
        DatabaseHelperFactory.initialize(this);

        ParseConfig.getInBackground()
                .continueWith(new Continuation<ParseConfig, Object>() {
                    @Override
                    public Object then(Task<ParseConfig> task) throws Exception {
                        ParseConfig config = null;
                        if (task.isFaulted()) {
                            Timber.w(task.getError(), "Failed to fetch latest ParseConfig. " +
                                    "Using the last cached one...");
                            config = ParseConfig.getCurrentConfig();
                        } else {
                            config = task.getResult();
                        }
                        if (config != null) {
                            onParseConfigLoaded(config);
                        }
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);

        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        analytics.setDryRun(false);
        analytics.setLocalDispatchPeriod(900);
        analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);

    }

    @Override @DebugLog
    public void onTerminate() {
        super.onTerminate();
        DatabaseHelperFactory.releaseHelper();
    }

    void onParseConfigLoaded(ParseConfig parseConfig) {
        PreferenceHelper prefHelper = new PreferenceHelper(this, true);
        prefHelper.putString(
                ConfigurationConstants.CONFIG_PUBNUB_PUB_KEY,
                parseConfig.getString("pubnub_pub_key", null)
        );
        prefHelper.putString(
                ConfigurationConstants.CONFIG_PUBNUB_SUB_KEY,
                parseConfig.getString("pubnub_sub_key", null)
        );
        prefHelper.putString(
                ConfigurationConstants.CONFIG_PUBNUB_CHANNEL,
                parseConfig.getString("pubnub_channel", null)
        );
        Timber.d("ParseConfig loaded.");
    }

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            // create new tracker
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker tracker = null;
            switch (trackerId) {
                case APP_TRACKER:
                    tracker = analytics.newTracker(R.xml.app_tracker);
                    break;
                case GLOBAL_TRACKER:
                default:
                    tracker = analytics.newTracker(R.xml.global_tracker);
            }
            tracker.enableAdvertisingIdCollection(true);
            // save this tracker to cache
            mTrackers.put(trackerId, tracker);
        }

        // return tracker from cache
        return mTrackers.get(trackerId);
    }
}