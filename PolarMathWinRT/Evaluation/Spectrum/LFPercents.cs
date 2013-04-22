using HrmMath.Data;
using System;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class LFPercents : IEvaluation
    {
        public Index Name
        {
            get { return Index.LFPercents; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var lf = Convert.ToDouble(training.Evaluate( new LF() ));
            var tp = Convert.ToDouble(training.Evaluate( new TP() ));
            return (lf / tp) * 100;
        }
    }
}
