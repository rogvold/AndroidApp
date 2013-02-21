using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PolarMath.Evaluation;

namespace PolarMath.Data
{
    public class SessionData
    {
        public String IdString { get; set; }
        public List<int> Intervals { get; set; }

        public readonly SessionDataCache Cache = new SessionDataCache();

        public T Evaluate<T>(IEvaluation<T> evaluation) {
            if (Cache.Contains(evaluation)) {
                return Cache.Get(evaluation);
            } else {
                T evaluationResult = evaluation.Evaluate(this);
                Cache.Add(evaluation, evaluationResult);
                return evaluationResult;
            }
        }

        //public static Training readTraining(InputStream ins) throw IOException {
        //    return new TrainingReader().readTraining(ins);
        //}
    }
}
