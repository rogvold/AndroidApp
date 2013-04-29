package com.cardiomood.android.analysis.bayevsky;

import java.util.ArrayList;
import java.util.List;

import com.cardiomood.android.analysis.characteristics.CharacteristicsScore;

/**
 * 
 * @author rogvold
 */
public class BaevskyAnalysis {

    private static BaevskyAnalysis instance;

    public static BaevskyAnalysis getInstance() {
        if (instance == null) {
            instance = new BaevskyAnalysis();
        }
        return instance;
    }

    private boolean isBetween(Integer a1, Integer a2, Integer c) {
        return ((c > a1) && (c < a2));
    }

    public CharacteristicsScore getCharacteristics(List<Integer> rates) {
        Integer pos = 0;
        Integer neg = 0;
        Integer sum = 0;
        String description = "";
        List<CharacteristicsScore> clist = new ArrayList<CharacteristicsScore>();
        clist.add((new BaevskyCharacteristicsH1(rates)).getResult());
        clist.add((new BaevskyCharacteristicsH2(rates)).getResult());
        clist.add((new BaevskyCharacteristicsH3(rates)).getResult());
        clist.add((new BaevskyCharacteristicsH4(rates)).getResult());
        clist.add((new BaevskyCharacteristicsH5(rates)).getResult());

        for (CharacteristicsScore cs : clist) {
            if (cs.getScore() > 0) {
                pos += cs.getScore();
            } else {
                neg += cs.getScore();
            }
            sum += Math.abs(cs.getScore());
            description += cs.getDescription() + "("+cs.getScore() + ") | ";
        }
        neg = Math.abs(neg);

        CharacteristicsScore resultCS = new CharacteristicsScore(0, "");
        
        if (sum < 3) {
            resultCS = new CharacteristicsScore(sum, "", "Норма");
        }

        if (isBetween(2, 5, sum) && (neg <= 0.5 * sum)) {
            resultCS = new CharacteristicsScore(sum, "", "Функциональное напряжение умеренное");
        }

        if (isBetween(2, 5, sum) && (neg > 0.5 * sum)) {
            resultCS = new CharacteristicsScore(sum, "", "Функциональное напряжение умеренное c активацией хилинергетического звена регуляции");
        }

        if (isBetween(4, 7, sum) && (neg <= 0.5 * sum)) {
            resultCS = new CharacteristicsScore(sum, "", "Функциональное напряжение выраженное");
        }

        if (isBetween(4, 7, sum) && (neg > 0.5 * sum)) {
            resultCS = new CharacteristicsScore(sum, "", "Функциональное напряжение выраженное c активацией хилинергетического звена регуляции");
        }

        if (sum == 7) {
            resultCS = new CharacteristicsScore(sum, "", "Функциональное напряжение резкое");
        }


        if (isBetween(7, 10, sum)) {
            resultCS = new CharacteristicsScore(sum, "", "Перенапряжение регуляторных систем");
        }

        if (sum == 10) {
            resultCS = new CharacteristicsScore(sum, "", "Астенизация (истощение) регуляторных систем");
        }

        resultCS.setDescription(description);
        
        return resultCS;
    }
}
