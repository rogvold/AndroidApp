package org.macondo.polar.ui;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.macondo.polar.data.Training;
import org.macondo.polar.evaluation.Evaluation;
import org.macondo.polar.evaluation.hrv.AMoPercents;
import org.macondo.polar.evaluation.hrv.BP;
import org.macondo.polar.evaluation.hrv.IN;
import org.macondo.polar.evaluation.spectrum.HFPercents;
import org.macondo.polar.evaluation.spectrum.IC;
import org.macondo.polar.evaluation.spectrum.LFPercents;
import org.macondo.polar.evaluation.spectrum.TP;
import org.macondo.polar.evaluation.spectrum.ULFPercents;
import org.macondo.polar.evaluation.spectrum.VLFPercents;
import org.macondo.polar.evaluation.statistics.Average;
import org.macondo.polar.evaluation.statistics.RMSSD;
import org.macondo.polar.evaluation.statistics.SDNN;
import org.macondo.polar.evaluation.time.PNN50;
import org.macondo.polar.ui.hr.GraphDisplayPanel;
import org.macondo.polar.util.Autocorrelation;
import org.macondo.polar.util.TimedValue;

/**
 * <p></p>
 *
 * Date: 29.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class PolarUI {
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static final List<Evaluation> EVALUATIONS =  new LinkedList<Evaluation>(Arrays.asList(
            new Average(),
            new SDNN(),
            new RMSSD(),
            new PNN50(),
            new AMoPercents(),
            new BP(),
            new IN(),
            new TP(),
            new HFPercents(),
            new LFPercents(),
            new ULFPercents(),
            new VLFPercents(),
            new IC()
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


