package com.cardiomood.math.parameters;

/**
 * Created by danon on 21.02.14.
 */
public class SDANN90mValue extends SDNNValue {

    private final double STEP = 90*60*1000; // 90min

    public SDANN90mValue() {
    }

    @Override
    public String getName() {
        return "SDANN90m";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return evaluateSDANNt(x, y, begin, length, STEP);
    }
}
