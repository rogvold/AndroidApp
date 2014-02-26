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
public class FFT {

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
        int k = (int) Math.ceil(maxFreq*2*duration/1000);
        while (n < k)
            n <<= 1;

        maxFreq = toFrequency(n-1);

        result = transform(t, y, 0, t.length, n);
    }

    public
    FFT(double t[], double y[], double maxFreq) {
        this.t = t;
        this.y = y;
        this.maxFreq = maxFreq;
        duration = t[t.length-1] - t[0];
        n = 1;
        int k = (int) Math.ceil(maxFreq*2*duration/1000);
        while (n < k)
            n <<= 1;
        this.maxFreq = toFrequency(n-1);

        result = transform(t, y, 0, t.length, n);
    }

    public double toFrequency(int i) {
        return ((double)i)/duration*1000;
    }

    public void setMaxFrequency(double maxFreq) {
        this.maxFreq = maxFreq;
    }

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

        double m = StatUtils.mean(b);
        for (int i=0; i<b.length; i++)
            b[i] -= m;

        SplineInterpolator spline = new SplineInterpolator();
        UnivariateFunction f = spline.interpolate(a, b);
        return fft.transform(f, x[begin], x[begin+length-1], n, TransformType.FORWARD);
    }

    public static void main(String[] args) {
        double[] t = new double[200];
        double[] f = new double[200];

        double k = 0;
        for (int i=0; i<t.length; i++) {
            t[i] = k;
            k+= 0.1;
            if (i <= 100)
                f[i] = 0.1;
            else f[i] = 0;
        }

        FFT fft = new FFT(t, f, 1000);
        double power[] = fft.getPower();

        for (int i=0 ;i<power.length; i++) {
            System.out.println(fft.toFrequency(i) + " " + power[i]);
        }
    }

}
