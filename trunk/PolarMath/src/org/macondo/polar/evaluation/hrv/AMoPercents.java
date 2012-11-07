package org.macondo.polar.evaluation.hrv;

import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.util.Histogram;

public class AMoPercents implements Evaluation<Integer> {
	
	public Integer evaluate(Training training) {
		List<Integer> intervals = training.getIntervals();
		Histogram h = new Histogram(intervals.size()).init();
		for (Integer interval : intervals) {
            h.addRRInterval(interval);
        }
		int maxRangeValue = h.getMaxIntervalNumber();
		int totalCount = h.getTotalCount();
		
		return (int)((maxRangeValue /  (double) totalCount) * 100);
	}

}
