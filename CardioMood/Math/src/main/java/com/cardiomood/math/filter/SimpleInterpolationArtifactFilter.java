package com.cardiomood.math.filter;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;

import java.util.Arrays;

/**
 * Created by danon on 07.04.2014.
 */
public class SimpleInterpolationArtifactFilter implements ArtifactFilter {

    private static final int WINDOW = 6;
    private static final UnivariateInterpolator INTERPOLATOR = new SplineInterpolator();
    private static final double VALUE_THRESHOLD = 0.2;
    private static final int COUNT_THRESHOLD = 2;

    @Override
    public double[] doFilter(double[] rrIntervals) {
        int artifact[] = new int[rrIntervals.length];
        for (int i=0; i<rrIntervals.length-WINDOW; i++) {
            double window[] = Arrays.copyOfRange(rrIntervals, i, i+WINDOW);
            double mean = StatUtils.mean(window);
            for (int j=0; j<window.length; j++) {
                if (Math.abs(window[j] - mean)/mean > VALUE_THRESHOLD) {
                    artifact[i+j]++;
                }
            }
        }

        DoubleArray x = new ResizableDoubleArray();
        DoubleArray rr = new ResizableDoubleArray();
        for (int i=0; i<rrIntervals.length; i++) {
            if (artifact[i] < COUNT_THRESHOLD)  {
                rr.addElement(rrIntervals[i]);
                x.addElement(i);
            }
        }
        UnivariateFunction f = INTERPOLATOR.interpolate(x.getElements(), rr.getElements());
        double[] result = Arrays.copyOf(rrIntervals, rrIntervals.length);
        for (int i=0; i<result.length; i++) {
            if (artifact[i] >= COUNT_THRESHOLD)  {
                result[i] = f.value(i);
                if (result[i] < 500)
                    result[i] = 2100;
                if (result[i] > 300)
                    result[i] = 280;
            }
        }
        return result;
    }

    @Override
    public int getArtifactsCount(double[] rrIntervals) {
        int count = 0;
        int artifact[] = new int[rrIntervals.length];
        for (int i=0; i<rrIntervals.length-WINDOW; i++) {
            double window[] = Arrays.copyOfRange(rrIntervals, i, i+WINDOW);
            double mean = StatUtils.mean(window);
            for (int j=0; j<window.length; j++) {
                if (Math.abs(window[j] - mean)/mean > VALUE_THRESHOLD) {
                    artifact[i+j]++;
                }
            }
        }

        for (int i=0; i<rrIntervals.length; i++) {
            if (artifact[i] >= COUNT_THRESHOLD)  {
                count++;
            }
        }

        return count;
    }
}
