package org.macondo.polar.evaluation.spectrum;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class ULFPercents implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		double ulf = training.evaluate(new ULF());
		double tp = training.evaluate(new TP());
		return (ulf / tp) * 100;
	}

}
