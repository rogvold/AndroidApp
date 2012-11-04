package org.macondo.polar.evaluation.geometry;

import flanagan.interpolation.CubicSpline;

import java.util.List;
import java.util.LinkedList;

/**
 * <p></p>
 *
 * Date: 21.05.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class AdvancedHistogram extends Histogram {
    private int step;
    private double[] xValues, yValues;
    private CubicSpline spline;
    private double Mo;
    private double Amo;

    public AdvancedHistogram(int dataSize) {
        this.step = (int) (1400 / Math.ceil(1 + Math.log(dataSize)) + 1);
    }

    public AdvancedHistogram init() {
        List<HistogramInterval> intervals = getIntervals();
        int startFrom = 300;

        do {
            intervals.add(new HistogramInterval(startFrom, startFrom + this.step));
            startFrom += this.step;
        } while (startFrom < 1700);

        return this;
    }

    private int getTotalRRIntervals() {
        int ct = 0;
        List<HistogramInterval> intervals = getIntervals();
        for (HistogramInterval interval : intervals) {
            ct += interval.getNumber();
        }
        return ct;
    }

    public void loadValues() {
        List<Double> xs = new LinkedList<Double>();
        List<Double> ys = new LinkedList<Double>();
        int totalIntervals = getTotalRRIntervals();
        int maxIdx = -1;
        int maxValue = -1;
        for (HistogramInterval histogramInterval : getIntervals()) {
            final List<Integer> valuesList = histogramInterval.getValues();
            if (valuesList == null || valuesList.isEmpty()) {
                continue;
            }
            final double avgX = average(valuesList);
            xs.add(avgX);
            final double yValue = ((double) valuesList.size());
            ys.add(yValue);
            if (yValue > maxValue) {
                maxValue = (int) yValue;
                maxIdx = (int) avgX;
            }
        }

        xValues = listToArrayOfPrimitives(xs);
        yValues = listToArrayOfPrimitives(ys);
        spline = new CubicSpline(xValues, yValues);
        int searchDirection = spline.interpolate(maxIdx - 1) > spline.interpolate(maxIdx + 1) ? -1 : 1;
        double currentValue = 0;
        double maxFunctionValue = spline.interpolate(maxIdx);
        int currentLocation = maxIdx + searchDirection;
        while ((currentValue = spline.interpolate(currentLocation)) > maxFunctionValue) {
            maxFunctionValue = currentValue;
            currentLocation += searchDirection;
        }
        currentLocation -= searchDirection;
        final int diagramWidth = findWidth(.02 * maxFunctionValue, currentLocation);
        System.out.println(currentLocation + ": " + maxFunctionValue);
        System.out.println(diagramWidth);
        int SI = (int) (maxFunctionValue * 1000000 / 2 / currentLocation / diagramWidth);
        System.out.println("Stress Index: " + SI);
    }

    private int findWidth(double height, int middle) {
        int left = findFirstUnder(middle, -1, height);
        int right = findFirstUnder(middle, 1, height);
        return right - left;
    }

    private int findFirstUnder(int start, int direction, double value) {
        int current = start + direction;
        while(spline.getXmax() > current && spline.getXmin() < current && spline.interpolate(current) > value) {
            current += direction;
        }
        return current;
    }

    private double average(List<Integer> values) {
        int tmp = 0;
        for (Integer value : values) {
            tmp += value;
        }
        return tmp / (double)values.size();
    }

    private double[] listToArrayOfPrimitives(List<Double> values) {
        double[] output = new double[values.size()];
        int c = 0;
        for (Double value : values) {
            output[c] = value;
            c++;
        }
        return output;
    }
}
