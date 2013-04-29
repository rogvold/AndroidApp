package com.cardiomood.android.analysis.indicators.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import flanagan.analysis.Stat;

public class Lomb implements Evaluation<List<Periodogram>> {

    private int size;

    public List<Periodogram> evaluate(Training training) {
        List<Integer> intervals = training.getIntervals();

        size = intervals.size();

        List<Double> freqValues = new ArrayList<Double>();
        List<Double> intervs = new ArrayList<Double>();
        List<Double> timeValues = new ArrayList<Double>();

        double time = 0;
        //int endIndex = size;
        for (int i = 0; i < size; i++) {
            /*if (time > 300) {
                endIndex = i;
                break;
            }*/
            time += intervals.get(i) / (double) 1000;
            timeValues.add(time);
            intervs.add(intervals.get(i) / (double) 1000);
        }

        double maxFreq = 0.5 / (Collections.min(intervals) / (double) 1000);

        //size = endIndex;

        List<String> str = new ArrayList<String>();

        double[] intervalValues = new double[size];
        for (int i = 0; i < size; i++) {
            intervalValues[i] = intervs.get(i);
            str.add(timeValues.get(i) + " " + intervalValues[i]);
        }

        int freqSize = 0;
        double freqTotal = 0;

        while (freqTotal <= maxFreq) {
            freqValues.add(freqSize / (double) 1024);
            freqSize++;
            freqTotal += 1 / (double) 1024;
        }

        double var = Stat.standardDeviation(intervalValues);
        intervalValues = Stat.subtractMean(intervalValues);

        List<Periodogram> periodogram = new ArrayList<Periodogram>();

        for (int i = 0; i< freqSize; i++) {
            double w = 2 * Math.PI * freqValues.get(i);

            if (w > 0) {
                double sinSum = 0;
                double cosSum = 0;

                for (int j = 0; j < size; j++) {
                    sinSum += Math.sin(2 * w * timeValues.get(j));
                    cosSum += Math.cos(2 * w * timeValues.get(j));
                }

                double tau = Math.atan(sinSum / cosSum) / 2 / w;

                double sinHigh = 0;
                double sinLow = 0;
                double cosHigh = 0;
                double cosLow = 0;

                for (int j = 0; j < size; j++) {
                    sinHigh += intervalValues[j] * Math.sin(w * (timeValues.get(j) - tau));
                    cosHigh += intervalValues[j] * Math.cos(w * (timeValues.get(j) - tau));
                    cosLow += Math.pow(Math.cos(w * (timeValues.get(j) - tau)), 2);
                    sinLow += Math.pow(Math.sin(w * (timeValues.get(j) - tau)), 2);
                }
                periodogram.add(new Periodogram(freqValues.get(i), (1 / (2 * var * var) *
                        (sinHigh * sinHigh / sinLow + cosHigh * cosHigh / cosLow))));
            }
        }

        double tp = 0;

        for (int i = 0; i < freqSize - 1; i++) {
            tp += (1 / (double) 1024) * periodogram.get(i).getValue();
        }

        return periodogram;
    }
}
