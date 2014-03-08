package com.cardiomood.data;

import android.content.Context;

import com.cardiomood.android.tools.ConfigurationManager;
import com.cardiomood.android.tools.PreferenceHelper;

import org.codegist.crest.CRestBuilder;

/**
 * Created by danon on 08.03.14.
 */
public enum  CardioMoodServer implements ServerConstants {

    INSTANCE;

    private CardioMoodDataService service = null;

    public CardioMoodDataService rebuildService(Context ctx) {
        synchronized (INSTANCE) {
            ConfigurationManager config = ConfigurationManager.getInstance();
            PreferenceHelper pref = new PreferenceHelper(ctx);
            pref.setPersistent(true);
            config.setString(SERVICE_PROTOCOL, pref.getString(SERVICE_PROTOCOL, DEFAULT_SERVICE_PROTOCOL));
            config.setString(SERVICE_HOST, pref.getString(SERVICE_HOST, DEFAULT_SERVICE_HOST));
            config.setString(SERVICE_PORT, pref.getString(SERVICE_PORT, DEFAULT_SERVICE_PORT));
            config.setString(SERVICE_PATH, pref.getString(SERVICE_PATH, DEFAULT_SERVICE_PATH));
            return INSTANCE.service = buildService();
        }
    }

    private CardioMoodDataService buildService() {
            ConfigurationManager conf = ConfigurationManager.getInstance();
            CRestBuilder builder = new CRestBuilder();
            builder.placeholder(SERVICE_PROTOCOL, conf.getString(SERVICE_PROTOCOL, DEFAULT_SERVICE_PROTOCOL));
            builder.placeholder(SERVICE_HOST, conf.getString(SERVICE_HOST, DEFAULT_SERVICE_HOST));
            builder.placeholder(SERVICE_PORT, conf.getString(SERVICE_PORT, DEFAULT_SERVICE_PORT));
            builder.placeholder(SERVICE_PATH, conf.getString(SERVICE_PATH, DEFAULT_SERVICE_PATH));
            return builder.build().build(CardioMoodDataService.class);
        //return new ServiceStub();
    }

    public CardioMoodDataService getService() {
        synchronized (INSTANCE) {
            if (service == null)
                service = buildService();
        }
        return service;
    }

}
