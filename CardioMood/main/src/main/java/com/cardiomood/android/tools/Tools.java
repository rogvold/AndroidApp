package com.cardiomood.android.tools;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
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
}
