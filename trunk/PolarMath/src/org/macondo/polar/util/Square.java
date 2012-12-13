package org.macondo.polar.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Square {
	private List<Periodogram> periodogram = new ArrayList<Periodogram>();
	
	private double left;
	private double right;
	
	public Square(List<Periodogram> periodogram, double left, double right) {
		this.periodogram = periodogram;
		this.left = left;
		this.right = right;
	}
	
	public double Calculate(){
		double square = 0;
		Collections.sort(this.periodogram, Square.FrequencyComparator);
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
		for (int i = 0, size = periodogram.size(); i < size; i++) {
			if (periodogram.get(i).getFrequency() >= left && periodogram.get(i).getFrequency() <= right) {
				square += periodogram.get(i).getValue();
			}
		}
		return square;
	}
	
	public static Comparator<Periodogram> FrequencyComparator 
	= new Comparator<Periodogram>() {

		public int compare(Periodogram periodogram1, Periodogram periodogram2) {
			double freq1 = periodogram1.getFrequency();
			double freq2 = periodogram2.getFrequency();
			if(freq1 > freq2)
	            return 1;
	        else if(freq1 < freq2)
	            return -1;
	        else
	            return 0;    
		}

};
}


