package com.cardiomood.android.analysis.indicators;

import java.util.List;

/**
 *
 * @author rogvold, ???
 */
public class TimeIndicatorsService extends AbstractIndicatorsService{

    public TimeIndicatorsService(List<Integer> intervals) {
        super(intervals);
    }

    public TimeIndicatorsService() {
    }
    
    
    
    public double getPNN50(){
        int pnn = 0;
        for (int i = 1; i < intervals.size(); i++) {
            Integer now = intervals.get(i);
            Integer before = intervals.get(i-1);
            if (Math.abs(now - before) >= 50) {
                pnn++;
            }
        }

        return Math.floor(((double) pnn) / (intervals.size() - 1) * 10000)/100.0;
    }
    
}
