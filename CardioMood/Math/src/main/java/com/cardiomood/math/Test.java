package com.cardiomood.math;

import com.cardiomood.math.histogram.Histogram;
import com.cardiomood.math.parameters.PNN50Value;
import com.cardiomood.math.parameters.RMSSDValue;
import com.cardiomood.math.parameters.SDNNValue;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Created by Anton Danshin on 08/01/15.
 */
public class Test {


    public static void main(String[] args) {
        double rr[] = TEST_RR;
        System.out.println("mRR = " + Math.round(StatUtils.mean(rr)*10)/10d + " vs 906");
        System.out.println("SDNN = " + Math.round(new SDNNValue().evaluate(null, rr)*100)/100d + " vs 71");
        System.out.println("RMSSD = " + Math.round(new RMSSDValue().evaluate(null, rr)*100)/100d + " vs 54");
        System.out.println("pNN50 = " + Math.round(new PNN50Value().evaluate(null, rr)*100)/100d + " vs 40");
        Histogram h = new Histogram50(rr);
        System.out.println("Mo = " + Math.round(h.getMo()) + " vs 900");
        System.out.println("AMo = " + Math.round(h.getAMo()*100)/100d + " vs 27");
        System.out.println("SI = " + Math.round(h.getSI()*10)/10d + " vs 82");

        System.out.println("Histogram:");
        for (double x = 300; x < 1400; x += 50) {
            System.out.println(x + "\t" + h.getCountFor(x));
        }
    }

    static final double[] TEST_RR = new double[]{
            //367 ,
            754 ,
            783 ,
            769 ,
            744 ,
            783 ,
            811 ,
            791 ,
            758 ,
            725 ,
            766 ,
            796 ,
            808 ,
            783 ,
            754 ,
            776 ,
            806 ,
            858 ,
            866 ,
            834 ,
            899 ,
            934 ,
            899 ,
            817 ,
            805 ,
            873 ,
            949 ,
            943 ,
            955 ,
            982 ,
            965 ,
            906 ,
            904 ,
            934 ,
            986 ,
            924 ,
            889 ,
            951 ,
            1019,
            1001,
            925 ,
            954 ,
            1004,
            995 ,
            914 ,
            876 ,
            940 ,
            990 ,
            958 ,
            968 ,
            1003,
            1007,
            941 ,
            886 ,
            950 ,
            1007,
            975 ,
            903 ,
            932 ,
            932 ,
            925 ,
            865 ,
            917 ,
            992 ,
            1009,
            930 ,
            947 ,
            964 ,
            952 ,
            861 ,
            805 ,
            845 ,
            942 ,
            933 ,
            921 ,
            980 ,
            972 ,
            908 ,
            817 ,
            816 ,
            881 ,
            979 ,
            933 ,
            884 ,
            947 ,
            1011,
            958 ,
            881 ,
            852 ,
            877 ,
            946 ,
            907 ,
            868 ,
            952 ,
            1022,
            914 ,
            894 ,
            931 ,
            876 ,
            890 ,
            847 ,
            882 ,
            1000,
            1023,
            950 ,
            926 ,
            971 ,
            998 ,
            983 ,
            902 ,
            863 ,
            861 ,
            896 ,
            889 ,
            859 ,
            968 ,
            1035,
            858 ,
            1017,
            932 ,
            883 ,
            815 ,
            888 ,
            964 ,
            912 ,
            886 ,
            931 ,
            874 ,
            887 ,
            909 ,
            880 ,
            892 ,
            906
    };

    static class Histogram50 extends Histogram {
        Histogram50(double[] values) {
            super(values, 50);
        }

        @Override
        protected void init() {
            double step = getStep();
            for (double value: values) {
                if (value < 350 || value > 1350)
                    continue;
                int i = (int) Math.floor(value / step);
                if (i < 0) i = 0;
                this.count[i]++;
            }
        }
    }

}
