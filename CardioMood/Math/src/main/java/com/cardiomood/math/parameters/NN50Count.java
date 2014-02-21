package com.cardiomood.math.parameters;

/**
 * Created by danon on 18.02.14.
 */
public class NN50Count extends AbstractSingleValue {
    @Override
    public String getName() {
        return "NN50";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        int count = 0;
        for (int i=begin+1; i<length+begin && i<y.length; i++) {
            if (Math.abs(y[i] - y[i-1]) > 50.0)
                count++;
        }
        return count;
    }
}
