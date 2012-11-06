package org.macondo.polar.ui;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.CV;
import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.evaluation.RMSSD;
import org.macondo.polar.evaluation.TimedValue;
import org.macondo.polar.evaluation.geometry.EvaluateBasicHistogram;
import org.macondo.polar.evaluation.hrv.AMoPercents;
import org.macondo.polar.evaluation.hrv.BP;
import org.macondo.polar.evaluation.hrv.IN;
import org.macondo.polar.evaluation.hrv.Mo;
import org.macondo.polar.evaluation.statistics.Average;
import org.macondo.polar.evaluation.statistics.SDNN;
import org.macondo.polar.evaluation.time.PNN50;
import org.macondo.polar.ui.hr.GraphDisplayPanel;

/**
 * <p></p>
 *
 * Date: 29.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class PolarUI {
    private static final List<Evaluation> EVALUATIONS =  new LinkedList<Evaluation>(Arrays.asList(
            new Average(),
            new SDNN(),
            new RMSSD(),
            new PNN50(),
            new CV(),
            new EvaluateBasicHistogram(),
            //new EvaluateAdvancedHistogram()
            new AMoPercents(),
            new IN(),
            new BP(),
            new Mo()
    ));


    private Training training;
    private GraphDisplayPanel graphPanel;

    public PolarUI() {
        graphPanel = new GraphDisplayPanel();
    }

    public GraphDisplayPanel getGraphPanel() {
        return graphPanel;
    }

    public void setGraphPanel(GraphDisplayPanel graphPanel) {
        this.graphPanel = graphPanel;
    }

    public Training getTraining() {
        return training;
    }

    public void setTraining(Training training) {
        this.training = training;
        graphPanel.setValues(TimedValue.convertList(training.getIntervals()));


        for (Evaluation evaluation : EVALUATIONS) {
            System.out.println(evaluation.getClass().getName() + ": " + training.evaluate(evaluation));
        }
    }
}


