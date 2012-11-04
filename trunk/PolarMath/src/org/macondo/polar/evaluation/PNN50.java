package org.macondo.polar.evaluation;

import org.macondo.polar.data.Training;

import java.util.List;

/**
 * <p></p>
 *
 * Date: 19.05.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class PNN50 implements Evaluation<Double> {
    public Double evaluate(Training training) {
        final List<Integer> intervals = training.getIntervals();
        int pnn = 0;


        for (int i = 1; i < intervals.size(); i++) {
            Integer now = intervals.get(i);
            Integer before = intervals.get(i-1);

            if (Math.abs(now - before) >= 50) {
                pnn++;
            }
        }

        return ((double) pnn) / (intervals.size() - 1);
    }
}
