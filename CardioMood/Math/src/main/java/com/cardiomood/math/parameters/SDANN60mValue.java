package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN60mValue extends SDNNValue {

    private final double STEP = 60*60*1000; // 60min

    public SDANN60mValue() {
    }

    @Override
    public String getName() {
        return "SDANN60m";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
