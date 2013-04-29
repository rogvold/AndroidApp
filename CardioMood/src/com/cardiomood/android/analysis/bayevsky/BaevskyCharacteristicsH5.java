package com.cardiomood.android.analysis.bayevsky;

import java.util.List;

import com.cardiomood.android.analysis.characteristics.Characteristics;
import com.cardiomood.android.analysis.characteristics.CharacteristicsScore;
import com.cardiomood.android.analysis.indicators.SpectrumIndicatorsService;

/**
 *
 * @author rogvold
 */
public class BaevskyCharacteristicsH5 extends Characteristics {

    public BaevskyCharacteristicsH5(List<Integer> rates) {
        super(rates);
    }

    @Override
    public String getName() {
        return "basal activity of the nervous centers";
    }

    @Override
    public CharacteristicsScore getResult() {
        SpectrumIndicatorsService spis = new SpectrumIndicatorsService(rates);
        double sD = spis.getSdPercents();
        double s1 = spis.getS1Percents();
        double s2 = spis.getS2Percents();
        double s = s1 + s2 + sD;
        double s2_s = s2 * 100.0 / s;
        double s1_s = s1 * 100.0 / s;
        double sD_s = sD * 100.0 / s;

        if ((s2_s >= 70) && (s1_s >= 25) && (sD_s <= 5)) {
            return new CharacteristicsScore(2, "expressed activity increase");
        }

        if ((s2_s >= 60) && (sD_s <= 20)) {
            return new CharacteristicsScore(1, "moderate activity increase");
        }

        if ((s2_s <= 20) && (sD_s >= 40)) {
            return new CharacteristicsScore(-2, "expressed activity decrease");
        }

        if ((s2_s <= 40) && (sD_s >= 30)) {
            return new CharacteristicsScore(-1, "moderate activity decrease");
        }

        return new CharacteristicsScore(0, "normal activity");
    }
}
