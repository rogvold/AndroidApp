using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Evaluation.Statistics
{
    public sealed class SDNN : IEvaluation
    {
        public Index Name
        {
            get { return Index.SDNN; }
            set { }
        }

        public object Evaluate(SessionData training) 
        {
            var average = (double)training.Evaluate(new Average());
            var total = training.Intervals.Aggregate<int, double>(0, (current, integer) =>
                (double)current + (average - integer) * (average - integer));
            return Math.Sqrt(total / training.Intervals.Count);
        }
    }
}
