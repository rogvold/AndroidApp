using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation
{
    public class CV : IEvaluation<int>
    {
        public int Evaluate(SessionData session)
        {
            return 100 * session.Evaluate(new SDNN()) / session.Evaluate(new Average());
        }
    }
}