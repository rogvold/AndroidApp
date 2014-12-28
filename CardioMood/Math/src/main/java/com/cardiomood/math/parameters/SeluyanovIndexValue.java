package com.cardiomood.math.parameters;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Created by danon on 18.02.14.
 */
public class SeluyanovIndexValue extends AbstractSingleValue {

    @Override
    public String getName() {
        return "SDNN";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        if (length < 3)
            return 0;
        double d[] = new double[length-1];
        for (int i=0; i<d.length && begin+i+1<y.length; i++) {
            d[i] = y[begin+i+1] - y[begin+i];
        }
        return Math.sqrt(StatUtils.populationVariance(d));
    }
}
