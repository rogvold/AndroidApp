package com.cardiomood.math.parameters;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Created by danon on 18.02.14.
 */
public class Variation extends AbstractSingleValue {

    @Override
    public String getName() {
        return "variance";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return Math.sqrt(StatUtils.populationVariance(y, begin, length))/StatUtils.mean(y, begin, length)*100;
    }
}
