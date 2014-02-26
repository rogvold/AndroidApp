package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN30mValue extends SDNNValue {

    private final double STEP = 30*60*1000; // 30min

    public SDANN30mValue() {
    }

    @Override
    public String getName() {
        return "SDANN30m";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
