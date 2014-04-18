package com.cardiomood.math.parameters;

import com.cardiomood.math.histogram.Histogram;

/**
 * Created by danon on 18.04.2014.
 */
public class AValue extends AbstractSingleValue {

    @Override
    public String getName() {
        return "SI";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        int end = (begin + length > y.length) ? y.length : begin + y.length;
        double[] rr = new double[end-begin];
        System.arraycopy(y, begin, rr, 0, rr.length);
        Histogram h = new Histogram(rr, 50);
        return h.getA();
    }
}
