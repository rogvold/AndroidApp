package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN2mValue extends SDNNValue {

    private final double STEP = 2*60*1000; // 2min

    @Override
    public String getName() {
        return "SDANN2m";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
