using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Evaluation.Statistics
{
    public class SDNN : IEvaluation<int>
    {
        public int Evaluate(SessionData training) 
        {
            var average = training.Evaluate(new Average());
            var total = training.Intervals.Aggregate<int, long>(0, (current, integer) =>
                current + (average - integer) * (average - integer));
            return (int)Math.Sqrt(total / training.Intervals.Count);
        }
    }
}
