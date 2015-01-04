package com.cardiomood.android;

import android.support.multidex.MultiDexApplication;

import com.cardiomood.android.db.DatabaseHelperFactory;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.android.tools.config.ConfigurationConstants;
import com.parse.Parse;
import com.parse.ParseConfig;

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

    @Override @DebugLog
    public void onCreate() {
        super.onCreate();

        // initialize Timber for logging
        Timber.plant(new Timber.DebugTree());

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
}
