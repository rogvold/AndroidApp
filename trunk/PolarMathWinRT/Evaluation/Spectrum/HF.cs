using HrmMath.Data;
using HrmMath.Util;
using System.Collections.Generic;

namespace HrmMath.Evaluation.Spectrum
{
    internal sealed class HF : IEvaluation
    {
        public Index Name
        {
            get { return Index.HF; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var periodogram = (List<Periodogram>)training.Evaluate( new Lomb() );

            return new Square(periodogram, 0.1, 1).Calculate();//time period 2.5 : 6.67
        }
    }
}
