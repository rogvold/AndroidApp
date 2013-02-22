using HrmMath.Data;
using HrmMath.Util;
using System.Collections.Generic;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class LF : IEvaluation
    {
        public Index Name
        {
            get { return Index.LF; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var periodogram = (List<Periodogram>)training.Evaluate( new Lomb() );

            return new Square( periodogram, 0.0333, 0.1 ).Calculate();
        }
    }
}
