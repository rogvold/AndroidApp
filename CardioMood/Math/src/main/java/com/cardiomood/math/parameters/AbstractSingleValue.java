package com.cardiomood.math.parameters;

/**
 * Created by danon on 18.02.14.
 */
public abstract class AbstractSingleValue {

    public abstract String getName();

    public abstract double evaluate(double x[], double y[], int begin, int length);

    public double evaluate(double x[], double y[]) {
        return evaluate(x, y, 0, x.length);
    }

}
