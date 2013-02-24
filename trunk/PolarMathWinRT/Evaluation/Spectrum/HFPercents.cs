using HrmMath.Data;
using System;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class HFPercents : IEvaluation
    {
        public Index Name
        {
            get { return Index.HFPercents; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var hf = Convert.ToDouble(training.Evaluate(new HF()));
            var tp = Convert.ToDouble(training.Evaluate(new TP()));
            return (hf / tp) * 100;
        }
    }
}
