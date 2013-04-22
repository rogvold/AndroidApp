package org.macondo.polar.evaluation.statistics;

import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

/**
 * <p></p>
 *
 * Date: 19.05.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class RMSSD implements Evaluation<Integer> {
    public Integer evaluate(Training training) {
        final List<Integer> intervals = training.getIntervals();
        long total = 0;

        for (int i = 1; i < intervals.size(); i++) {
            Integer now = intervals.get(i);
            Integer before = intervals.get(i-1);

            total += (now - before) * (now - before);
        }

        return (int) Math.sqrt(total / intervals.size());
    }
}
