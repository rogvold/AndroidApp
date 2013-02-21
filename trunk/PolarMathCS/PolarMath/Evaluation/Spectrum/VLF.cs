using PolarMath.Data;
using PolarMath.Util;
using System.Collections.Generic;

namespace PolarMath.Evaluation.Spectrum
{
    public class VLF : IEvaluation<double>
    {
        public double Evaluate(SessionData training)
        {
            var periodogram = training.Evaluate( new Lomb() );

            return new Square( periodogram, 0.0033, 0.04 ).Calculate();
        }
    }
}
