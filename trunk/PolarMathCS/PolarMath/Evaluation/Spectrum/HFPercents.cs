using PolarMath.Data;
using System;

namespace PolarMath.Evaluation.Spectrum
{
    public class HFPercents : IEvaluation<double>
    {
        public Double Evaluate(Training training)
        {
            var hf = training.Evaluate( new HF() );
            var tp = training.Evaluate( new TP() );
            return (hf / tp) * 100;
        }
    }
}
