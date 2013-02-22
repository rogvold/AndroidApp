using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace HrmMath.Util
{
    class Square
    {
        private readonly List<Periodogram> _periodogram = new List<Periodogram>();
	
	    private readonly double _left;
	    private readonly double _right;
	
	    public Square(List<Periodogram> periodogram, double left, double right) {
		    _periodogram = periodogram;
		    _left = left;
		    _right = right;
	    }
	
	    public double Calculate(){
		    double square = 0;
            _periodogram.Sort(new FrequencyComparator());
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
		    for (int i = 0, size = _periodogram.Count; i < size; i++) {
			    if (_periodogram.ElementAt(i).Frequency >= _left && _periodogram.ElementAt(i).Frequency <= _right) {
				    square += _periodogram.ElementAt(i).Value;
			    }
		    }
		    return square;
	    }
	
	    public sealed class FrequencyComparator : IComparer<Periodogram> 
	    {
		    public int Compare(Periodogram periodogram1, Periodogram periodogram2) {
			    var freq1 = periodogram1.Frequency;
			    var freq2 = periodogram2.Frequency;
			    if(freq1 > freq2)
	                return 1;
	            if(freq1 < freq2)
	                return -1;
	            return 0;    
		    }
        }
    }
}
