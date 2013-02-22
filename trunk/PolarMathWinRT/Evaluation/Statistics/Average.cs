using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Evaluation.Statistics
{
    public sealed class Average : IEvaluation
    {
        public Index Name
        {
            get { return Index.Average; }
            set { }
        }

        public object Evaluate(SessionData training)
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
