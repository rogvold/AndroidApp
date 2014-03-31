package com.cardiomood.math.parameters;

/**
 * Created by danon on 18.02.14.
 */
public class RMSSDValue extends AbstractSingleValue {


    @Override
    public String getName() {
        return "RMSSD";
    }

    @Override
    public double evaluate(double[] x, double[] y, int begin, int length) {
        double s = 0.0;
        int n = 0;
        for (int i=begin+1; i<length+begin && i<y.length; i++) {
            s += (y[i]-y[i-1]);
            n++;
        }
        double m = s/n;
        s = 0.0;
        for (int i=begin+1; i<length+begin && i<y.length; i++) {
            double q = (m - (y[i]-y[i-1]));
            s += q*q;
        }
        return Math.sqrt(s/n);
    }
}
