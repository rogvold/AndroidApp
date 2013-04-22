package org.macondo.polar.data;

import org.macondo.polar.evaluation.Evaluation;

import java.util.List;
import java.io.InputStream;
import java.io.IOException;

/**
 * <p>Object representing training data.</p>
 *
 * Date: 16.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class Training {
    private String idString;
    private List<Integer> intervals;

    private final TrainingCache cache = new TrainingCache();

    public Training() {
    }

    Training(String idString, List<Integer> intervals) {
        this.idString = idString;
        this.intervals = intervals;
    }

    public String getIdString() {
        return idString;
    }

    public void setIdString(String idString) {
        this.idString = idString;
    }

    public List<Integer> getIntervals() {
        return intervals;
    }

    public void setIntervals(List<Integer> intervals) {
        this.intervals = intervals;
    }

    public <T> T evaluate(Evaluation<T> evaluation) {
        if (cache.contains(evaluation)) {
            return cache.get(evaluation);
        } else {
            T evaluationResult = evaluation.evaluate(this);
            cache.add(evaluation, evaluationResult);
            return evaluationResult;
        }
    }

    public static Training readTraining(InputStream is) throws IOException {
        return new TrainingReader().readTraining(is);
    }
}
