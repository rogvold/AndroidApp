package com.cardiomood.math.filter;

/**
 * Created by danon on 07.04.2014.
 */
public class SimpleInterpolationArtifactFilter implements ArtifactFilter {


    @Override
    public double[] doFilter(double[] rrIntervals) {
        return new double[0];
    }

    @Override
    public int getArtifactsCount(double[] rrIntervals) {
        return 0;
    }
}
