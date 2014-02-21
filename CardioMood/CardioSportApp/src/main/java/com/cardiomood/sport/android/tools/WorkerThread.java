package com.cardiomood.sport.android.tools;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Project: CardioSport
 * User: danon
 * Date: 10.06.13
 * Time: 18:20
 */
public abstract class WorkerThread<T> extends Thread {

    public static long TIMEOUT = 200;
    private final BlockingDeque<T> queue = new LinkedBlockingDeque<T>();

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                T item = extract();
                if (!isInterrupted() && item != null)
                    processItem(item);
            } catch (InterruptedException ex) {
                break;
            }
        }
        clear();
    }

    public void interrupt() {
        clear();
        super.interrupt();
    }

    public void put(T e) {
        queue.add(e);
    }

    private T extract() throws InterruptedException{
            return queue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public void clear() {
            queue.clear();
    }

    public abstract void processItem(T item);
}
