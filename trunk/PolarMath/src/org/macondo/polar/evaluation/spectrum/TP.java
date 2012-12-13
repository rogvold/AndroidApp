package org.macondo.polar.evaluation.spectrum;

import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.util.FFT;
import org.macondo.polar.util.Lomb;
import org.macondo.polar.util.Periodogram;
import org.macondo.polar.util.Square;

public class TP implements Evaluation<Double> {
	
	public Double evaluate(Training training) {
		//List<Periodogram> periodogram = training.evaluate(new FFT());
		List<Periodogram> periodogram1 = training.evaluate(new Lomb());
		return new Square(periodogram1, 0, 0.4).Calculate();
	}

}
