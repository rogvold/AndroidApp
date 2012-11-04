package org.macondo.polar.evaluation.geometry;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;

/**
 * <p></p>
 *
 * Date: 20.05.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class Histogram {
    private List<HistogramInterval> intervals = new LinkedList<HistogramInterval>();

    public Histogram() {
    }

    public Histogram init() {
        for (int i = 300; i < 1700; i += 50) {
            intervals.add(new HistogramInterval(i, i+50));
        }
        return this;
    }

    public void addRRInterval(Integer length) {
        getIntervalForRR(length).add(length);
    }

    private HistogramInterval getIntervalForRR(int RR) {
        for (HistogramInterval interval : intervals) {
            if (interval.getStart() <= RR && interval.getEnd() > RR) {
                return interval;
            }
        }
        return null;
    }

    protected List<HistogramInterval> getIntervals() {
        return intervals;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Histogram\n");
        for (HistogramInterval interval : intervals) {
            sb.append(interval.toString()).append("\n");
        }
        return sb.toString();
    }
}
