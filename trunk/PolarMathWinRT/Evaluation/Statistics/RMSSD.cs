using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Evaluation.Statistics
{
    internal sealed class RMSSD : IEvaluation
    {
        public Index Name
        {
            get { return Index.RMSSD; }
            set { }
        }

        public object Evaluate(SessionData training) 
        {
            var intervals = training.Intervals;
            long total = 0;

            for (var i = 1; i < intervals.Count; i++)
            {
                var now = intervals.ElementAt( i );
                var before = intervals.ElementAt( i - 1 );

                total += (now - before) * (now - before);
            }

            return (int) Math.Sqrt( total / (double)intervals.Count );
        }
    }
}
