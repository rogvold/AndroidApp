using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation
{
    public class PNN50 : IEvaluation<double>
    {
        public double Evaluate(SessionData session)
        {
            int pnn = 0;

            for (int i = 1, count = session.Intervals.Count; i < count; i++)
            {
                int now = session.Intervals[i];
                int before = session.Intervals[i - 1];
                if (System.Math.Abs(now - before) >= 50) {
                    pnn++;
                }
            }

            return ((double) pnn) / (session.Intervals.Count - 1);
        }
    }
}