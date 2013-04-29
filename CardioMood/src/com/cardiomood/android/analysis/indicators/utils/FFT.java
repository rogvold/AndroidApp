package com.cardiomood.android.analysis.indicators.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


import flanagan.complex.Complex;
import flanagan.math.FourierTransform;

/**
 * <p>
 * </p>
 *
 * Date: 17.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class FFT implements Evaluation<List<Periodogram>> {
    public static final int SAMPLING_T = 10; // Corresponding to 500 Hz. This
                                                // is considered to be a good
                                                // choice
    public static final double SAMPLING_T_ABS = 0.01; // Corresponding to 500
                                                        // Hz. This is
                                                        // considered to be a
                                                        // good choice

    private List<Integer> intervals = new ArrayList<Integer>();

    public List<Periodogram> evaluate(Training training) {
        this.intervals = training.getIntervals();

        int total = 0;
        int endIndex = this.intervals.size();
        for (int i = 0; i < this.intervals.size(); i++) {
            if (total > 300000) {
                endIndex = i;
                break;
            }
            total += intervals.get(i);
        }
        for (int i = this.intervals.size() - 1; i >= endIndex; i--) {
            this.intervals.remove(i);
        }
        int avg = total / this.intervals.size();

        Complex[] data = padZeroes(resample(this.intervals, avg));

        FourierTransform ft = new FourierTransform(data);
        ft.setBartlett();
        //ft.setDeltaT(1.25);
        ft.transform();

        double[][] power = ft.powerSpectrum();
        //Complex[] result = ft.getTransformedDataAsComplex();
        //ft.printPowerSpectrum("power.txt");

        List<Periodogram> periodogram = new ArrayList<Periodogram>();

        for (int i = 0; i < ft.getUsedDataLength() / 2; i++) {
            periodogram.add(new Periodogram(power[0][i], (1 / (double) SAMPLING_T) * power[1][i]));
        }

        return periodogram;
    }

    Complex[] padZeroes(List<Double> numbers) {
        int length = 1 << Functions.findLength(numbers.size() - 1);
        Complex[] result = new Complex[length];
        for (int i = 0; i < length; i++) {
            if (i < numbers.size()) {
                result[i] = new Complex(numbers.get(i), 0);
            } else {
                result[i] = new Complex(0, 0);
            }
        }
        Functions.reverse(result);
        return result;
    }

    List<Double> resample(List<Integer> initial, int avg) {
        List<Double> resampled = new LinkedList<Double>();
        ListIterator<Integer> li = initial.listIterator();
        Integer previous = li.next();
        int[] delta = new int[1];
        while (li.hasNext()) {
            Integer current = li.next();
            resampled.addAll(getSamples(previous - avg, current - avg, current,
                    delta));
            previous = current;
        }

        return resampled;
    }

    /*List<Double> resample(List<Integer> initial, int avg) {
        List<Double> resampled = new LinkedList<Double>();

        double[] intData = new double[initial.size() + 1];
        double[] timeData = new double[initial.size() + 1];
        double time = 0;
        intData[0] = avg / (double) 1000;
        timeData[0] = time;
        for (int i = 1; i < initial.size() + 1; i++) {
            time += initial.get(i - 1) / (double) 1000;
            intData[i] = initial.get(i - 1) / (double) 1000;
            timeData[i] = time;
        }

        CubicSpline cs = new CubicSpline(timeData, intData);

        double step = 0;
        while (step <= time) {
            resampled.add(cs.interpolate(step));
            step += SAMPLING_T_ABS;
        }

        return resampled;
    }*/

    List<Double> getSamples(Integer startValue, Integer endValue,
            Integer intervalLength, int[] delta) {
        int interval = delta[0];
        double slope = (endValue.doubleValue() - startValue.doubleValue())
                / intervalLength.doubleValue();
        List<Double> samples = new LinkedList<Double>();
        while (interval <= intervalLength) {
            samples.add(startValue.doubleValue() + slope * interval);
            interval += SAMPLING_T;
        }
        delta[0] = interval - intervalLength;
        return samples;
    }
}