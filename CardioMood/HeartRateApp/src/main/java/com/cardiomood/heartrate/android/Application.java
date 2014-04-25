package com.cardiomood.heartrate.android;

import android.os.Build;

import com.cardiomood.android.tools.CommonTools;
import com.cardiomood.android.tools.PreferenceHelper;
import com.cardiomood.heartrate.android.tools.ConfigurationConstants;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.PushService;

import java.util.Date;

/**
 * Created by danon on 19.04.2014.
 */
public class Application extends android.app.Application {

    private static final String PARSE_APP_ID = "2cUQsV8z56nDEeWPqmoDNdPdkcA1763mSfOUEWiQ";
    private static final String PARSE_CLIENT_ID = "Ns9hMX3FHax69xoG2Xy71iPkumjRkkZoPUCMGf5g";

    private PreferenceHelper prefHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        //OpenHelperManager.getHelper(this, DatabaseHelper.class).getWritableDatabase();

        prefHelper = new PreferenceHelper(getApplicationContext(), true);
        String gender = prefHelper.getString(ConfigurationConstants.USER_SEX_KEY, "UNSPECIFIED");
        long birthDate = prefHelper.getLong(ConfigurationConstants.USER_BIRTH_DATE_KEY, 0);
        String country = getResources().getConfiguration().locale.getCountry();
        String locale = getResources().getConfiguration().locale.toString();
        String language = getResources().getConfiguration().locale.getLanguage();

        // Initialize Parse
        Parse.initialize(this, PARSE_APP_ID, PARSE_CLIENT_ID);

        // Set up Parse Push Notifications
        // 1. Set up channels
        PushService.subscribe(this, "type-beta", MainActivity.class);
        PushService.subscribe(this, "gender-"+gender, MainActivity.class);
        PushService.subscribe(this, "country-"+country, MainActivity.class);
        PushService.subscribe(this, "locale-"+locale, MainActivity.class);
        PushService.subscribe(this, "lang-"+language, MainActivity.class);
        PushService.subscribe(this, "os-android", MainActivity.class);
        if (birthDate > 0) {
            PushService.subscribe(this, "age-" + CommonTools.getAge(new Date(birthDate)), MainActivity.class);
        }

        // 2. Set up Parse targeting parameters
        ParseInstallation parseInstallation = ParseInstallation.getCurrentInstallation();
        parseInstallation.put("gender", gender);
        if (birthDate > 0) {
            parseInstallation.put("birth_date", birthDate);
        } else parseInstallation.remove("birth_date");
        if (birthDate > 0) {
            parseInstallation.put("age", CommonTools.getAge(new Date(birthDate)));
        } else parseInstallation.remove("age");
        parseInstallation.put("country", country);
        parseInstallation.put("locale", locale);
        parseInstallation.put("os", "android");
        parseInstallation.put("sdk_version", Build.VERSION.SDK_INT);

        // Apply all configuration changes
        parseInstallation.saveInBackground();
    }
}
