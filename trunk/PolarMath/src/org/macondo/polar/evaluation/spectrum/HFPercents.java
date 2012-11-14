package org.macondo.polar.evaluation.spectrum;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class HFPercents implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		double hf = training.evaluate(new HF());
		double tp = training.evaluate(new TP());
		return (hf / tp) * 100;
	}

}
