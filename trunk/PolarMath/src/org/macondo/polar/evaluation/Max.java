package org.macondo.polar.evaluation;

import org.macondo.polar.data.Training;

import java.util.Collections;

/**
 * <p></p>
 *
 * Date: 29.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class Max implements Evaluation<Integer>{
    public Integer evaluate(Training training) {
        return Collections.max(training.getIntervals());
    }
}
