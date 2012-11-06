package org.macondo.polar.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Math {
	
	private static List<Integer> ranges;
	private static List<Integer> rangeSizes;
	
	public static List<Integer> getRanges() {
		return ranges;
	}
	
	public static List<Integer> getRangeSizes() {
		return rangeSizes;
	}
	
	private static int getRangeNumber(List<Integer> ranges, int interval) {
		int i;
		for (i = 0; i < ranges.size() - 1; i++) {
			if (interval >= ranges.get(i) && interval <= ranges.get(i + 1)) {
				break;
			}
		}
		return i;
	}
	
	private static final int lowBorder = 400;
	private static final int highBorder = 1300;
	
	public static double logb( double a, double b )
	{
		return java.lang.Math.log(a) / java.lang.Math.log(b);
	}

	private static double lg( double a )
	{
		return logb(a,2);
	}
	
	public static int getTotalCount() {
		int total = 0;
		for (int rangeSize:rangeSizes) {
			total += rangeSize;
		}
		return total;
	}
	
	public static int getMaxRangeValue(List<Integer> intervals) {
		List<Integer> intervalsToRemove = new ArrayList<Integer>();
		for (int interval:intervals) {
			if (interval < lowBorder || interval > highBorder) {
				intervalsToRemove.add(interval);
			}
		}
		intervals.removeAll(intervalsToRemove);
		int maxInt = Collections.max(intervals);
		int minInt = Collections.min(intervals);
		int k = (int)(1 + 3.322 * lg(intervals.size()));
		int step = (maxInt - minInt) / k;
		
		ranges = new ArrayList<Integer>();
		
		for (int i = minInt; i < maxInt; i += step) {
			ranges.add(i);
		}
		if (ranges.get(ranges.size() - 1) < step)
			ranges.remove(ranges.size() - 1);
		ranges.add(maxInt);
		List<Integer> [] hystogram = new ArrayList[ranges.size() - 1];
		for (int i = 0; i < ranges.size() - 1; i++) {
			hystogram[i] = new ArrayList<Integer>();
		}
		for (int interval:intervals) {
			hystogram[getRangeNumber(ranges, interval)].add(interval);
		}
		rangeSizes = new ArrayList<Integer>();
		for (int i = 0; i < k; i++) {
			rangeSizes.add(hystogram[i].size());
		}
		return Collections.max(rangeSizes);
	}
	
	public static int getMaxRangeSize() {
		return getRangeSizes().indexOf(Collections.max(rangeSizes));
	}
}
