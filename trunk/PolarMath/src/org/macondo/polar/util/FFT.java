package org.macondo.polar.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

import flanagan.complex.Complex;
import flanagan.math.FourierTransform;

/**
 * <p></p>
 *
 * Date: 17.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class FFT implements Evaluation<List<Periodogram>>{
    public static final int SAMPLING_T = 10; // Corresponding to 500 Hz. This is considered to be a good choice
    
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
    	ft.transform();
    	
    	double[][] power = ft.powerSpectrum();
    	
    	List<Periodogram> periodogram = new ArrayList<Periodogram>();
    	
    	for (int i = 0; i < ft.getUsedDataLength() / 2; i++) {
    		periodogram.add(new Periodogram(power[0][i], power[1][i]));
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
