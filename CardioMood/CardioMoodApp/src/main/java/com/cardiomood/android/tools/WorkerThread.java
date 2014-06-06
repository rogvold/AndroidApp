package com.cardiomood.android.tools;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class WorkerThread<T> extends Thread {

    public static final long TIMEOUT = 200;
    private final BlockingQueue<T> queue = new LinkedBlockingQueue();
    private volatile boolean finished = false;

    @Override
    public void run() {
        onStart();
        while (!finished && !isInterrupted()) {
            try {
                T item = queue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!isInterrupted() && item != null) {
                    processItem(item);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        if (finished) {
            int k = queue.size();
            while (k > 0) {
                T item = queue.poll();
                processItem(item);
                k--;
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

    public boolean hasMoreElements() {
        return !queue.isEmpty();
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void finishWork() {
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public abstract void processItem(T item);
}
