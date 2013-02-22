using HrmMath.Data;
using HrmMath.Util;
using System.Collections.Generic;

namespace HrmMath.Evaluation.Spectrum
{
    public class HF : IEvaluation<double>
    {
        public double Evaluate(SessionData training)
        {
            var periodogram = training.Evaluate( new Lomb() );

            return new Square(periodogram, 0.1, 1).Calculate();//time period 2.5 : 6.67
        }
    }
}
