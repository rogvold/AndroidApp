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
public class BaevskyCharacteristicsH3 extends Characteristics {

    public BaevskyCharacteristicsH3(List<Integer> rates) {
        super(rates);
    }

    @Override
    public String getName() {
        return "vegetative homeostasis";
    }

    @Override
    public CharacteristicsScore getResult() {
        HRVIndicatorsService hrv = new HRVIndicatorsService(rates);
        StatisticsIndicatorsService sis = new StatisticsIndicatorsService(rates);
        double Imo = hrv.getAMoPercents();
        double delta = hrv.getBP();
        double N = hrv.getIN();

        if ((delta <= 60) && (Imo >= 80) && (N >= 500)) {
            return new CharacteristicsScore(2, "Expressed predominance of the sympathetic nervous system");
        }

        if ((delta <= 150) && (Imo >= 50) && (N >= 200)) {
            return new CharacteristicsScore(1, "Moderate predominance of the sympathetic nervous system");
        }

        if ((delta >= 500) && (Imo <= 15) && (N <= 25)) {
            return new CharacteristicsScore(-2, "Expressed predominance of the parasympathetic nervous system");
        }

        if ((delta >= 300) && (Imo <= 30) && (N <= 50)) {
            return new CharacteristicsScore(-1, "Moderate predominance of the parasympathetic nervous system");
        }

        return new CharacteristicsScore(0, "Vegetative homeostasis saved");
    }
}
