using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using PolarMath.Data;

namespace PolarMath.Evaluation.HRV
{
    public class RSAI : IEvaluation<double>
    {
        public double Evaluate(SessionData training)
        {
            var intervals = training.Intervals;
            return 0d;
        }
    }
}