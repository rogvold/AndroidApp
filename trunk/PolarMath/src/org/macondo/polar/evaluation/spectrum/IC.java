package org.macondo.polar.evaluation.spectrum;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

public class IC implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		double hf = training.evaluate(new HF());
		double lf = training.evaluate(new LF());
		double vlf = training.evaluate(new LF());
		
		return (lf + vlf) / hf;
	}

}