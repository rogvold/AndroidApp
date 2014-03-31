package com.cardiomood.math.filter;

public interface Filter {

    double[] doFilter(double[] rrIntervals);

    int getArtifactsCount(double[] rrIntervals);

}
