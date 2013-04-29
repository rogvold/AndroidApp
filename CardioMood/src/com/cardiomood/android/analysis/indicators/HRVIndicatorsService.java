package com.cardiomood.android.analysis.indicators;

import java.util.Collections;
import java.util.List;
import com.cardiomood.android.analysis.indicators.utils.Histogram;

/**
 *
 * @author rogvold, ???
 */
public class HRVIndicatorsService extends AbstractIndicatorsService {

    public HRVIndicatorsService(List<Integer> intervals) {
        super(intervals);
    }

    public HRVIndicatorsService() {
    }

    public double getAMoPercents() {
        List<Integer> list = getIntervalsInDuration();
        Histogram h = new Histogram(list.size()).init();
        for (Integer interval : list) {
            h.addRRInterval(interval);
        }
        int maxRangeValue = h.getMaxIntervalNumber();
        int totalCount = h.getTotalCount();
        return Math.floor((maxRangeValue / (double) totalCount) * 10000) / 100.0;
    }

    public double getIN() {
        double bp = getBP();
        double amo = getAMoPercents();
        double mo = getMo();
        return Math.floor((amo * 1000 * 1000 / (2 * bp * mo)) * 100) / 100.0;
    }

    public double getBP() {
        List<Integer> list = getIntervalsInDuration();
        return (Collections.max(list) - Collections.min(list));
    }

    public double getMo() {
        List<Integer> list = getIntervalsInDuration();
        Histogram h = new Histogram(list.size()).init();
        for (Integer interval : list) {
            h.addRRInterval(interval);
        }
        return h.getMaxIntervalStart();
    }
}
