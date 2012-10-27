using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace HeartRateMonitor.Math.Evaluation.Geometry
{
    public class Histogram
    {
        public List<HistogramInterval> Intervals { get; set; }

        public Histogram Init()
        {
            Intervals = new List<HistogramInterval>();
            for (int i = 300; i < 1700; i += 50)
            {
                Intervals.Add(new HistogramInterval()
                    {
                        Start = i,
                        End = i + 50,
                        Values = new List<int>()
                    });
            }
            return this;
        }

        public void AddRRInterval(int length)
        {
            GetIntervalForRR(length).Add(length);
        }

        private HistogramInterval GetIntervalForRR(int RR)
        {
            return Intervals.FirstOrDefault(interval => interval.Start <= RR && interval.End > RR);
        }

        public new String ToString() {
        var sb = new StringBuilder("Histogram\n");
        foreach (HistogramInterval interval in Intervals) {
            sb.Append(interval.ToString()).Append("\n");
        }
        return sb.ToString();
    }
    }
}
