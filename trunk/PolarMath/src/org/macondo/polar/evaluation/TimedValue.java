package org.macondo.polar.evaluation;

import java.util.List;
import java.util.LinkedList;

/**
 * <p></p>
 *
 * Date: 16.04.2008
 *
 * @author <a href="mailto:ktsibriy@gmail.com">Kirill Y. Tsibriy</a>
 */
public class TimedValue <T> {
    private int time;
    private T value;

    public TimedValue(int time, T value) {
        this.time = time;
        this.value = value;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public static <T> List<TimedValue<T>> convertList(List<T> values) {
        List<TimedValue<T>> output = new LinkedList<TimedValue<T>>();
        for (int i = 0; i < values.size(); i++) {
            T t = values.get(i);
            output.add(new TimedValue<T>(i, t));
        }
        return output;
    }
}
