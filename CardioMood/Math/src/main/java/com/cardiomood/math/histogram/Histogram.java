package com.cardiomood.math.histogram;

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
    private double WN4;

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
        height = -1;
        for (int i=0; i<count.length; i++) {
            if (count[i] > height) {
                height = count[i];
                index = i;
            }
        }

//        int iMn = -1, iMx = -1;
//        for (int i=0; i<count.length; i++) {
//            if (count[i] * 100.0 / height < 3.0)
//                continue;
//            if (iMn < 0)
//                iMn = i;
//            iMx = i;
//        }


        Mo = index*step;
        AMo = ((double) count[index]) / ((double) values.length) * 100;
        mxDMn = getWidthAbove((int) Math.ceil(values.length*0.03)); //(iMx >= 0) ? step*(iMx - iMn + 1) : 0;
        WN4 = getWN(4);
        SI = 1e6*AMo/(2*Mo*WN4);
        mRR = StatUtils.mean(values);
        IAB = AMo/mRR;
        ARRP = AMo/Mo;
    }

    public double getA() {
        double H = 0;
        int a = (int) Math.floor(300/step);
        int b = (int) Math.floor(2200/step);
        int n = 0;
        for (int i=a; i<count.length && i<=b; i++) {
            double p = (double) count[i] / values.length;
            if (p > 1e-10)
                H -= p * Math.log(p);
            n++;
        }
        double Hm = Math.log(n);
        return 1.0 - H/Hm;
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
        if (min < 0)
            return 0;
        return step*(max-min+1);
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
        int i = (int) Math.floor(x/step);
        if (i > count.length)
            return 0;
        if (i < 0)
            return 0;
        return count[i];
    }

    public double getWN(double percent) {
        int target = (int) Math.floor(values.length*percent/100);
//        int min = 0;
//        int max = (int) Math.floor(getAMo()*values.length/100);
//        int x = 0;
//        do {
//            x = (max + min) / 2;
//            int c = getCountBelow(x);
//            if (c < target)
//                min = x + 1;
//            else if (c > target)
//                max = x;
//            else break;
//        } while (max > min);
//        x = (max + min) / 2;
        return getWidthAbove(target);
    }

    private int getCountBelow(int x) {
        int sum = 0;
        for (int c: count) {
            if (c < x)
                sum += c;
        }
        return sum;
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

    public int[] getCount() {
        return count.clone();
    }

    public double getWN4() {
        return WN4;
    }
}
