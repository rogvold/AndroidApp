package com.cardiomood.android.air;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.cardiomood.android.air.gps.GPSService;
import com.cardiomood.android.air.gps.GPSServiceApi;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by antondanhsin on 13/11/14.
 */
public class EntryActivity extends SplashScreenActivity {

    private static final String TAG = EntryActivity.class.getSimpleName();

    private volatile boolean sessionRunning = false;

    private GPSServiceApi gpsService;
    private boolean gpsBound = false;
    private CountDownLatch latch;

    private Task task;

    private ServiceConnection gpsConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            gpsService = GPSServiceApi.Stub.asInterface(service);

            // hide notification (it is not needed when the UI is active)
            try {
                if (!gpsService.isRunning()) {
                    gpsService.hideNotification();
                } else {
                    gpsService.showNotification();
                }
            } catch (RemoteException ex) {
                Log.d(TAG, "onServiceConnected(): api.hideNotification() failed", ex);
            }

            // check Service status and update UI
            try {
                // check whether the tracking session is started
                if (gpsService.isRunning()) {
                    setSessionRunning(true);
                } else {
                    setSessionRunning(false);
                }
            } catch (RemoteException ex) {
                Log.d(TAG, "onServiceConnected(): api.isRunning() failed", ex);
                setSessionRunning(false);
            }

            latch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            gpsBound = false;
            gpsService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        latch = new CountDownLatch(2);
        task = Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    latch.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        });
        task.continueWith(new Continuation<Object, Object>() {
            @Override
            public Object then(Task<Object> task) throws Exception {
                if (task.isCompleted() && !Thread.currentThread().isInterrupted()) {
                    startNextActivity();
                } else {
                    finish();
                }
                task = null;
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        }, 2000L);

        // start service
        Intent intent = new Intent(this, GPSService.class);
        startService(intent);

        // Bind GPSService
        bindService(intent, gpsConnection, Context.BIND_AUTO_CREATE);
        gpsBound = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (gpsBound) {
            unbindService(gpsConnection);
        }
    }

    void setSessionRunning(boolean running) {
        sessionRunning = running;
    }

    void startNextActivity() {
        if (sessionRunning) {
            startActivity(new Intent(this, TrackingActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
