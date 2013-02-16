using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.Statistics
{
    public class Average : Evaluation<int>
    {
        public int evaluate(Training training)
        {
            return doEvaluate( training.getIntervals() );
        }

        private int doEvaluate(List<int> intervals) {
        long intervalsTotal = 0;
        
        foreach (int interval in intervals) {
            intervalsTotal += interval;
        }
        return (int) (intervalsTotal / intervals.Count);
    }
    }
}
