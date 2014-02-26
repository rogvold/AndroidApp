package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN10mValue extends SDNNValue {

    private final double STEP = 10*60*1000; // 10min

    public SDANN10mValue() {
    }

    @Override
    public String getName() {
        return "SDANN10m";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
