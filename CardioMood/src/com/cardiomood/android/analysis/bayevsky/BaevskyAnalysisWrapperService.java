package com.cardiomood.android.analysis.bayevsky;

import java.util.List;

import com.cardiomood.android.analysis.characteristics.CharacteristicsScore;
import com.cardiomood.android.analysis.indicators.AbstractIndicatorsService;

/**
 *
 * @author rogvold
 */
public class BaevskyAnalysisWrapperService extends AbstractIndicatorsService {

    public BaevskyAnalysisWrapperService(List<Integer> intervals) {
        super(intervals);
    }

    public BaevskyAnalysisWrapperService() {
    }
    
    public Integer getP(){
        CharacteristicsScore cs = BaevskyAnalysis.getInstance().getCharacteristics(intervals);
        return cs.getScore();
    }
    
}
