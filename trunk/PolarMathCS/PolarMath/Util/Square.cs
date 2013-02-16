using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PolarMath.Util
{
    class Square
    {
        private List<Periodogram> periodogram = new List<Periodogram>();
	
	    private double left;
	    private double right;
	
	    public Square(List<Periodogram> periodogram, double left, double right) {
		    this.periodogram = periodogram;
		    this.left = left;
		    this.right = right;
	    }
	
	    public double Calculate(){
		    double square = 0;
            this.periodogram.Sort(new Square.FrequencyComparator());
		    /*for (int i = 0; i < this.periodogram.size() - 1; i++) {
			    double value1 = this.periodogram.get(i).getValue();
			    double value2 = this.periodogram.get(i + 1).getValue();
			    double freq1 = this.periodogram.get(i).getFrequency();
			    double freq2 = this.periodogram.get(i + 1).getFrequency();
			    if (freq1 > this.right) {
				    break;
			    }
			    if (freq2 < this.left) {
				    continue;
			    }
			    if (freq1 < this.left) {
				    value1 = ((this.left - freq1) / (freq2 - freq1)) * (value2 - value1) + value1;
				    freq1 = this.left;
			    }
			    if (freq2 > this.right) {
				    value2 = ((this.right - freq1) / (freq2 - freq1)) * (value2 - value1) + value1;
				    freq2 = this.right;
			    }
			    double avgValue = (value1 + value2) / 2;
			    double freq = freq2 - freq1;
			    square += freq * avgValue * 1000000;
		    }*/
		    for (int i = 0, size = periodogram.Count; i < size; i++) {
			    if (periodogram.ElementAt(i).getFrequency() >= left && periodogram.ElementAt(i).getFrequency() <= right) {
				    square += periodogram.ElementAt(i).getValue();
			    }
		    }
		    return square;
	    }
	
	    public sealed class FrequencyComparator : IComparer<Periodogram> 
	    {
		    public int Compare(Periodogram periodogram1, Periodogram periodogram2) {
			    double freq1 = periodogram1.getFrequency();
			    double freq2 = periodogram2.getFrequency();
			    if(freq1 > freq2)
	                return 1;
	            else if(freq1 < freq2)
	                return -1;
	            else
	                return 0;    
		    }
        }
    }
}
