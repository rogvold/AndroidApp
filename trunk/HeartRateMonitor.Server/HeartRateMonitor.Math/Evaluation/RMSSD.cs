using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation
{
    public class RMSSD : IEvaluation<int>
    {
        public int Evaluate(SessionData session)
        {
            long total = 0;

            for (int i = 1, count = session.Intervals.Count; i < count; i++) {
                var now = session.Intervals[i];
                var before = session.Intervals[i - 1];
                total += (now - before) * (now - before);
            }

            return (int) System.Math.Sqrt(total / (session.Intervals.Count - 1));
        }
    }


}

