using HrmMath.Data;
using System;
using System.Collections.Generic;
using HrmMath.Util;

namespace HrmMath.Evaluation.Spectrum
{
    public class TP : IEvaluation<double>
    {
        public Double Evaluate(SessionData training)
        {
            //List<Periodogram> periodogram = training.evaluate(new FFT());
            var periodogram1 = training.Evaluate( new Lomb() );
            return new Square( periodogram1, 0, 0.4 ).Calculate();
        }
    }
}
