﻿using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using HrmMath.Util;

namespace HrmMath.Evaluation.HRV
{
    public class Mo : IEvaluation<double>
    {
        public double Evaluate(SessionData training) 
        {
		    var intervals = training.Intervals;
		
		    //Histogram h = new Histogram(intervals.size()).init();
		    var h = new Histogram().Init();
		    foreach (var interval in intervals) {
                h.AddRrInterval(interval);
            }
		    return h.GetMaxIntervalStart() / (double) 1000;
	    }
    }
}
