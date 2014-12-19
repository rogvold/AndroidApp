package com.cardiomood.math.filter;

import java.util.Arrays;

/**
 * Created by danon on 10.04.2014.
 */
public class PisarukArtifactFilter implements ArtifactFilter, Scan3Filter {

    @Override
    public double[] doFilter(double[] rr) {
        double[] result = Arrays.copyOf(rr, rr.length);
        for (int i=1; i<result.length-1; i++) {
            result[i] = filter(rr[i-1], rr[i], rr[i+1]);
        }
        return result;
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
        if (Math.abs(x - a) > 100.0) {
            return (a + b) / 2.0;
        }
        return x;
    }
}
