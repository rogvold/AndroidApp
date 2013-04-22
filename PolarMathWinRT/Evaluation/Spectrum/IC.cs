using HrmMath.Data;
using System;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class IC : IEvaluation
    {
        public Index Name
        {
            get { return Index.IC; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var hf = Convert.ToDouble(training.Evaluate( new HF() ));
            var lf = Convert.ToDouble(training.Evaluate( new LF() ));
            var vlf = Convert.ToDouble(training.Evaluate( new VLF() ));

            return (lf + vlf) / hf;
        }
    }
}
