package org.macondo.polar.ui.menu;

import org.macondo.polar.ui.PolarUI;
import org.macondo.polar.ui.Polar;
import org.macondo.polar.evaluation.FFT;
import org.macondo.polar.evaluation.Harmonics;
import org.macondo.polar.evaluation.TimedValue;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.LinkedList;

/**
 * <p></p>
 *
 * Date: 17.04.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class DisplayFourierTransoformAction extends AbstractAction {
    public DisplayFourierTransoformAction(String name) {
        super(name);
    }

    public void actionPerformed(ActionEvent e) {
        final List<Harmonics> fft = Polar.getPolarUI().getTraining().evaluate(new FFT());
        List<TimedValue<Integer>> values = new LinkedList<TimedValue<Integer>>();

        for (Harmonics harmonics : fft) {
            values.add(new TimedValue<Integer>(
                    (int) (100000 * harmonics.getFrequency()),
                    (int) harmonics.getValue().abs()
            ));
        }

        Polar.getPolarUI().getGraphPanel().setValues(values);
    }
}
