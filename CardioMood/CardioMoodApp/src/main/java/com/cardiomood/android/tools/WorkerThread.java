package com.cardiomood.android.tools;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class WorkerThread<T> extends Thread {

    public static final long TIMEOUT = 200;
    private final BlockingQueue<T> queue = new LinkedBlockingQueue();

    @Override
    public void run() {
        onStart();
        while (!isInterrupted()) {
            try {
                T item = queue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!isInterrupted() && item != null) {
                    processItem(item);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        onStop();
    }

    public void put(T e) {
        queue.add(e);
    }

    public void onStart() {

    }

    public void onStop() {

    }

    public abstract void processItem(T item);
}
