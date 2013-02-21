using PolarMath.Data;
using System;

namespace PolarMath.Evaluation.Spectrum
{
    public class VLFPercents : IEvaluation<double>
    {
        public Double Evaluate(SessionData training)
        {
            var vlf = training.Evaluate( new VLF() );
            var tp = training.Evaluate( new TP() );
            return (vlf / tp) * 100;
        }
    }
}
