package org.macondo.polar.ui.menu;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;

import org.macondo.polar.ui.Polar;
import org.macondo.polar.util.FFT;
import org.macondo.polar.util.Periodogram;
import org.macondo.polar.util.TimedValue;

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
        final List<Periodogram> fft = Polar.getPolarUI().getTraining().evaluate(new FFT());
        List<TimedValue<Integer>> values = new LinkedList<TimedValue<Integer>>();

        for (Periodogram periodogram : fft) {
            values.add(new TimedValue<Integer>(
                    (int) (100000 * periodogram.getFrequency()),
                    (int) periodogram.getValue()
            ));
        }

        Polar.getPolarUI().getGraphPanel().setValues(values);
    }
}
