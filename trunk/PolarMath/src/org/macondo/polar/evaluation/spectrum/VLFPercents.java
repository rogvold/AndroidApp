package org.macondo.polar.evaluation.spectrum;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class VLFPercents implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		double vlf = training.evaluate(new VLF());
		double tp = training.evaluate(new TP());
		return (vlf / tp) * 100;
	}

}
