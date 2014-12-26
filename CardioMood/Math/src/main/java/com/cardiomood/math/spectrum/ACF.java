package com.cardiomood.math.spectrum;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Created by danon on 31.03.2014.
 */
public class ACF implements SpectralPowerEvaluator {

    private static final double FREQUENCY_STEP = 0.005;

    double[] rr;
    double[] r;
    double[] g;
    int n;

    public ACF(double[] rr) {
        this.rr = rr;
        n = rr.length;
        r = new double[n/4];
        g = new double[101];
        init();
    }

    private void init() {
        double m = StatUtils.mean(rr);
        double d = StatUtils.variance(rr);
        for (int k=0; k<r.length; k++) {
            double s = 0.0;
            for (int i=0; i<n-k-1; i++)
                s += (rr[i] - m)*(rr[i+k+1]-m);
            r[k] = s/n;
        }

        g[0] = 0;
        double f = 0;
        for (int i=1; i<=100; i++) {
            double s = 0.0;
            f += FREQUENCY_STEP;
            double w = f*Math.PI*2;
            for (int k=0; k<r.length; k++)
                s += r[k] * Math.cos(w*(k+1));
            g[i] = Math.abs(2*(d + 2*s));
        }
    }

    @Override
    public double[] getPower() {
        return g.clone();
    }

    @Override
    public double toFrequency(int index) {
        return FREQUENCY_STEP * index;
    }
}
