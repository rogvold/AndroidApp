package org.macondo.polar.util;

import java.util.List;
import java.util.LinkedList;

/**
 * <p>Interval of a histogram</p>
 *
 * Date: 20.05.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class HistogramInterval {
    private Integer start;
    private Integer end;
    private Integer number = 0;
    private List<Integer> values = new LinkedList<Integer>();

    public HistogramInterval(Integer start, Integer end) {
        this.start = start;
        this.end = end;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getEnd() {
        return end;
    }

    public Integer getNumber() {
        return number;
    }

    public void add(Integer interval) {
        this.number++;
        this.values.add(interval);
    }

    public List<Integer> getValues() {
        return this.values;
    }

    public String toString() {
        return new StringBuilder("(")
                .append(start)
                .append(", ")
                .append(end)
                .append(") = ")
                .append(number).toString();
    }
}
