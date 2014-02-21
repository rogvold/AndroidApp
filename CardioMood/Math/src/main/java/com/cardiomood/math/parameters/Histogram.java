package com.cardiomood.math.parameters;

import org.apache.commons.math3.stat.StatUtils;

import java.util.Arrays;

/**
 * Created by danon on 18.02.14.
 */
public class Histogram {

    protected final double[] values;
    protected final int count[];
    private final double step;
    private double Mo;
    private double AMo;
    private double mxDMn;
    private double SI;
    private double mRR;
    private double IAB;
    private double ARRP; // ПАПР - Adequacy ratio of regulatory processes.
    private int height;

    public Histogram(double values[], double step) {
        this.values = new double[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
        this.step = step;
        this.count = new int[(int)(Math.floor(StatUtils.max(values)/step)) + 1];
        Arrays.fill(this.count, 0);
        init();
    }

    private void init() {
        for (double value: values) {
            int i = (int) Math.floor(value / step);
            this.count[i]++;
        }

        int index = 0;
        int height = -1;
        for (int i=0; i<count.length; i++) {
            if (count[i] > height) {
                height = count[i];
                index = i;
            }
        }

        Mo = index*step;
        AMo = ((double) count[index]) / ((double) values.length) * 100;
        mxDMn = step*(Math.ceil(StatUtils.max(values) / step) -  Math.floor(StatUtils.min(values)/step));
        SI = 1e6*AMo/(2*Mo*mxDMn);
        mRR = StatUtils.mean(values);
        IAB = AMo/mRR;
        ARRP = AMo/Mo;
    }

    public double getWidthAbove(int x) {
        int min = -1;
        int max = -1;
        for (int i=0; i<count.length; i++) {
            if (count[i] < x)
                continue;
            if (min < 0)
                min = i;
            max = i;
        }
        return step*(max-min);
    }
      
    public int getCountFor(double min, double max) {
        final int a = (int) Math.floor(min/step);
        final int b = (int) Math.floor(max/step);
        int s = 0;
        for (int i=a; i<=b; i++)
            s += count[i];
        return s;
    }

    public int getCountFor(double x) {
        return count[(int) Math.floor(x/step)];
    }

    public double getStep() {
        return step;
    }

    public double getMo() {
        return Mo;
    }

    public double getAMo() {
        return AMo;
    }

    public double getMxDMn() {
        return mxDMn;
    }

    public double getSI() {
        return SI;
    }

    public double getmRR() {
        return mRR;
    }

    public double getIAB() {
        return IAB;
    }

    public double getARRP() {
        return ARRP;
    }

    public int getHeight() {
        return height;
    }
}
