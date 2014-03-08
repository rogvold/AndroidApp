package com.cardiomood.data.async;

/**
 * Project: CardioMood
 * User: danon
 * Date: 28.04.13
 * Time: 14:59
 */
public abstract class ServerResponseCallbackRetry<T> implements ServerResponseCallback<T> {

    private boolean retryRequired = true;

    public abstract void retry();

    public boolean isRetryRequired() {
        return retryRequired;
    }

    public void setRetryRequired(boolean retryRequired) {
        this.retryRequired = retryRequired;
    }
}