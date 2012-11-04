package org.macondo.polar.evaluation.geometry;

import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.data.Training;

import java.util.List;

/**
 * <p></p>
 *
 * Date: 11.06.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class EvaluateAdvancedHistogram implements Evaluation<AdvancedHistogram> {
    public AdvancedHistogram evaluate(Training training) {
        AdvancedHistogram h = new AdvancedHistogram(training.getIntervals().size()).init();
        List<Integer> intervals = training.getIntervals();
        for (Integer interval : intervals) {
            h.addRRInterval(interval);
        }
        h.loadValues();
        return h;
    }
}
