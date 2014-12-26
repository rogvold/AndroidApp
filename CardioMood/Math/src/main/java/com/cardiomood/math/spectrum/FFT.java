package com.cardiomood.math.spectrum;

import com.cardiomood.math.interpolation.ConstrainedSplineInterpolator;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Created by danon on 21.02.14.
 */
public class FFT implements SpectralPowerEvaluator {

    private static final UnivariateInterpolator INTERPOLATOR = new ConstrainedSplineInterpolator();

    private final Complex[] result;
    private final int n;
    private final double duration;
    private final double maxFreq;

    public FFT(double t[], double y[]) {
        duration = t[t.length-1] - t[0];
        n = getN();
        maxFreq = toFrequency(n-1);

        result = transform(t, y, 0, t.length, n);
        result[0] = new Complex(0, 0);
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
        double[] power = new double[n/2];
        for (int i=0; i<n/2; i++) {
            power[i] = result[i].abs()/n;
            power[i] *= power[i];
        }
        return power;
    }

    public static Complex[] transform(double[] x, double[] y, int begin, int length, int n) {
        double a[] = new double[length];
        double b[] = new double[length];
        System.arraycopy(x, begin, a, 0, length);
        System.arraycopy(y, begin, b, 0, length);

        // subtract mean value
        double m = StatUtils.mean(b);
        for (int i=0; i<b.length; i++)
            b[i] -= m;

        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        UnivariateFunction f = INTERPOLATOR.interpolate(a, b);
        return fft.transform(f, x[begin], x[begin+length-1], n, TransformType.FORWARD);
    }

    private int getN() {
        int n = 1;
        int k = (int) Math.ceil(duration/200);
        while (n < k) {
            n <<= 1;
        }
        return n;
    }

}
