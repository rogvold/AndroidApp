package com.cardiomood.android.kolomna.db;

import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;

public class HelperFactory {

   private volatile static DatabaseHelper databaseHelper;
   
   public static DatabaseHelper getHelper() {
       return databaseHelper;
   }

   public synchronized static void setHelper(Context context) {
       databaseHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
   }

   public synchronized static void releaseHelper() {
       OpenHelperManager.releaseHelper();
       databaseHelper = null;
   }
}