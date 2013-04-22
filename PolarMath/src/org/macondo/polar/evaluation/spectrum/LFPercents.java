package org.macondo.polar.evaluation.spectrum;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class LFPercents implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		double lf = training.evaluate(new LF());
		double tp = training.evaluate(new TP());
		return (lf / tp) * 100;
	}

}
