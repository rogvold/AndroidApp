using PolarMath.Data;
using PolarMath.Util;
using System.Collections.Generic;

namespace PolarMath.Evaluation.Spectrum
{
    public class ULF : IEvaluation<double>
    {
        public double Evaluate(Training training)
        {
            var periodogram = training.Evaluate( new Lomb() );
            return new Square( periodogram, 0, 0.0033 ).Calculate();
        }
    }
}
