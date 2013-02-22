using HrmMath.Data;
using System;

namespace HrmMath.Evaluation.Spectrum
{
    internal sealed class HFPercents : IEvaluation
    {
        public Index Name
        {
            get { return Index.HFPercents; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var hf = (double)training.Evaluate(new HF());
            var tp = (double)training.Evaluate(new TP());
            return (hf / tp) * 100;
        }
    }
}
