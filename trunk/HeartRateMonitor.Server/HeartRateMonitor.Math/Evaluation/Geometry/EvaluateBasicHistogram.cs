using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation.Geometry
{
    public class EvaluateBasicHistogram : IEvaluation<Histogram>
    {
        public Histogram Evaluate(SessionData session)
        {
            var h = new Histogram().Init();
            foreach (int interval in session.Intervals) {
                h.AddRRInterval(interval);
            }
            return h;
        }
    }
}
