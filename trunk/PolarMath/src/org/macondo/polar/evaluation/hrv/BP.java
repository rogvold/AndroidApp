package org.macondo.polar.evaluation.hrv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class BP implements Evaluation<Double> {
	private static final int lowBorder = 400;
	private static final int highBorder = 1300;
	
	public Double evaluate(Training training) {
		final List<Integer> intervals = training.getIntervals();
		List<Integer> localIntervals = new ArrayList<Integer>(intervals);
		
		List<Integer> intervalsToRemove = new ArrayList<Integer>();
		for (int interval : localIntervals) {
			if (interval < lowBorder || interval > highBorder) {
				intervalsToRemove.add(interval);
			}
		}
		
		localIntervals.removeAll(intervalsToRemove);
		int maxInt = Collections.max(localIntervals);
		int minInt = Collections.min(localIntervals);		
		
		return (maxInt - minInt) / (double) 1000;
	}

}
