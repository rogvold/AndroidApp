package com.cardiomood.android.analysis.bayevsky;

import java.util.List;

import com.cardiomood.android.analysis.characteristics.Characteristics;
import com.cardiomood.android.analysis.characteristics.CharacteristicsScore;
import com.cardiomood.android.analysis.indicators.StatisticsIndicatorsService;

/**
 *
 * @author rogvold
 */
public class BaevskyCharacteristicsH1 extends Characteristics {

    public BaevskyCharacteristicsH1(List<Integer> rates) {
        super(rates);
    }

    @Override
    public String getName() {
        return "The total effect of regulation";
    }

    @Override
    public CharacteristicsScore getResult() {
        StatisticsIndicatorsService sis = new StatisticsIndicatorsService(rates);
        double m = sis.getRRNN();
        if (m <= 660) {
            return new CharacteristicsScore(2, "Expressed tachycardia","Выражденная тахикардия");
        }

        if (m <= 800) {
            return new CharacteristicsScore(1, "Moderate tachycardia", "Умеренная тахикардия");
        }

        if ((m > 800) && (m < 1000)) {
            return new CharacteristicsScore(0, "Normokardia", "Нормокардия");
        }

        if (m >= 1200) {
            return new CharacteristicsScore(-2, "Expressed bradykardia", "Вырожденная барикардия");
        }

        if (m >= 1000) {
            return new CharacteristicsScore(-1, "Moderate bradykardia","Умеренная брадикардия");
        }

        return null;
    }
}
