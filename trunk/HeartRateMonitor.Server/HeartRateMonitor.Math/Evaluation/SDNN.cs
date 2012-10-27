using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation
{
    public class SDNN :IEvaluation<int>
    {
        public int Evaluate(SessionData session)
        {
            var average = session.Evaluate(new Average());
            var total = session.Intervals.Sum(interval => (average - interval)*(average - interval));
            return (int) System.Math.Sqrt(total / session.Intervals.Count);
        }
    }
}
