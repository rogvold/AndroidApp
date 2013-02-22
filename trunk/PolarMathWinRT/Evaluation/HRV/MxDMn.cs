using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Evaluation.HRV
{
    public sealed class MxDMn : IEvaluation
    {
        private const int LowBorder = 400;
        private const int HighBorder = 1300;

        public Index Name
        {
            get { return Index.MxDMn; }
            set { }
        }

        public object Evaluate(SessionData training) {
		    var intervals = training.Intervals;
		    var localIntervals = intervals.Where(interval => interval >= LowBorder && interval <= HighBorder).ToList();


		    var maxInt = localIntervals.Max();
		    var minInt = localIntervals.Min();
		
		    return (maxInt - minInt) / (double) 1000;
	    }
    }
}
