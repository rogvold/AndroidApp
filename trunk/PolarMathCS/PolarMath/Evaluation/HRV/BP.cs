using PolarMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.HRV
{
    public class BP : Evaluation<double>
    {
        private static int lowBorder = 400;
	    private static int highBorder = 1300;
	
	    public Double evaluate(Training training) {
		    List<int> intervals = training.getIntervals();
		    List<int> localIntervals = new List<int>(intervals);
		
		    List<int> intervalsToRemove = new List<int>();
		    foreach (int interval in localIntervals) {
			    if (interval < lowBorder || interval > highBorder) {
				    intervalsToRemove.Add(interval);
			    }
		    }
		
		    localIntervals.Except(intervalsToRemove);
		    int maxInt = localIntervals.Max();
		    int minInt = localIntervals.Min();
		
		    return (maxInt - minInt) / (double) 1000;
	    }
    }
}
