package com.cardiomood.android.db;

import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;

public class DatabaseHelperFactory {

   private static DatabaseHelper databaseHelper;
   
   synchronized public static DatabaseHelper getHelper() {
       return databaseHelper;
   }

   synchronized  public static void initialize(Context context) {
       databaseHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
   }

   synchronized public static void releaseHelper() {
       OpenHelperManager.releaseHelper();
       databaseHelper = null;
   }
}