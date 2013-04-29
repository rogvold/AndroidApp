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
public class BaevskyCharacteristicsH4 extends Characteristics {

    public BaevskyCharacteristicsH4(List<Integer> rates) {
        super(rates);
    }

    @Override
    public String getName() {
        return "stability of regulation";
    }

    @Override
    public CharacteristicsScore getResult() {
        HRVIndicatorsService hrv = new HRVIndicatorsService(rates);
        StatisticsIndicatorsService sis = new StatisticsIndicatorsService(rates);
        double m = sis.getRRNN();
        double sigma = sis.getSDNN();
        double v = 100.0 * sigma / m;

        if (v <= 3) {
            return new CharacteristicsScore(2, " dysregulation");
        }

        if (v >= 6) {
            return new CharacteristicsScore(-2, " dysregulation");
        }

        return new CharacteristicsScore(0, "Stable dysregulation");
    }
}
