using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Data
{
    public class TrainingCache
    {
        private Dictionary<Type, Object> cache = new Dictionary<Type, Object>();

        public TrainingCache() {
        }

        public bool contains<T>(Evaluation<T> evaluation) {
            return cache.ContainsKey(evaluation.GetType());
        }

        public void add<T>(Evaluation<T> evaluation, T result) {
            cache.Add(evaluation.GetType(), result);
        }

        public T get<T>(Evaluation<T> evaluation) {
            object value;
            cache.TryGetValue(evaluation.GetType(), out value);
            return (T) value;
        }
    }
}
