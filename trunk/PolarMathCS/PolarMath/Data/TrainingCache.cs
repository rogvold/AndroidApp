using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PolarMath.Evaluation;

namespace PolarMath.Data
{
    public class TrainingCache
    {
        private readonly Dictionary<Type, Object> _cache = new Dictionary<Type, Object>();

        public bool Contains<T>(IEvaluation<T> evaluation) {
            return _cache.ContainsKey(evaluation.GetType());
        }

        public void Add<T>(IEvaluation<T> evaluation, T result) {
            _cache.Add(evaluation.GetType(), result);
        }

        public T Get<T>(IEvaluation<T> evaluation) {
            object value;
            _cache.TryGetValue(evaluation.GetType(), out value);
            return (T) value;
        }
    }
}
