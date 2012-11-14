package org.macondo.polar.evaluation;

import java.util.LinkedList;
import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.util.TimedValue;

/**
 * <p></p>
 *
 * Date: 16.04.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class FrequencyFromIntervals implements Evaluation<List<TimedValue<Integer>>>{

    public List<TimedValue<Integer>> evaluate(Training training) {
        int time = 0;
        List<TimedValue<Integer>> values = new LinkedList<TimedValue<Integer>>();
        for (Integer integer : training.getIntervals()) {
            values.add(new TimedValue<Integer>(
                    time,
                    60000 / integer
            ));
        }
        return values;
    }
}
