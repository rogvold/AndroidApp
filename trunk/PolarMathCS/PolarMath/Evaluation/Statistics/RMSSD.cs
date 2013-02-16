using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.Statistics
{
    public class RMSSD : Evaluation<int>
    {
        public int evaluate(Training training) 
        {
            List<int> intervals = training.getIntervals();
            long total = 0;

            for (int i = 1; i < intervals.Count; i++)
            {
                int now = intervals.ElementAt( i );
                int before = intervals.ElementAt( i - 1 );

                total += (now - before) * (now - before);
            }

            return (int) Math.Sqrt( total / intervals.Count );
        }
    }
}
