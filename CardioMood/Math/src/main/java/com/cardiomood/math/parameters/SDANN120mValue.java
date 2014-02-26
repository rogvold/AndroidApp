package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN120mValue extends SDNNValue {

    private final double STEP = 120*60*1000; // 120min

    public SDANN120mValue() {
    }

    @Override
    public String getName() {
        return "SDANN120m";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
