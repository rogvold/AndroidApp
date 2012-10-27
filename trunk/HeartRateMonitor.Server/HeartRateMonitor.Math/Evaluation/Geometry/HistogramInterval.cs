using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation.Geometry
{
    public class HistogramInterval
    {
        public int Start { get; set; }
        public int End { get; set; }

        [DefaultValue(0)]
        public int Number { get; set; }

        public List<int> Values { get; set; }


        public void Add(int interval)
        {
            Number++;
            Values.Add(interval);
        }

        public new String ToString()
        {
            return new StringBuilder("(")
                .Append(Start)
                .Append(", ")
                .Append(End)
                .Append(") = ")
                .Append(Number).ToString();
        }
    }
}