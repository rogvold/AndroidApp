package org.macondo.polar.evaluation.spectrum;

import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.util.FFT;
import org.macondo.polar.util.Periodogram;
import org.macondo.polar.util.Square;

public class ULF implements Evaluation<Double>{
	public Double evaluate(Training training) {
		List<Periodogram> periodogram = training.evaluate(new FFT());
		return new Square(periodogram, 0, 0.0033).Calculate();
	}
}
