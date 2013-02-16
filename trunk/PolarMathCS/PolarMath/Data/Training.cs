using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Data
{
    public class Training
    {
        private String idString;
        private List<int> intervals;

        private TrainingCache cache = new TrainingCache();

        public Training() {
        }

        Training(String idString, List<int> intervals) {
            this.idString = idString;
            this.intervals = intervals;
        }

        public String getIdString() {
            return idString;
        }

        public void setIdString(String idString) {
            this.idString = idString;
        }

        public List<int> getIntervals() {
            return intervals;
        }

        public void setIntervals(List<int> intervals) {
            this.intervals = intervals;
        }

        public T evaluate<T>(Evaluation<T> evaluation) {
            if (cache.contains(evaluation)) {
                return cache.get(evaluation);
            } else {
                T evaluationResult = evaluation.evaluate(this);
                cache.add(evaluation, evaluationResult);
                return evaluationResult;
            }
        }

        //public static Training readTraining(InputStream ins) throw IOException {
        //    return new TrainingReader().readTraining(ins);
        //}
    }
}
