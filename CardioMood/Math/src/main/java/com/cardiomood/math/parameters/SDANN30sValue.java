package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN30sValue extends SDNNValue {

    private final double STEP = 30*1000; // 30s

    @Override
    public String getName() {
        return "SDANN30s";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
