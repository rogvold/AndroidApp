using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using PolarMath.Data;

namespace PolarMath.Evaluation.HRV
{
    class RSAI : IEvaluation<double>
    {
        public double Evaluate(Training training)
        {
            var intervals = training.Intervals;
        }
    }
}