package io.github.foadsf.kangaroohops;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * A rising note per stone, so counting up sounds like climbing a scale and counting down like
 * going down it: stone n plays the n-th note of a G major scale. Plus a "boop" and a fanfare.
 * Synthesised at start-up; no sound files.
 */
final class Sounds {
    private static final int RATE = 22050;
    private static final int[] MAJOR = {0, 2, 4, 5, 7, 9, 11};
    private final AudioTrack[] stones = new AudioTrack[21];
    private final AudioTrack boop, fanfare;

    Sounds() {
        for (int n = 0; n < stones.length; n++) {
            int semis = 12 * (n / 7) + MAJOR[n % 7];
            float f = (float) (196.0 * Math.pow(2, semis / 12.0));  // from G3
            stones[n] = track(new float[]{f}, 0.16f, 0.22f);
        }
        boop = track(new float[]{233.08f, 196f}, 0.10f, 0.28f);
        fanfare = track(new float[]{523.25f, 659.25f, 783.99f, 1046.5f}, 0.10f, 0.22f);
    }

    private static AudioTrack track(float[] tones, float seconds, float volume) {
        int per = (int) (RATE * seconds);
        short[] pcm = new short[per * tones.length];
        for (int k = 0; k < tones.length; k++) {
            for (int i = 0; i < per; i++) {
                double t = i / (double) RATE;
                double env = Math.min(1, i / (RATE * 0.006)) * Math.pow(1 - i / (double) per, 2);
                double s = Math.sin(2 * Math.PI * tones[k] * t) + 0.2 * Math.sin(4 * Math.PI * tones[k] * t);
                pcm[k * per + i] = (short) (s / 1.2 * env * volume * Short.MAX_VALUE);
            }
        }
        try {
            AudioTrack a = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, pcm.length * 2, AudioTrack.MODE_STATIC);
            a.write(pcm, 0, pcm.length);
            return a;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void play(AudioTrack a) {
        if (a == null || a.getState() != AudioTrack.STATE_INITIALIZED) return;
        try {
            a.stop();
            a.reloadStaticData();
            a.play();
        } catch (IllegalStateException ignored) {
        }
    }

    void stone(int n) {
        if (n >= 0 && n < stones.length) play(stones[n]);
    }

    void boop() {
        play(boop);
    }

    void fanfare() {
        play(fanfare);
    }

    void release() {
        for (AudioTrack a : stones) if (a != null) a.release();
        if (boop != null) boop.release();
        if (fanfare != null) fanfare.release();
    }
}
