using HrmMath.Data;
using HrmMath.Util;
using System.Collections.Generic;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class ULF : IEvaluation
    {
        public Index Name
        {
            get { return Index.ULF; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var periodogram = (List<Periodogram>)training.Evaluate( new Lomb() );

            return new Square( periodogram, 0, 0.0033 ).Calculate();
        }
    }
}
