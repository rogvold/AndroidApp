package org.macondo.polar;

import org.macondo.polar.data.Training;

import java.io.InputStream;
import java.io.IOException;

/**
 * <p></p>
 *
 * Date: 16.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public final class Statistics {
    public static void main(String[] args) throws IOException {
        InputStream is = Statistics.class.getResourceAsStream("datahrm.properties");
        Training t = Training.readTraining(is);
        System.out.println(t.getIntervals().size());
    }
}
