﻿using HrmMath.Data;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Evaluation.Time
{
    public sealed class PNN50 : IEvaluation
    {
        public Index Name
        {
            get { return Index.PNN50; }
            set { }
        }

        public object Evaluate(SessionData training) 
        {
            var intervals = training.Intervals;
            var pnn = 0;


            for (var i = 1; i < intervals.Count; i++) {
                var now = intervals.ElementAt(i);
                var before = intervals.ElementAt(i-1);

                if (Math.Abs(now - before) >= 50) {
                    pnn++;
                }
            }

            return (int)(((double) pnn) / (intervals.Count - 1) * 100);
        }
    }
}