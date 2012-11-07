package org.macondo.polar.evaluation.hrv;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class IN implements Evaluation<Integer> {
	
	public Integer evaluate(Training training) {
		double bp = new BP().evaluate(training);
		int amo = new AMoPercents().evaluate(training);
		double mo = new Mo().evaluate(training);
		
		return (int)(amo / (2 * bp * mo));
	}

}
