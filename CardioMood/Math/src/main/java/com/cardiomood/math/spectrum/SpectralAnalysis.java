package com.cardiomood.math.spectrum;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Created by danon on 22.02.14.
 */
public class SpectralAnalysis {

    private FFT fft;
    private double[] power;
    private double TP = 0;
    private double ULF = 0;
    private double VLF = 0;
    private double LF = 0;
    private double HF = 0;

    public SpectralAnalysis(double[] x, double y[]) {
        fft = new FFT(x, y);
        power = fft.getPower();
        final double step = fft.toFrequency(1)*1000;
        int i = 0;

        // ULF: [0.0 .. 0.0033 Hz]
        for (; fft.toFrequency(i) < 0.0033; i++) {
            ULF += power[i];
        }
        ULF *= step;
        TP += ULF;

        // VLF: [0.0033 .. 0.04 Hz]
        for (; fft.toFrequency(i) < 0.04; i++) {
            VLF += power[i];
        }
        VLF *= step;
        TP += VLF;

        // LF: [0.04 .. 0.15 Hz]
        for (; fft.toFrequency(i) < 0.15; i++) {
            LF += power[i];
        }
        LF *= step;
        TP += LF;

        // HF: [0.15 .. 0.4 Hz]
        for (; fft.toFrequency(i) <= 0.4; i++) {
            HF += power[i];
        }
        HF *= step;
        TP += HF;
    }

    public FFT getFft() {
        return fft;
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
        return fft.toFrequency(i);
    }

    public PolynomialSplineFunction getSplinePower() {
        double[] power = getPower();
        double[] freq = new double[power.length];
        for (int i=0; i<freq.length; i++)
            freq[i] = fft.toFrequency(i);

        return new SplineInterpolator().interpolate(freq, power);
    }
}
