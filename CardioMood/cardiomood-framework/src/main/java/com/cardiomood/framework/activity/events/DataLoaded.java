package com.cardiomood.framework.activity.events;

/**
 * Created by Anton Danshin on 11/01/15.
 */
public class DataLoaded<E> {

    private final E data;

    public DataLoaded(E data) {
        this.data = data;
    }

    public E getData() {
        return data;
    }
}
