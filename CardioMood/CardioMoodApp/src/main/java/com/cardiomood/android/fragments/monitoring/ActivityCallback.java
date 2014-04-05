package com.cardiomood.android.fragments.monitoring;

/**
 * Created by danon on 04.03.14.
 */
public interface ActivityCallback {

    void registerFragmentCallback(FragmentCallback callback);

    void unregisterFragmentCallback(FragmentCallback callback);
}
