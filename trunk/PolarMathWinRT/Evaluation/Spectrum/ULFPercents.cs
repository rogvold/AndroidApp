using HrmMath.Data;
using System;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class ULFPercents : IEvaluation
    {
        public Index Name
        {
            get { return Index.ULFPercents; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var ulf = Convert.ToDouble(training.Evaluate( new ULF() ));
            var tp = Convert.ToDouble(training.Evaluate( new TP() ));
            return (ulf / tp) * 100;
        }
    }
}
