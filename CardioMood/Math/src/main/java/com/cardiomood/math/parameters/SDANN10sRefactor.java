package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN10sRefactor extends SDNNValue {

    private final double STEP = 10*1000; // 10s

    @Override
    public String getName() {
        return "SDANN10s";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
