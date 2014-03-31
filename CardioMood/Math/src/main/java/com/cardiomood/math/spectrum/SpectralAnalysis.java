package com.cardiomood.math.spectrum;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Created by danon on 22.02.14.
 */
public class SpectralAnalysis {

    private final Algorithm algorithm;
    private final SpectralPowerEvaluator spe;
    private final double[] power;
    private double TP = 0;
    private double ULF = 0;
    private double VLF = 0;
    private double LF = 0;
    private double HF = 0;

    public SpectralAnalysis(double[] x, double[] y) {
        this(x, y, Algorithm.ACF);
    }

    public SpectralAnalysis(double[] x, double[] y, Algorithm algorithm) {
        this.algorithm = algorithm;
        spe = algorithm.getInstance(x, y);
        power = spe.getPower();

        final double step = spe.toFrequency(1);

        int i = 1;
        // ULF: [0.0 .. 0.0033 Hz]
        for (; spe.toFrequency(i) < 0.0033; i++) {
            ULF += power[i];
        }
        ULF *= step;
        TP += ULF;

        // VLF: [0.0033 .. 0.04 Hz]
        for (; spe.toFrequency(i) < 0.04; i++) {
            VLF += power[i];
        }
        VLF *= step;
        TP += VLF;

        // LF: [0.04 .. 0.15 Hz]
        for (; spe.toFrequency(i) < 0.15; i++) {
            LF += power[i];
        }
        LF *= step;
        TP += LF;

        // HF: [0.15 .. 0.4 Hz]
        for (; spe.toFrequency(i) <= 0.4; i++) {
            HF += power[i];
        }
        HF *= step;
        TP += HF;

        for (; spe.toFrequency(i) < 0.5; i++)
            TP += power[i]*step;

    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public double[] getPower() {
        return power;
    }

    public double getTP() {
        return TP;
    }

    public double getULF() {
        return ULF;
    }

    public double getVLF() {
        return VLF;
    }

    public double getLF() {
        return LF;
    }

    public double getHF() {
        return HF;
    }

    public double toFrequency(int i) {
        return spe.toFrequency(i);
    }

    public PolynomialSplineFunction getSplinePower() {
        double[] power = getPower();
        double[] freq = new double[power.length];
        for (int i=0; i<freq.length; i++)
            freq[i] = spe.toFrequency(i);

        return new SplineInterpolator().interpolate(freq, power);
    }

    public static enum Algorithm {
        FFT,
        ACF;

        public SpectralPowerEvaluator getInstance(double t[], double rr[]) {
            //rr = new SimpleBayevskyFilter().doFilter(rr);
            switch (this) {
                case FFT:
                    return new FFT(t, rr);
                case ACF:
                    return new ACF(rr);
            }
            return null;
        }

    }
}
