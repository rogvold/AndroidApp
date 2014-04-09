package com.cardiomood.math.filter;

public interface ArtifactFilter {

    double[] doFilter(double[] rrIntervals);

    int getArtifactsCount(double[] rrIntervals);

}
