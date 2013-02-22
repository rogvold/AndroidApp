using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using HrmMath.Data;
using HrmMath.Evaluation.Statistics;

namespace HrmMath.Evaluation.HRV
{
    public sealed class CV : IEvaluation
    {
        public Index Name
        {
            get { return Index.CV; }
            set { }
        }

        public object Evaluate(SessionData training)
        {
            var sdnn = (double)training.Evaluate(new SDNN());
            var average = (double)training.Evaluate(new Average());
            return sdnn / average * 100;
        }
    }
}
