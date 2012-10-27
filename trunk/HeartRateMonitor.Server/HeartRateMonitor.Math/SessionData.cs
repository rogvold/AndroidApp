using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using HeartRateMonitor.Math.Evaluation;

namespace HeartRateMonitor.Math
{
    public class SessionData
    {
        public List<int> Intervals { get; set; }
        public SessionCache Cache = new SessionCache();

        public T Evaluate<T>(IEvaluation<T> evaluation)
        {
            if (Cache.Contains(evaluation))
            {
                return Cache.Get(evaluation);
            }
            T evaluationResult = evaluation.Evaluate(this);
            Cache.Add(evaluation, evaluationResult);
            return evaluationResult;
        }
    }
}
