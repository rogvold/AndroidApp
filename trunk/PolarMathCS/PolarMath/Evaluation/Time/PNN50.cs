using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.Time
{
    public class PNN50 : Evaluation<int>
    {
        public int evaluate(Training training) 
        {
            List<int> intervals = training.getIntervals();
            int pnn = 0;


            for (int i = 1; i < intervals.Count; i++) {
                int now = intervals.ElementAt(i);
                int before = intervals.ElementAt(i-1);

                if (Math.Abs(now - before) >= 50) {
                    pnn++;
                }
            }

            return (int)(((double) pnn) / (intervals.Count - 1) * 100);
        }
    }
}
