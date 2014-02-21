package com.cardiomood.sport.android.client;

import android.content.Context;

import com.cardiomood.android.tools.ConfigurationManager;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.sport.android.tools.config.ConfigurationConstants;

import org.codegist.crest.CRestBuilder;

/**
 * Project: CardioSport
 * User: danon
 * Date: 15.06.13
 * Time: 14:02
 */
public class CardioSportService implements ConfigurationConstants {

    public static ICardioSportService rebuildService(Context ctx) {
        ConfigurationManager config = ConfigurationManager.getInstance();
        PreferenceHelper pref = new PreferenceHelper(ctx);
        config.setString(SERVICE_PROTOCOL, pref.getString(SERVICE_PROTOCOL, DEFAULT_SERVICE_PROTOCOL));
        config.setString(SERVICE_HOST, pref.getString(SERVICE_HOST, DEFAULT_SERVICE_HOST));
        config.setString(SERVICE_PORT, pref.getString(SERVICE_PORT, DEFAULT_SERVICE_PORT));
        config.setString(SERVICE_PATH, pref.getString(SERVICE_PATH, DEFAULT_SERVICE_PATH));
        return Holder.instance = Holder.buildService();
    }

    private static class Holder {
        private static ICardioSportService instance = new ServiceStub(); //buildService();

        private static ICardioSportService buildService() {
            ConfigurationManager conf = ConfigurationManager.getInstance();
            CRestBuilder builder = new CRestBuilder();
            builder.placeholder(SERVICE_PROTOCOL, conf.getString(SERVICE_PROTOCOL, DEFAULT_SERVICE_PROTOCOL));
            builder.placeholder(SERVICE_HOST, conf.getString(SERVICE_HOST, DEFAULT_SERVICE_HOST));
            builder.placeholder(SERVICE_PORT, conf.getString(SERVICE_PORT, DEFAULT_SERVICE_PORT));
            builder.placeholder(SERVICE_PATH, conf.getString(SERVICE_PATH, DEFAULT_SERVICE_PATH));
            return builder.build().build(ICardioSportService.class);
        }
    }

    public static ICardioSportService getInstance() {
        return Holder.instance;
    }



}
