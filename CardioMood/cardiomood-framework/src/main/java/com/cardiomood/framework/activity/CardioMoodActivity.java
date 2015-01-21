package com.cardiomood.framework.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.squareup.otto.Bus;

/**
 * Base class for all activities that are using CardioMood Framework.
 *
 * Created by Anton Danshin on 10/01/15.
 */
public class CardioMoodActivity extends ActionBarActivity {

    private Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBus = new Bus(getClass().getSimpleName() + ".bus");
        mBus.register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBus.unregister(this);
    }

    public final Bus getBus() {
        return mBus;
    }
}
