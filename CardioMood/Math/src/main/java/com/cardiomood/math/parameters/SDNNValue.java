package com.cardiomood.math.parameters;

import com.cardiomood.math.filter.ArtifactFilter;
import com.cardiomood.math.filter.PisarukArtifactFilter;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;

import java.util.Arrays;

/**
 * Created by danon on 18.02.14.
 */
public class SDNNValue extends AbstractSingleValue {

    private static final ArtifactFilter FILTER = new PisarukArtifactFilter();

    @Override
    public String getName() {
        return "SDNN";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        if (begin > y.length)
            begin = y.length;
        double[] nn = FILTER.doFilter(Arrays.copyOfRange(y, begin, Math.min(begin+length, y.length)));
        return Math.sqrt(StatUtils.populationVariance(nn));
    }

    public double evaluateSDANNt(double[] x, double[] y, int begin, int length, double t) {
        DoubleArray rr = new ResizableDoubleArray();
        StandardDeviation stdDev = new StandardDeviation();
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
