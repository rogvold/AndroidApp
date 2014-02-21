package com.cardiomood.sport.android.audio;

import android.media.AudioManager;
import android.media.SoundPool;

public enum SportSoundManager {

    INSTANCE;

    private final SoundPool soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
    private SoundLoader loader;
    private int streamID = -1;

    public void stop() {
        soundPool.stop(streamID);
    }

    public void release() {
        soundPool.release();
    }

    public void pause() {
        if (streamID != -1)
            soundPool.pause(streamID);
    }

    public void resume() {
        if (streamID != -1)
            soundPool.resume(streamID);
    }

    public void play(int bpm) {
        int closestBPM = loader.getClosestBPM(bpm);
        streamID = soundPool.play(loader.load(closestBPM), 1.0f, 1.0f, 0, -1, (float)(((double)bpm/closestBPM)));
    }

    public SoundLoader getLoader() {
        return loader;
    }

    public void setLoader(SoundLoader loader) {
        this.loader = loader;
    }

    public SoundPool getSoundPool() {
        return soundPool;
    }

    public int getStreamID() {
        return streamID;
    }

}
