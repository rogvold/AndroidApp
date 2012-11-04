package org.macondo.polar.evaluation;

import org.macondo.polar.data.Training;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * <p></p>
 *
 * Date: 17.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class FFT implements Evaluation<List<Harmonics>>{
    public static final int SAMPLING_T = 10; // Corresponding to 500 Hz. This is considered to be a good choice
    private static final double SAMPLING_T_ABS = .01; // Corresponding to 500 Hz. This is considered to be a good choice
    private static final double PI_2 = 2 * Math.PI;

    public List<Harmonics> evaluate(Training training) {
        int avg = training.evaluate(new Average());
        Complex[] data = padZeroes(resample(training.getIntervals(), avg));

        if ((data.length & 1) == 1) {
            throw new IllegalArgumentException("Only arrays of 2^N elements allowed");
        }

        for (int N = 2, Nd2 = 1; N < data.length + 1; Nd2 = N, N <<= 1) {
            for (int k = 0; k < Nd2; k++) {
                Complex W = Complex.EXP(-PI_2 * k / N * SAMPLING_T_ABS);
                for (int m = k; m < data.length; m += N) {
                    int pairIdx = m + Nd2;
                    Complex tmp = W.times(data[pairIdx]);
                    data[pairIdx] = data[m].sub(tmp);
                    data[m] = data[m].add(tmp);
                }
            }
        }

        List<Harmonics> harmonics = new LinkedList<Harmonics>();
        for (int i = 0; i < data.length; i++) {
            Complex complex = data[i];
            harmonics.add(Harmonics.valueOf(i, data.length, complex));
        }

        return harmonics;
    }

    void applyHanningWindow(Complex[] data) {
        for (int i = 0; i < data.length; i++) {
            Complex complex = data[i];
            data[i] = complex.times(Complex.valueOf(.5 * (1 - Math.cos(PI_2 * i / (data.length - 1)))));
        }
    }

    Complex[] padZeroes(List<Double> numbers) {
        int length = 1 << Functions.findLength(numbers.size() - 1);
        Complex[] result = new Complex[length];
        for (int i = 0; i < length; i++) {
            if (i < numbers.size()) {
                result[i] = Complex.valueOf(numbers.get(i));
            } else {
                result[i] = Complex.ZERO;
            }
        }
        Functions.reverse(result);
        return result;
    }

    List<Double> resample(List<Integer> initial, int avg)  {
        List<Double> resampled = new LinkedList<Double>();
        ListIterator<Integer> li = initial.listIterator();
        Integer previous = li.next();
        int[] delta = new int[1];
        while (li.hasNext()) {
            Integer current = li.next();
            resampled.addAll(getSamples(previous - avg, current - avg, current, delta));
            previous = current;
        }
        return resampled;
    }

    List<Double> getSamples(Integer startValue, Integer endValue, Integer intervalLength, int[] delta) {
        int interval = delta[0];
        double slope = (endValue.doubleValue() - startValue.doubleValue()) / intervalLength.doubleValue();
        List<Double> samples = new LinkedList<Double>();
        while (interval <= intervalLength) {
            samples.add(startValue.doubleValue() + slope * interval);
            interval += SAMPLING_T;
        }
        delta[0] = interval - intervalLength;
        return samples;
    }
}
