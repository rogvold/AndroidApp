package org.macondo.polar.evaluation.hrv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.util.Math;

public class AMoPercents implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		List<Integer> intervals = training.getIntervals();
		
		int maxRangeValue = Math.getMaxRangeValue(intervals);
		int totalCount = Math.getTotalCount();
		
		return (maxRangeValue / (double) totalCount) * 100;
	}

}
