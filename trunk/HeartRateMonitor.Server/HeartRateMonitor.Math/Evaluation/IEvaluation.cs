using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation
{
    public interface IEvaluation<out TC>
    {
        TC Evaluate(SessionData session);
    }
}
