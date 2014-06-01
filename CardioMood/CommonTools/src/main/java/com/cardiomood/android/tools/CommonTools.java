package com.cardiomood.android.tools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
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

    public static int getAge(Date dateOfBirth) {

        Calendar today = Calendar.getInstance();
        Calendar birthDate = Calendar.getInstance();

        int age = 0;

        birthDate.setTime(dateOfBirth);
        if (birthDate.after(today)) {
            throw new IllegalArgumentException("Can't be born in the future");
        }

        age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

        // If birth date is greater than todays date (after 2 days adjustment of leap year) then decrement age one year
        if ( (birthDate.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR) > 3) ||
                (birthDate.get(Calendar.MONTH) > today.get(Calendar.MONTH ))){
            age--;

            // If birth date and todays date are of same month and birth day of month is greater than todays day of month then decrement age
        }else if ((birthDate.get(Calendar.MONTH) == today.get(Calendar.MONTH )) &&
                (birthDate.get(Calendar.DAY_OF_MONTH) > today.get(Calendar.DAY_OF_MONTH ))){
            age--;
        }

        return age;
    }

    public static String getAndroidDeviceID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static short[] parseArrayOfShort(String s) {
        if (s == null || s.isEmpty()) {
            return new short[0];
        }
        int len = s.length();
        short[] data = new short[len / 4];
        for (int i = 0; i < len; i += 4) {
            data[i / 2] = (short) ((Character.digit(s.charAt(i), 16) << 12)
                    + Character.digit(s.charAt(i+1), 16) << 8
                    + Character.digit(s.charAt(i+2), 16) << 4
                    + Character.digit(s.charAt(i+3), 16));
        }
        return data;
    }

    public static String arrayOfShortToHexString(short[] a) {
        if (a == null)
            a = new short[0];
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[a.length * 4];
        int v;
        for ( int j = 0; j < a.length; j++ ) {
            v = a[j] & 0xFFFF;
            hexChars[j * 4] = hexArray[v >>> 12];
            hexChars[j * 4 + 1] = hexArray[(v & 0x0F00) >>> 8];
            hexChars[j * 4 + 1] = hexArray[(v & 0x00F0) >>> 4];
            hexChars[j * 4 + 3] = hexArray[v & 0x000F];
        }
        return new String(hexChars);
    }

    public static boolean isGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

}
