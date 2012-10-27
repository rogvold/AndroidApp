using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation
{
    public class Average : IEvaluation<int>
    {
        public int Evaluate(SessionData session)
        {
            var intervalsTotal = session.Intervals.Aggregate<int, long>(0, (current, interval) => current + interval);

            return (int) (intervalsTotal / session.Intervals.Count);
        }
    }
}

