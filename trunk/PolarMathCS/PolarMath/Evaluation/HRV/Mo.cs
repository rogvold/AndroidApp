using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PolarMath.Evaluation.Util;

namespace PolarMath.Evaluation.HRV
{
    public class Mo : Evaluation<double>
    {
        public double evaluate(Training training) 
        {
		    List<int> intervals = training.getIntervals();
		
		    //Histogram h = new Histogram(intervals.size()).init();
		    Histogram h = new Histogram().init();
		    foreach (int interval in intervals) {
                h.addRRInterval(interval);
            }
		    return h.getMaxIntervalStart() / (double) 1000;
	    }
    }
}
