package com.cardiomood.android.tools;

import android.widget.RelativeLayout;

/**
 * Created by danon on 23.04.2014.
 */
public interface AdsControllerBase {

    void createView(RelativeLayout layout);
    void showBanner(boolean show);
    void onStart();
    void onDestroy();
    void onResume();
    void onStop();
    void onPause();
}