package com.cardiomood.android.analysis.indicators.utils;

import java.util.LinkedList;
import java.util.List;

/**
 * <p></p>
 *
 * Date: 20.05.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class Histogram {
    private List<HistogramInterval> intervals = new LinkedList<HistogramInterval>();

    private static final int lowBorder = 400;
    private static final int highBorder = 1300;

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
        return java.lang.Math.log(a) / java.lang.Math.log(b);
    }

    private static double lg( double a )
    {
        return logb(a,2);
    }

    public Histogram init() {
        for (int i = lowBorder; i < highBorder; i += step) {
            if (i + step < highBorder) {
                intervals.add(new HistogramInterval(i, i + step));
            }
            else {
                intervals.add(new HistogramInterval(i, highBorder));

            }
        }
        return this;
    }

    public void addRRInterval(Integer length) {
        if (length >= lowBorder && length <= highBorder) {
            getIntervalForRR(length).add(length);
        }
    }

    public int getMaxIntervalNumber() {
        int maxValue = 0;
        for (HistogramInterval interval : intervals) {
            if (interval.getNumber() > maxValue) {
                maxValue = interval.getNumber();
            }
        }
        return maxValue;
    }

   public int getTotalCount() {
       int total = 0;
       for (HistogramInterval interval : intervals) {
           total += interval.getNumber();
       }
       return total;
   }

   public int getMaxIntervalStart() {
       int maxValue = getMaxIntervalNumber();
       int intervalStart = 0;
       for (HistogramInterval interval : intervals) {
           if (interval.getNumber() == maxValue) {
               intervalStart = interval.getStart();
               break;
           }
       }
       return intervalStart;
   }

    private HistogramInterval getIntervalForRR(int RR) {
        for (HistogramInterval interval : intervals) {
            if (interval.getStart() <= RR && interval.getEnd() > RR) {
                return interval;
            }
        }
        return null;
    }

    protected List<HistogramInterval> getIntervals() {
        return intervals;
    }
}