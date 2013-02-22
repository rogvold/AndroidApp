using HrmMath.Data;
using System;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class VLFPercents : IEvaluation
    {
        public Index Name
        {
            get { return Index.VLF; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var vlf = (double)training.Evaluate( new VLF() );
            var tp = (double)training.Evaluate(new TP());
            return (vlf / tp) * 100;
        }
    }
}
