package org.macondo.polar.evaluation.hrv;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class IN implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		double bp = new BP().evaluate(training);
		double amo = new AMoPercents().evaluate(training);
		double mo = new Mo().evaluate(training);
		
		return amo / (2 * bp * mo);
	}

}
