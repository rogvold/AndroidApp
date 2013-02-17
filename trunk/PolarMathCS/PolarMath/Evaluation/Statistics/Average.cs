using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.Statistics
{
    public class Average : IEvaluation<int>
    {
        public int Evaluate(Training training)
        {
            return DoEvaluate( training.Intervals );
        }

        private static int DoEvaluate(ICollection<int> intervals) 
        {
            var intervalsTotal = intervals.Aggregate<int, long>(0, (current, interval) => current + interval);
            return (int) (intervalsTotal / intervals.Count);
        }
    }
}
