package org.macondo.polar.evaluation.geometry;

import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.data.Training;

import java.util.List;

/**
 * <p></p>
 *
 * Date: 21.05.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class EvaluateBasicHistogram implements Evaluation<Histogram> {
    public Histogram evaluate(Training training) {
        Histogram h = new Histogram().init();
        List<Integer> intervals = training.getIntervals();
        for (Integer interval : intervals) {
            h.addRRInterval(interval);
        }
        return h;
    }
}
