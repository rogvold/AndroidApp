using HrmMath.Data;
using System;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class VLFPercents : IEvaluation
    {
        public Index Name
        {
            get { return Index.VLFPercents; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var vlf = Convert.ToDouble(training.Evaluate( new VLF() ));
            var tp = Convert.ToDouble(training.Evaluate(new TP()));
            return (vlf / tp) * 100;
        }
    }
}
