package io.github.foadsf.balancescale;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * A "plop" when fruit lands (lower for heavier fruit), a soft "pop" when one comes off, and a
 * little fanfare when the scale balances. Synthesised at start-up; no sound files.
 */
final class Sounds {
    private static final int RATE = 22050;
    private final AudioTrack[] plop = new AudioTrack[4];
    private final AudioTrack pop, fanfare;

    Sounds() {
        float[] base = {520f, 560f, 330f, 220f};   // apple, pear, banana, melon
        for (int i = 0; i < plop.length; i++) plop[i] = sweep(base[i], base[i] * 0.55f, 0.12f, 0.30f);
        pop = sweep(700f, 980f, 0.07f, 0.18f);
        fanfare = notes(new float[]{523.25f, 659.25f, 783.99f, 1046.5f}, 0.10f, 0.22f);
    }

    /** A short tone gliding from f0 to f1: the "plop". */
    private static AudioTrack sweep(float f0, float f1, float seconds, float volume) {
        int n = (int) (RATE * seconds);
        short[] pcm = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = f0 + (f1 - f0) * t;
            phase += 2 * Math.PI * f / RATE;
            double env = Math.min(1, i / (RATE * 0.004)) * Math.pow(1 - t, 2);
            pcm[i] = (short) (Math.sin(phase) * env * volume * Short.MAX_VALUE);
        }
        return track(pcm);
    }

    private static AudioTrack notes(float[] tones, float seconds, float volume) {
        int per = (int) (RATE * seconds);
        short[] pcm = new short[per * tones.length];
        for (int k = 0; k < tones.length; k++) {
            for (int i = 0; i < per; i++) {
                double t = i / (double) RATE;
                double env = Math.min(1, i / (RATE * 0.006)) * Math.pow(1 - i / (double) per, 2);
                pcm[k * per + i] = (short) (Math.sin(2 * Math.PI * tones[k] * t) * env * volume * Short.MAX_VALUE);
            }
        }
        return track(pcm);
    }

    private static AudioTrack track(short[] pcm) {
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

    void plop(int fruit) {
        play(plop[fruit]);
    }

    void pop() {
        play(pop);
    }

    void fanfare() {
        play(fanfare);
    }

    void release() {
        for (AudioTrack a : plop) if (a != null) a.release();
        if (pop != null) pop.release();
        if (fanfare != null) fanfare.release();
    }
}
