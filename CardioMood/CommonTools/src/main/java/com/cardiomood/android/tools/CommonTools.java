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
import java.security.MessageDigest;
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

    public static String encryptString(String s, String algo) {
        if (s == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            md.update(s.getBytes());
            s = null;
            return toHexString(md.digest());
        } catch (Exception ex) {
            Log.e(TAG, "encryptString(): Couldn't calculate MD5 of string", ex);
            throw new RuntimeException("Couldn't calculate MD5 of string", ex);
        }
    }

    public static String toHexString(byte[] byteData) {
        if (byteData == null) {
            return null;
        }
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static byte[] fromHexString(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String SHA256(String s) {
        return encryptString(s, "SHA-256");
    }

    public static String MD5(String s) {
        return encryptString(s, "MD5");
    }

}