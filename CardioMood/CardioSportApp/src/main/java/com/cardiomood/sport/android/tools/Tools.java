package com.cardiomood.sport.android.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.location.LocationManager;
import android.provider.Settings.Secure;
import android.view.Display;
import android.view.WindowManager;

public class Tools {
	public static boolean isBLESupported() {
		try {
			Class.forName("android.bluetooth.BluetoothGattService");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String convertToString(byte[] data) {
		if(data == null)
			return String.valueOf(data);
		StringBuilder sb = new StringBuilder().append("{");
		for(byte b: data) {
			int i = b;
			sb.append((i + 256)%256).append(" ");
		}
		return sb.append("}").toString();
	}
	
	public static String convertToString(short[] data) {
		if(data == null)
			return String.valueOf(data);
		StringBuilder sb = new StringBuilder().append("{");
		for(short b: data) {
			int i = b;
			sb.append((i + Short.MAX_VALUE)%Short.MAX_VALUE).append(" ");
		}
		return sb.append("}").toString();
	}
	
	public static Point getDisplayResolution(Activity activity) {
		if (activity == null) {
			return null;
		}
		Display display = activity.getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size;
	}
	
	public static Point getDisplayResolution(Context ctx) {
		if (ctx == null) {
			return null;
		}
		WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size;
	}
	
	public static String getAndroidDeviceID(Context context) {
		return Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
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

    public static boolean isGPSEnabled(Activity mainActivity) {
        LocationManager locationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
