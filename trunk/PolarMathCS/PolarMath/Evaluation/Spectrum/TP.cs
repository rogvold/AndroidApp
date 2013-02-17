using PolarMath.Data;
using System;
using System.Collections.Generic;
using PolarMath.Util;

namespace PolarMath.Evaluation.Spectrum
{
    public class TP : IEvaluation<double>
    {
        public Double Evaluate(Training training)
        {
            //List<Periodogram> periodogram = training.evaluate(new FFT());
            var periodogram1 = training.Evaluate( new Lomb() );
            return new Square( periodogram1, 0, 0.4 ).Calculate();
        }
    }
}
