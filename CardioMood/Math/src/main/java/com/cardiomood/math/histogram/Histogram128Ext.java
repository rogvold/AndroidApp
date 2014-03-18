package com.cardiomood.math.histogram;

/**
 * Created by danon on 19.02.14.
 */
public class Histogram128Ext extends Histogram {

    private static final double STEP = 1000.0/128.0;

    private double HRVTi;
    private double WN1;
    private double WN5;
    private  double WN10;


    public Histogram128Ext(double[] values) {
        super(values, STEP);
        init();
    }



    private void init() {
        int height = getHeight();
        HRVTi = ((double) values.length) / ((double) height/STEP);
        WN1 = getWN(1.0);
        WN5 = getWN(5.0);
        WN10 = getWN(10.0);
    }

    public double getWN(double percent) {
        int target = (int) Math.floor(values.length*percent/100);
        int min = 0;
        int max = (int) Math.floor(getAMo()*values.length/100);
        int x = 0;
        do {
            x = (max + min) / 2;
            int c = getCountBelow(x);
            if (c < target)
                min = x + 1;
            else if (c > target)
                max = x;
            else break;
        } while (max > min);
        x = (max + min) / 2;
        return getWidthAbove(x);
    }

    public double getWN1() {
        return WN1;
    }

    public double getWN5() {
        return WN5;
    }

    public double getWN10() {
        return WN10;
    }

    public double getHRVTi() {
        return HRVTi;
    }

    private int getCountBelow(int x) {
        int sum = 0;
        for (int c: count) {
            if (c < x)
                sum += c;
        }
        return sum;
    }

}
