using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using HrmMath.Util;

namespace HrmMath.Evaluation.HRV
{
    public class AMoPercents : IEvaluation<int>
    {
        public int Evaluate(SessionData training)
        {
            var intervals = training.Intervals;
            //Histogram h = new Histogram(intervals.size()).init();
            var h = new Histogram().Init();
            foreach (var interval in intervals)
            {
                h.AddRrInterval( interval );
            }
            var maxRangeValue = h.GetMaxIntervalNumber();
            var totalCount = h.GetTotalCount();

            return (int)((maxRangeValue / (double) totalCount) * 100);
        }
    }
}
