using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using HeartRateMonitor.Math.Evaluation;

namespace HeartRateMonitor.Math
{
    public class SessionCache
    {
        private readonly Dictionary<Type, Object> Cache = new Dictionary<Type, Object>();

        public void Add<T>(IEvaluation<T> evaluation, T result)
        {
            Cache.Add(evaluation.GetType(), result);
        }

        public bool Contains<T>(IEvaluation<T> evaluation)
        {
            return Cache.ContainsKey(evaluation.GetType());
        }

        public T Get<T>(IEvaluation<T> evaluation)
        {
            return (T)Cache[evaluation.GetType()];
        }

    }
}
