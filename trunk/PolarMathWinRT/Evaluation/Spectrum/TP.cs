using HrmMath.Data;
using System;
using System.Collections.Generic;
using HrmMath.Util;

namespace HrmMath.Evaluation.Spectrum
{
    public sealed class TP : IEvaluation
    {
        public Index Name
        {
            get { return Index.TP; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            //List<Periodogram> periodogram = training.evaluate(new FFT());
            var periodogram1 = (List<Periodogram>)training.Evaluate( new Lomb() );
            return new Square( periodogram1, 0, 0.4 ).Calculate();
        }
    }
}
