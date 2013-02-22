using HrmMath.Data;
using System;

namespace HrmMath.Evaluation.Spectrum
{
    public class HFPercents : IEvaluation<double>
    {
        public Double Evaluate(SessionData training)
        {
            var hf = training.Evaluate( new HF() );
            var tp = training.Evaluate( new TP() );
            return (hf / tp) * 100;
        }
    }
}
