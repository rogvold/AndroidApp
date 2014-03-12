package com.cardiomood.android.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by danon on 11.03.14.
 */
public abstract class CommonTools {

    private static final String TAG = CommonTools.class.getSimpleName();

    public static String timeToHumanString(long millis) {
        if (millis > 1000*60*60) {
            return String.format(
                    "%d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
        } else {
            return String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
        }
    }

    @SuppressLint("NewApi")
    public static void vibrate(Context context, long period) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate
        if (v != null && (Build.VERSION.SDK_INT < 11 || v.hasVibrator())) {
            v.vibrate(period);
        }
    }

    public static void hideSoftInputKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(
                    activity.findViewById(android.R.id.content).getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS
            );

    }

    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
                URL url = new URL("http://data.cardiomood.com/");
                HttpURLConnection urlc = (HttpURLConnection) url .openConnection();
                urlc.setRequestProperty("User-Agent", "test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(2000); // mTimeout is in seconds
                urlc.connect();
                if (urlc.getResponseCode() == 200) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "isNetworkAvailable() exception", e);
        }
        return false;
    }

}
