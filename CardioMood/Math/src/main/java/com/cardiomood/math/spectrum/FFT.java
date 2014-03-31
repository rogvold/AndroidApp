package com.cardiomood.math.spectrum;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Created by danon on 21.02.14.
 */
public class FFT implements SpectralPowerEvaluator {

    private final double[] t;
    private final double[] y;

    private Complex[] result;
    private int n = 0;
    private final double duration;
    private double maxFreq = 0.5;

    public FFT(double t[], double y[]) {
        this.t = t;
        this.y = y;
        duration = t[t.length-1] - t[0];
        n = 1;
        int k = (int) Math.ceil(duration/200);
        while (n < k)
            n <<= 1;

        maxFreq = toFrequency(n-1);

        result = transform(t, y, 0, t.length, n);
    }

    @Override
    public double toFrequency(int i) {
        return ((double)i)/duration*1000;
    }

    public double getMaxFrequency() {
        return maxFreq;
    }

    @Override
    public double[] getPower() {
        double[] result = new double[n/2];
        for (int i=0; i<result.length; i++) {
            result[i] = this.result[i].abs()/n;
            result[i] *= result[i];
        }
        return result;
    }

    public static Complex[] transform(double[] x, double[] y, int begin, int length, int n) {
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        double a[] = new double[length];
        double b[] = new double[length];
        System.arraycopy(x, begin, a, 0, length);
        System.arraycopy(y, begin, b, 0, length);
//        for (int i=0; i<a.length; i++) {
//            b[i] = Math.cos(a[i])*Math.cos(a[i])*b[i];
//        }
        double m = StatUtils.mean(b);
        for (int i=0; i<b.length; i++)
            b[i] -= m;

        SplineInterpolator spline = new SplineInterpolator();
        UnivariateFunction f = spline.interpolate(a, b);
        return fft.transform(f, x[begin], x[begin+length-1], n, TransformType.FORWARD);
    }

}
