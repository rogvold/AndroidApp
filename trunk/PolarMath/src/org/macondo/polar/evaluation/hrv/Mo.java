package org.macondo.polar.evaluation.hrv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.macondo.polar.util.Math;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class Mo implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		List<Integer> intervals = training.getIntervals();
		
		int maxRangeValue = Math.getMaxRangeValue(intervals);
		
		return (double)Math.getRanges().get(Math.getMaxRangeSize()) / (double) 1000;
	}

}
