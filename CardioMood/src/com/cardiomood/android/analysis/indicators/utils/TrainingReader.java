
package com.cardiomood.android.analysis.indicators.utils;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

/**
 * <p></p>
 *
 * Date: 16.03.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
class TrainingReader {
    public static final String HR_DATA_ELEMENT = "[HRData]";
    Training readTraining(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String lastLine;
        do {} while (!HR_DATA_ELEMENT.equals(lastLine = br.readLine()));

        List<Integer> intervals = new LinkedList<Integer>();
        while ((lastLine = br.readLine()) != null) {
            intervals.add(Integer.valueOf(lastLine));
        }

        return new Training("", intervals);
    }
}