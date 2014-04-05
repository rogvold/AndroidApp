package com.cardiomood.math.filter;

/**
 * Created by danon on 03.03.14.
 */
public class SimpleBayevskyFilter implements Filter {

    @Override
    public double[] doFilter(double[] rr) {
        double[] rrIntervals = rr.clone();
        for (int i=1; i<rrIntervals.length-1; i++) {
            double a = rrIntervals[i-1];
            double x = rrIntervals[i];
            double b = rrIntervals[i+1];
            if (Math.abs(x/b-1) > 0.2) {
                rrIntervals[i] = Math.abs((a + b) / 2);
            } else if (Math.abs(x/a-1) > 0.2)
                rrIntervals[i] = Math.abs((a + b) / 2);
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
            if (Math.abs(x/b-1) > 0.2) {
                count++;
            } else if (Math.abs(x/a-1) > 0.2) {
                count++;
            }
        }
        return count;
    }
}
