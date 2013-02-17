using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PolarMath.Evaluation;

namespace PolarMath.Data
{
    public class Training
    {
        public String IdString { get; set; }
        public List<int> Intervals { get; set; }

        private readonly TrainingCache _cache = new TrainingCache();

        public T Evaluate<T>(IEvaluation<T> evaluation) {
            if (_cache.Contains(evaluation)) {
                return _cache.Get(evaluation);
            } else {
                T evaluationResult = evaluation.Evaluate(this);
                _cache.Add(evaluation, evaluationResult);
                return evaluationResult;
            }
        }

        //public static Training readTraining(InputStream ins) throw IOException {
        //    return new TrainingReader().readTraining(ins);
        //}
    }
}
