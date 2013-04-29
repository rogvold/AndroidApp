package com.cardiomood.android.analysis.bayevsky;

import java.util.List;

import com.cardiomood.android.analysis.characteristics.Characteristics;
import com.cardiomood.android.analysis.characteristics.CharacteristicsScore;
import com.cardiomood.android.analysis.indicators.HRVIndicatorsService;
import com.cardiomood.android.analysis.indicators.StatisticsIndicatorsService;

/**
 *
 * @author rogvold
 */
public class BaevskyCharacteristicsH2 extends Characteristics {

    public BaevskyCharacteristicsH2(List<Integer> rates) {
        super(rates);
    }

    @Override
    public String getName() {
        return "function of automatism";
    }

    @Override
    public CharacteristicsScore getResult() {
        HRVIndicatorsService hrv = new HRVIndicatorsService(rates);
        StatisticsIndicatorsService sis = new StatisticsIndicatorsService(rates);
        double m = sis.getRRNN();
        double sigma = sis.getSDNN();
        double delta = hrv.getBP();
        double v = 100.0 * sigma / m;

        if ((sigma < 100) && (delta >= 600) && (v < 8)) {
            return new CharacteristicsScore(-2, "Expressed violation of automatism");
        }

        if ((delta >= 450)) {
            return new CharacteristicsScore(-1, "Moderate violation of automatism");
        }

        if ((sigma <= 20) && (delta <= 100) && (v <= 2)) {
            return new CharacteristicsScore(2, "Stable rhythm");
        }

        if ((sigma >= 100) && (delta >= 300) && (v >= 8)) {
            return new CharacteristicsScore(1, "Expressed sinus arrhythmia");
        }

        return new CharacteristicsScore(0, "Moderate sinus arrhythmia");
    }
}
