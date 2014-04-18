package com.cardiomood.math.filter;

import java.util.Arrays;

/**
 * Created by danon on 10.04.2014.
 */
public class PisarukArtifactFilter implements ArtifactFilter {
    @Override
    public double[] doFilter(double[] rr) {
        double[] result = Arrays.copyOf(rr, rr.length);
        for (int i=1; i<result.length-1; i++) {
            double a = result[i-1];
            if (Math.abs(result[i] - a) > 100.0) {
                result[i] = (result[i+1] + a) / 2.0;
            }
        }
        return result;
    }

    @Override
    public int getArtifactsCount(double[] rr) {
        int count = 0;
        for (int i=1; i<rr.length-1; i++) {
            double a = rr[i-1];
            if (Math.abs(rr[i] - a) > 100.0) {
                count++;
            }
        }
        return count;
    }
}
