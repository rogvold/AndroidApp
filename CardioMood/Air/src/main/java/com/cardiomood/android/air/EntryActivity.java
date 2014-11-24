package com.cardiomood.android.air;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.cardiomood.android.air.service.TrackingService;
import com.cardiomood.android.air.util.SplashScreenActivity;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by antondanhsin on 13/11/14.
 */
public class EntryActivity extends SplashScreenActivity {

    private static final String TAG = EntryActivity.class.getSimpleName();

    private static final long WAIT_TIMEOUT = 1000;

    private boolean sessionRunning = false;

    private Messenger trackingService;
    private boolean trackingServiceBound = false;
    private CountDownLatch latch;

    private Task task;

    private ServiceConnection trackingServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            trackingService = new Messenger(service);
            requestServiceStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            trackingServiceBound = false;
            trackingService = null;
        }
    };

    protected class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //this switch reads the information in the message (usually just
                //an integer) and will do something depending on which integer is sent
                case TrackingService.MSG_GET_STATUS:
                    long sessionId = msg.getData().getLong("sessionId", -1L);
                    // check whether the tracking session is started
                    if (sessionId != -1L) {
                        setSessionRunning(true);
                    } else {
                        setSessionRunning(false);
                    }
                    latch.countDown();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    protected Messenger mMessenger = new Messenger(new IncomingHandler());


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
        }, WAIT_TIMEOUT);

        // start service
        Intent intent = new Intent(this, TrackingService.class);
        startService(intent);

        // Bind GPSService
        bindService(intent, trackingServiceConnection, Context.BIND_AUTO_CREATE);
        trackingServiceBound = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (trackingServiceBound) {
            unbindService(trackingServiceConnection);
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

    void requestServiceStatus() {
        // check Service status
        Message msg = Message.obtain(null, TrackingService.MSG_GET_STATUS);
        msg.replyTo = mMessenger;
        try {
            trackingService.send(msg);
        } catch (RemoteException ex) {
            Log.d(TAG, "requestServiceStatus(): failed", ex);
            setSessionRunning(false);
            latch.countDown();
        }
    }
}
