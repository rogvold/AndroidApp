using System;
using System.Collections.Generic;
using System.Linq;

namespace PolarMath.Util
{
    public class Histogram
    {
        private readonly LinkedList<HistogramInterval> _intervals = new LinkedList<HistogramInterval>();

        private const int LowBorder = 400;
        private const int HighBorder = 1300;

        private readonly int _step;
	
	    public Histogram() {
		    _step = 50;
	    }

        public Histogram(int dataSize) {
    	    var k = (int)(1 + 3.322 * Lg(dataSize));
    	    _step = (HighBorder - LowBorder) / k;
        }

        private static double Logb( double a, double b )
	    {
		    return Math.Log(a) / Math.Log(b);
	    }

	    private static double Lg( double a )
	    {
		    return Logb(a,2);
	    }
    
        public Histogram Init() {
            for (var i = LowBorder; i < HighBorder; i += _step) {
                _intervals.AddLast(i + _step < HighBorder
                                       ? new HistogramInterval(i, i + _step)
                                       : new HistogramInterval(i, HighBorder));
            }
            return this;
        }

        public void AddRrInterval(int length) {
            if (length >= LowBorder && length <= HighBorder) {
        	    GetIntervalForRr(length).Values.AddLast(length);
            }
        }
    
        public int GetMaxIntervalNumber()
        {
            return _intervals.Select(interval => interval.Values.Count).Max();
        }

        public int GetTotalCount()
        {
            return _intervals.Sum(interval => interval.Values.Count);
        }

        public int GetMaxIntervalStart() {
	        var maxValue = GetMaxIntervalNumber();
            var intervalResult = _intervals.FirstOrDefault(interval => interval.Values.Count == maxValue);
            return intervalResult == null ? 0 : intervalResult.Start;
        }

        private HistogramInterval GetIntervalForRr(int rr)
        {
            return _intervals.FirstOrDefault(interval => interval.Start <= rr && interval.End > rr);
        }

        protected LinkedList<HistogramInterval> GetIntervals() {
            return _intervals;
        }
    }
}
