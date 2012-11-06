package org.macondo.polar.ui;

import org.macondo.polar.ui.hr.GraphDisplayPanel;
import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.*;
import org.macondo.polar.evaluation.geometry.EvaluateBasicHistogram;
import org.macondo.polar.evaluation.geometry.EvaluateAdvancedHistogram;
import org.macondo.polar.evaluation.statistics.Average;
import org.macondo.polar.evaluation.statistics.SDNN;
import org.macondo.polar.evaluation.time.PNN50;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;

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
            new EvaluateAdvancedHistogram()
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


