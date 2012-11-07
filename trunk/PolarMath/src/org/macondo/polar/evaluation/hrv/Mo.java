package org.macondo.polar.evaluation.hrv;

import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.util.Histogram;

public class Mo implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		List<Integer> intervals = training.getIntervals();
		
		Histogram h = new Histogram(intervals.size()).init();
		for (Integer interval : intervals) {
            h.addRRInterval(interval);
        }
		return h.getMaxIntervalStart() / (double) 1000;
	}

}
