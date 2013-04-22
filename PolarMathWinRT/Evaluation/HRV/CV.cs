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
            var sdnn = Convert.ToDouble(training.Evaluate(new SDNN()));
            var average = Convert.ToDouble(training.Evaluate(new Average()));
            return sdnn / average * 100;
        }
    }
}
