package com.cardiomood.math.parameters;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;

/**
 * Created by danon on 18.02.14.
 */
public class SDNNValue extends AbstractSingleValue {

    @Override
    public String getName() {
        return "SDNN";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        return Math.sqrt(StatUtils.populationVariance(y, begin, length));
    }

    protected final double evaluateSDANNt(double[] x, double[] y, int begin, int length, double t) {
        DoubleArray rr = new ResizableDoubleArray();
        org.apache.commons.math3.stat.descriptive.moment.StandardDeviation stdDev = new StandardDeviation();
        double t0 = 0;
        int i = begin;
        while (i < begin + length) {
            t0 = x[i];
            while (x[i] - t0 < t && i < begin + length) {
                rr.addElement(y[i]);
                i++;
            }
            stdDev.increment(StatUtils.mean(rr.getElements()));
        }
        return stdDev.evaluate();
    }
}
