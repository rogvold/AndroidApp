package com.cardiomood.math.parameters;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Created by danon on 18.02.14.
 */
public class MRRValue extends AbstractSingleValue {

    @Override
    public String getName() {
        return "mRR";
    }

    @Override
    public double evaluate(double x[], double y[], int begin, int length) {
        return StatUtils.mean(y, begin, length);
    }

}
