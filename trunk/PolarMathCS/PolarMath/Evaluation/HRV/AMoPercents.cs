using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PolarMath.Evaluation.Util;

namespace PolarMath.Evaluation.HRV
{
    public class AMoPercents : Evaluation<int>
    {
        public int evaluate(Training training) {
		List<int> intervals = training.getIntervals();
		//Histogram h = new Histogram(intervals.size()).init();
		Histogram h = new Histogram().init();
		foreach (int interval in intervals) {
            h.addRRInterval(interval);
        }
		int maxRangeValue = h.getMaxIntervalNumber();
		int totalCount = h.getTotalCount();
		
		return (int)((maxRangeValue /  (double) totalCount) * 100);
	}
    }
}
