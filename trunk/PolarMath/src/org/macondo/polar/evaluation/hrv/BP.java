package org.macondo.polar.evaluation.hrv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class BP implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		List<Integer> intervals = training.getIntervals();
		
		int maxInt = Collections.max(intervals);
		int minInt = Collections.min(intervals);		
		
		return (maxInt - minInt) / (double) 1000;
	}

}
