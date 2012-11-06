package org.macondo.polar.evaluation.statistics;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

/**
 * <p></p>
 *
 * Date: 16.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class SDNN implements Evaluation<Integer>{
    public Integer evaluate(Training training) {
        Integer average = training.evaluate(new Average());
        long total = 0;
        for (Integer integer : training.getIntervals()) {
            total += (average - integer) * (average - integer);
        }
        return (int) Math.sqrt(total / training.getIntervals().size());
    }
}
