using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using HrmMath.Evaluation;

namespace HrmMath.Data
{
    public sealed class SessionDataCache
    {
        private readonly Dictionary<Index, object> _cache = new Dictionary<Index, object>();

        internal bool Contains(IEvaluation evaluation) {
            return _cache.ContainsKey(evaluation.Name);
        }

        internal void Add(IEvaluation evaluation, object value)
        {
            _cache.Add(evaluation.Name, value);
        }

        internal object Get(IEvaluation evaluation)
        {
            object result;
            _cache.TryGetValue(evaluation.Name, out result);
            return result;
        }
    }
}
