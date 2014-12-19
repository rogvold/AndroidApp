package com.cardiomood.math.filter;

/**
 * Created by danon on 03.03.14.
 */
public class SimpleBayevskyArtifactFilter implements ArtifactFilter, Scan3Filter {

    @Override
    public double[] doFilter(double[] rr) {
        double[] rrIntervals = rr.clone();
        for (int i=1; i<rrIntervals.length-1; i++) {
            double a = rr[i-1];
            double x = rr[i];
            double b = rr[i+1];
            rrIntervals[i] = filter(a, x, b);
        }
        return rrIntervals;
    }

    @Override
    public int getArtifactsCount(double[] rr) {
        int count = 0;
        for (int i=1; i<rr.length-1; i++) {
            double a = rr[i-1];
            double x = rr[i];
            double b = rr[i+1];
            if (Math.abs(x - filter(a, x, b)) > 0.1) {
                count++;
            }
        }
        return count;
    }

    @Override
    public double filter(double a, double x, double b) {
        if (Math.abs(x / b - 1) >= 0.2) {
            return Math.abs((a + b) / 2);
        } else if (Math.abs(x / a - 1) >= 0.2)
            return Math.abs((a + b) / 2);
        else return x;
    }
}
