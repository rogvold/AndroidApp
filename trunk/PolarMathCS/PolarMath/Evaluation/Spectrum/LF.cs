﻿using PolarMath.Data;
using PolarMath.Util;
using System.Collections.Generic;

namespace PolarMath.Evaluation.Spectrum
{
    public class LF : IEvaluation<double>
    {
        public double Evaluate(SessionData training)
        {
            var periodogram = training.Evaluate( new Lomb() );

            return new Square( periodogram, 0.0333, 0.1 ).Calculate();
        }
    }
}
