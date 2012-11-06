package org.macondo.polar.evaluation.statistics;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;

import java.util.List;

/**
 * <p></p>
 *
 * Date: 16.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class Average implements Evaluation<Integer> {
    public Integer evaluate(Training training) {
        return doEvaluate(training.getIntervals());
    }

    private Integer doEvaluate(List<Integer> intervals) {
        long intervalsTotal = 0;
        
        for (Integer interval : intervals) {
            intervalsTotal += interval;
        }
        return (int) (intervalsTotal / intervals.size());
    }
}
