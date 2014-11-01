/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cardiomood.android.step;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects steps and notifies all listeners (that implement StepListener).
 * @author Levente Bagi
 */
public class StepDetector implements SensorEventListener {

    private static final String TAG = "StepDetector";

    public static final float SENSITIVITY_EXTRA_LOW = 50.625f;
    public static final float SENSITIVITY_VERY_LOW = 33.75f;
    public static final float SENSITIVITY_LOW = 22.5f; // 10 * 1.5f^2
    public static final float SENSITIVITY_LOWER = 15.0f; // 10 * 1.5
    public static final float SENSITIVITY_MEDIUM = 10.0f;
    public static final float SENSITIVITY_HIGHER = 6.6667f; // 10 * 1.5^(-1)
    public static final float SENSITIVITY_HIGH = 4.4444f; // 10 * 1.5^(-2)
    public static final float SENSITIVITY_VERY_HIGH = 2.9630f;
    public static final float SENSITIVITY_EXTRA_HIGH = 1.9753f;

    private float   mLimit = SENSITIVITY_MEDIUM;
    private float   mLastValues[] = new float[3*2];
    private float   mScale[] = new float[2];
    private float   mYOffset;

    private float   mLastDirections[] = new float[3*2];
    private float   mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float   mLastDiff[] = new float[3*2];
    private int     mLastMatch = -1;
    
    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();
    
    public StepDetector() {
        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
    }
    
    public void setSensitivity(float sensitivity) {
        synchronized (this) {
            mLimit = sensitivity; // 1.97  2.96  4.44  6.66  10.00  15.00  22.50  33.75  50.62
        }
    }

    public float getSensitivity() {
        return mLimit;
    }

    public float calibrateSensitivity(List<SensorEvent> events, int expectedSteps) {
        // TODO: implement binary search
        setSensitivity(SENSITIVITY_MEDIUM);
        return getSensitivity();
    }
    
    public void addStepListener(StepListener sl) {
        synchronized (this) {
            mStepListeners.add(sl);
        }
    }
    
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor; 
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
            }
            else {
                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
                if (j == 1) {
                    float vSum = 0;
                    for (int i=0 ; i<3 ; i++) {
                        final float v = mYOffset + event.values[i] * mScale[j];
                        vSum += v;
                    }
                    int k = 0;
                    float v = vSum / 3;

                    float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                    if (direction == - mLastDirections[k]) {
                        // Direction changed
                        int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                        mLastExtremes[extType][k] = mLastValues[k];
                        float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                        if (diff > mLimit) {

                            boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
                            boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
                            boolean isNotContra = (mLastMatch != 1 - extType);

                            if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                                Log.i(TAG, "step");
                                for (StepListener stepListener : mStepListeners) {
                                    stepListener.onStep();
                                }
                                mLastMatch = extType;
                            }
                            else {
                                mLastMatch = -1;
                            }
                        }
                        mLastDiff[k] = diff;
                    }
                    mLastDirections[k] = direction;
                    mLastValues[k] = v;
                }
            }
        }
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

}