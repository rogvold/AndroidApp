package com.cardiomood.android.tools.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class WorkerThread<T> extends Thread {

    public static final long POLL_TIMEOUT = 100;


    private final Object lock = new Object();
    private final BlockingQueue<T> queue = new LinkedBlockingQueue();
    private volatile boolean finished = false;
    private long lastItemTime = 0;

    @Override
    public void run() {
        onStart();
        while (!finished && !isInterrupted()) {
            try {
                T item = queue.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
                if (!isInterrupted() && item != null) {
                    lastItemTime = System.currentTimeMillis();
                    processItem(item);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            onCycle();
        }
        if (finished) {
            int k = queue.size();
            while (k > 0) {
                T item = queue.poll();
                processItem(item);
                k--;
            }
            onCycle();
        }
        onStop();
    }

    protected void onCycle() {

    }

    public void put(T e) {
        synchronized (lock) {
            if (!finished)
                queue.add(e);
        }
    }

    public void onStart() {}

    public void onStop() {}

    public long getLastItemTime() {
        return lastItemTime;
    }

    public boolean hasMoreElements() {
        return !queue.isEmpty();
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void finishWork() {
        synchronized (lock) {
            finished = true;
        }
    }

    public boolean isFinished() {
        synchronized (lock) {
            return finished;
        }
    }

    public abstract void processItem(T item);
}
