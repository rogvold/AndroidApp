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
        HRVTi = ((double) values.length) / ((double) height);
        WN1 = getWN(1.0);
        WN5 = getWN(5.0);
        WN10 = getWN(10.0);
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

}
