package org.macondo.polar.evaluation;

import org.macondo.polar.data.Training;

/**
 * <p></p>
 *
 * Date: 19.05.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class CV implements Evaluation<Integer> {
    private static final Evaluation<Integer> avg = new Average();
    private static final Evaluation<Integer> sdnn = new SDNN();

    public Integer evaluate(Training training) {
        return 100 * training.evaluate(sdnn) / training.evaluate(avg);
    }
}
