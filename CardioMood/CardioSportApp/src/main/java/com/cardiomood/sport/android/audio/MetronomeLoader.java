package com.cardiomood.sport.android.audio;

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetronomeLoader extends SoundLoader {

    private Map<Integer, Integer> sounds = new HashMap<Integer, Integer>();
    private Context context;

    public MetronomeLoader(Context context) {
        try {
            for (int i=60;i<195;i+=5) {
                sounds.put(i, SportSoundManager.INSTANCE.getSoundPool().load(context.getAssets().openFd("data/sound/methronome/" + i + "bpm.mp3"), 0));
            }
        } catch (IOException ex) {
            Toast.makeText(context, "Failed to load sound.", Toast.LENGTH_SHORT).show();
        }
        this.context = context;
    }

    @Override
    public int load(int bpm) {
        if (bpm == -1 || !sounds.containsKey(bpm))
            return -1;
        return sounds.get(bpm);
    }

    @Override
    public List<Integer> getAvailableBPMS() {
        return new ArrayList<Integer>(sounds.keySet());
    }

    private Context getContext() {
        return context;
    }
}
