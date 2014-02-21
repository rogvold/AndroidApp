package com.cardiomood.sport.android.audio;

import java.util.Collections;
import java.util.List;

public abstract class SoundLoader {

    public int getClosestBPM(int bpm) {
        List<Integer> bpms = getAvailableBPMS();
        if (bpms.isEmpty())
            return -1;
        Collections.sort(bpms);
        int index = Collections.binarySearch(bpms, bpm);
        if (index >= 0) {
            return bpm;
        } else {
            index = Math.abs(index) - 1;
            if (index == 0)
                return bpms.get(0);
            else if (index == bpms.size())
                return bpms.get(index-1);
            else {
                if (bpms.get(index) - bpm > bpm - bpms.get(index-1)) {
                    return bpms.get(index-1);
                } else return bpms.get(index);
            }
        }
    }

    public abstract int load(int bpm);

    public abstract List<Integer> getAvailableBPMS();
}
