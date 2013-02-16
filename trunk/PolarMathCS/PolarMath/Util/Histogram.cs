using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Evaluation.Util
{
    public class Histogram
    {
        private LinkedList<HistogramInterval> intervals = new LinkedList<HistogramInterval>();
    
        private static int lowBorder = 400;
	    private static int highBorder = 1300;
	
	    private int step;
	
	    public Histogram() {
		    this.step = 50;
	    }

        public Histogram(int dataSize) {
    	    int k = (int)(1 + 3.322 * lg(dataSize));
    	    this.step = (int)(highBorder - lowBorder) / k;
        }

        private static double logb( double a, double b )
	    {
		    return Math.Log(a) / Math.Log(b);
	    }

	    private static double lg( double a )
	    {
		    return logb(a,2);
	    }
    
        public Histogram init() {
            for (int i = lowBorder; i < highBorder; i += step) {
        	    if (i + step < highBorder) {
        		    intervals.AddLast(new HistogramInterval(i, i + step));
        	    }
        	    else {
                    intervals.AddLast( new HistogramInterval( i, highBorder ) );
            	
        	    }
            }
            return this;
        }

        public void addRRInterval(int length) {
            if (length >= lowBorder && length <= highBorder) {
        	    getIntervalForRR(length).add(length);
            }
        }
    
        public int getMaxIntervalNumber() {
    	    int maxValue = 0;
    	    foreach (HistogramInterval interval in intervals) {
    		    if (interval.getNumber() > maxValue) {
    			    maxValue = interval.getNumber();
    		    }
    	    }
    	    return maxValue;
        }
    
       public int getTotalCount() {
	       int total = 0;
	       foreach (HistogramInterval interval in intervals) {
		       total += interval.getNumber();
	       }
	       return total;
       }
   
       public int getMaxIntervalStart() {
	       int maxValue = getMaxIntervalNumber();
	       int intervalStart = 0;
	       foreach (HistogramInterval interval in intervals) {
		       if (interval.getNumber() == maxValue) {
			       intervalStart = interval.getStart();
			       break;
		       }
	       }
	       return intervalStart;
       }

        private HistogramInterval getIntervalForRR(int RR) {
            foreach (HistogramInterval interval in intervals) {
                if (interval.getStart() <= RR && interval.getEnd() > RR) {
                    return interval;
                }
            }
            return null;
        }

        protected LinkedList<HistogramInterval> getIntervals() {
            return intervals;
        }
    }
}
