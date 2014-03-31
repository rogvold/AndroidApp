package com.cardiomood.math.spectrum;

/**
 * Created by danon on 31.03.2014.
 */
public interface SpectralPowerEvaluator {

    double[] getPower();
    double toFrequency(int index);

}
