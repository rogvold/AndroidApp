package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN1mValue extends SDNNValue {

    private final double STEP = 60*1000; // 1min

    @Override
    public String getName() {
        return "SDANN1m";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
