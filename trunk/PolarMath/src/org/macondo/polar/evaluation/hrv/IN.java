package org.macondo.polar.evaluation.hrv;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class IN implements Evaluation<Integer> {
	
	public Integer evaluate(Training training) {
		double bp = training.evaluate(new BP());
		int amo = training.evaluate(new AMoPercents());
		double mo = training.evaluate(new Mo());
		
		return (int)(amo / (2 * bp * mo));
	}

}
