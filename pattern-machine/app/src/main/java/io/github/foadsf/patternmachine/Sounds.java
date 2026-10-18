package io.github.foadsf.patternmachine;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Every token sings: six notes of a pentatonic scale, so any pattern she makes sounds pleasant.
 * Plus a soft "boop" for a wrong pick and a little fanfare. All synthesised, no sound files.
 */
final class Sounds {
    private static final int RATE = 22050;
    // C5 D5 E5 G5 A5 C6
    private static final float[] SCALE = {523.25f, 587.33f, 659.25f, 783.99f, 880.00f, 1046.50f};
    private final AudioTrack[] notes = new AudioTrack[SCALE.length];
    private final AudioTrack boop, fanfare;

    Sounds() {
        for (int i = 0; i < SCALE.length; i++) notes[i] = track(new float[]{SCALE[i]}, 0.22f, 0.22f);
        boop = track(new float[]{233.08f, 196f}, 0.10f, 0.28f);
        fanfare = track(new float[]{523.25f, 659.25f, 783.99f, 1046.5f}, 0.10f, 0.22f);
    }

    private static AudioTrack track(float[] tones, float seconds, float volume) {
        int per = (int) (RATE * seconds);
        short[] pcm = new short[per * tones.length];
        for (int n = 0; n < tones.length; n++) {
            for (int i = 0; i < per; i++) {
                double t = i / (double) RATE;
                double env = Math.min(1, i / (RATE * 0.006)) * Math.pow(1 - i / (double) per, 2);
                // a soft marimba-ish tone: fundamental plus a quiet 4th harmonic
                double s = Math.sin(2 * Math.PI * tones[n] * t) + 0.18 * Math.sin(8 * Math.PI * tones[n] * t);
                pcm[n * per + i] = (short) (s / 1.18 * env * volume * Short.MAX_VALUE);
            }
        }
        try {
            AudioTrack a = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, pcm.length * 2, AudioTrack.MODE_STATIC);
            a.write(pcm, 0, pcm.length);
            return a;
        } catch (RuntimeException e) {
            return null;  // the game works silently too
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

    void note(int i) {
        play(notes[((i % notes.length) + notes.length) % notes.length]);
    }

    void boop() {
        play(boop);
    }

    void fanfare() {
        play(fanfare);
    }

    void release() {
        for (AudioTrack a : notes) if (a != null) a.release();
        if (boop != null) boop.release();
        if (fanfare != null) fanfare.release();
    }
}
