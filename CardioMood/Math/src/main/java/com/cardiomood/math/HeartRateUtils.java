package com.cardiomood.math;

import com.cardiomood.math.parameters.AValue;
import com.cardiomood.math.parameters.AbstractSingleValue;
import com.cardiomood.math.parameters.MRRValue;
import com.cardiomood.math.parameters.PNN50Value;
import com.cardiomood.math.parameters.RMSSDValue;
import com.cardiomood.math.parameters.SDNNValue;
import com.cardiomood.math.parameters.SIValue;
import com.cardiomood.math.window.DataWindow;

import org.apache.commons.math3.util.DoubleArray;
import org.apache.commons.math3.util.ResizableDoubleArray;

import static com.cardiomood.math.window.DataWindow.CallbackAdapter;

/**
 * Created by danon on 06.04.2014.
 */
public final class HeartRateUtils {

    private static final AbstractSingleValue mRR = new MRRValue();
    private static final AbstractSingleValue SDNN = new SDNNValue();
    private static final AbstractSingleValue RMSSD = new RMSSDValue();
    private static final AbstractSingleValue PNN50 = new PNN50Value();
    private static final AbstractSingleValue SI = new SIValue();
    private static final AbstractSingleValue A = new AValue();


    private HeartRateUtils() {}

    public static double getMRR(double[] rrIntervals) {
        return getMRR(rrIntervals, 0, rrIntervals.length);
    }

    public static double getMRR(double[] rrIntervals, int begin, int length) {
        return mRR.evaluate(null, rrIntervals, begin, length);
    }

    public static double getSDNN(double[] rrIntervals) {
        return getSDNN(rrIntervals, 0, rrIntervals.length);
    }

    public static double getSDNN(double[] rrIntervals, int begin, int length) {
        return SDNN.evaluate(null, rrIntervals, begin, length);
    }

    public static double getRMSSD(double[] rrIntervals) {
        return getRMSSD(rrIntervals, 0, rrIntervals.length);
    }

    public static double getRMSSD(double[] rrIntervals, int begin, int length) {
        return RMSSD.evaluate(null, rrIntervals, begin, length);
    }

    public static double getPNN50(double[] rrIntervals) {
        return getPNN50(rrIntervals, 0, rrIntervals.length);
    }

    public static double getPNN50(double[] rrIntervals, int begin, int length) {
        return PNN50.evaluate(null, rrIntervals, begin, length);
    }

    public static double getSI(double[] rrIntervals) {
        return getSI(rrIntervals, 0, rrIntervals.length);
    }

    public static double getSI(double[] rrIntervals, int begin, int length) {
        return SI.evaluate(null, rrIntervals, begin, length);
    }

    public static double[][] getSI(double[] rrIntervals, DataWindow window) {
        return getSI(rrIntervals, 0, rrIntervals.length, window);
    }

    public static double getA(double[] rrIntervals) {
        return getA(rrIntervals, 0, rrIntervals.length);
    }

    public static double getA(double[] rrIntervals, int begin, int length) {
        return A.evaluate(null, rrIntervals, begin, length);
    }

    public static double[][] getA(double[] rrIntervals, DataWindow window) {
        return getA(rrIntervals, 0, rrIntervals.length, window);
    }

    public static double[][] getA(double[] rrIntervals, int begin, int length, DataWindow window) {
        final DoubleArray time = new ResizableDoubleArray();
        final DoubleArray values = new ResizableDoubleArray();

        window.setCallback(new CallbackAdapter() {
            @Override
            public void onStep(DataWindow window, int index, double t, double value) {
                time.addElement(t);
                values.addElement(getA(window.getIntervals().getElements()));
            }
        });
        int n = 0;
        for (int i=begin; i<rrIntervals.length && n<length; i++, n++) {
            window.add(rrIntervals[i]);
        }

        double[][] result = new double[2][];
        result[0] = time.getElements();
        result[1] = values.getElements();
        return result;
    }

    public static double[][] getSI(double[] rrIntervals, int begin, int length, DataWindow window) {
        final DoubleArray time = new ResizableDoubleArray();
        final DoubleArray values = new ResizableDoubleArray();

        window.setCallback(new CallbackAdapter() {
            @Override
            public void onStep(DataWindow window, int index, double t, double value) {
                time.addElement(t);
                values.addElement(getSI(window.getIntervals().getElements()));
            }
        });
        int n = 0;
        for (int i=begin; i<rrIntervals.length && n<length; i++, n++) {
            window.add(rrIntervals[i]);
        }

        double[][] result = new double[2][];
        result[0] = time.getElements();
        result[1] = values.getElements();
        return result;
    }

    public static double[][] getSDNN(double[] rrIntervals, DataWindow window) {
        return getSDNN(rrIntervals, 0, rrIntervals.length, window);
    }

    public static double[][] getSDNN(double[] rrIntervals, int begin, int length, DataWindow window) {
        final DoubleArray time = new ResizableDoubleArray();
        final DoubleArray values = new ResizableDoubleArray();

        window.setCallback(new CallbackAdapter() {
            @Override
            public void onStep(DataWindow window, int index, double t, double value) {
                time.addElement(t);
                values.addElement(getSDNN(window.getIntervals().getElements()));
            }
        });
        int n = 0;
        for (int i=begin; i<rrIntervals.length && n<length; i++, n++) {
            window.add(rrIntervals[i]);
        }

        double[][] result = new double[2][];
        result[0] = time.getElements();
        result[1] = values.getElements();
        return result;
    }

    public static double[][] getSDNN(double[] rrIntervals, double[] time, int windowSize, int step) {
        DoubleArray values = new ResizableDoubleArray();
        DoubleArray timeValues = new ResizableDoubleArray();
        int i = 0, i0=0;
        while (i < rrIntervals.length) {
            double t = time[i++];
            if (i-i0 >= windowSize) {
                values.addElement(getSDNN(rrIntervals, i0, i-i0));
                timeValues.addElement(t);
                i0 = values.getNumElements()*step;
            }
        }
        double[][] result = new double[2][];
        result[0] = timeValues.getElements();
        result[1] = values.getElements();
        return result;
    }

    public static double[][] getSI(double[] rrIntervals, double[] time, double windowSize, double step) {
        DoubleArray values = new ResizableDoubleArray();
        DoubleArray timeValues = new ResizableDoubleArray();
        int i = 0, i0=0;
        double t0 = time[0];
        while (i < rrIntervals.length) {
            double t = time[i++];
            if (t - t0 >= windowSize) {
                values.addElement(getSI(rrIntervals, i0, i-i0));
                timeValues.addElement(t);
                t0 = time[0] + values.getNumElements() * step;
            }
            if (t - time[0] >= windowSize) {
                while (i0 < i && time[i0] < t0) {
                    i0++;
                }
                t0 = time[i0];
            }
        }
        double[][] result = new double[2][];
        result[0] = timeValues.getElements();
        result[1] = values.getElements();
        return result;
    }

    public static double getSum(double[] rrIntervals) {
        return getSum(rrIntervals, 0, rrIntervals.length);
    }

    public static double getSum(double[] rrIntervals, int begin, int length) {
        int n = 0;
        double sum = 0;
        for (int i=begin; i<rrIntervals.length && n<length; i++, n++) {
            sum += rrIntervals[i];
        }
        return sum;
    }
}
