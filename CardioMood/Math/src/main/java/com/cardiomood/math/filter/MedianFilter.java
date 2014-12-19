package com.cardiomood.math.filter;

import java.util.Arrays;

/**
 * Created by Anton Danshin on 30/11/14.
 */
public class MedianFilter implements ArtifactFilter {

    private int windowSize = 3;

    public MedianFilter(int windowSize) {
        if (windowSize < 3) {
            throw new IllegalArgumentException("Windows size must be  >= 3");
        }
        this.windowSize = windowSize;
    }

    @Override
    public double[] doFilter(double[] rrIntervals) {
        double[] window = new double[windowSize];
        double[] result = Arrays.copyOf(rrIntervals, rrIntervals.length);
        for (int i=windowSize/2; i<rrIntervals.length-windowSize/2; i++) {
            if (i < windowSize/2) {
                for (int j=0; j<windowSize/2-i; j++) {
                    window[j] = rrIntervals[j+i];
                }
                System.arraycopy(rrIntervals, i, window, windowSize/2-i, windowSize - windowSize/2 + i);
            } else if (i > rrIntervals.length-windowSize/2-1) {
                for (int j=windowSize-1; j>=windowSize/2+rrIntervals.length-i; j--) {
                    window[j] = rrIntervals[i + windowSize - j - 1];
                }
                System.arraycopy(rrIntervals, i, window, 0, rrIntervals.length - i);
            } else {
                System.arraycopy(rrIntervals, i-windowSize/2, window, 0, windowSize);
            }
            result[i] = filter(window);
        }
        return result;
    }

    @Override
    public int getArtifactsCount(double[] rrIntervals) {
        return 0;
    }

    public double filter(double[] rr) {
        Arrays.sort(rr);
        if (rr.length % 2 == 1) {
            return rr[rr.length/2];
        } else {
            return (rr[rr.length/2-1] + rr[rr.length/2])/2;
        }
    }
}
