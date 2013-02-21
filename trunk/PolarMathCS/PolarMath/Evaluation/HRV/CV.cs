using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using PolarMath.Data;
using PolarMath.Evaluation.Statistics;

namespace PolarMath.Evaluation.HRV
{
    public class CV : IEvaluation<double>
    {
        public double Evaluate(SessionData training)
        {
            var sdnn = training.Evaluate(new SDNN());
            var average = training.Evaluate(new Average());
            return sdnn / (double)average * 100;
        }
    }
}
