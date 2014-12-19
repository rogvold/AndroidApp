package com.cardiomood.math.filter;

import java.util.Arrays;

/**
 * Created by Anton Danshin on 30/11/14.
 */
public class Median3Filter implements ArtifactFilter, Scan3Filter {
    @Override
    public double[] doFilter(double[] rrIntervals) {
        double[] result = Arrays.copyOf(rrIntervals, rrIntervals.length);
        if (rrIntervals.length <= 2) {
            return result;
        }
        for (int i=1; i<rrIntervals.length-1; i++) {
            result[i] = filter(rrIntervals[i-1], rrIntervals[i], rrIntervals[i+1]);
        }
        return result;
    }

    @Override
    public int getArtifactsCount(double[] rrIntervals) {
        return 0;
    }

    @Override
    public double filter(double a, double x, double b) {
        double[] rr = new double[] {a, x, b};
        Arrays.sort(rr);
        return rr[1];
    }
}
