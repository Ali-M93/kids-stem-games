package io.github.foadsf.robotpath;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Soft little sounds, synthesised at start-up rather than shipped as files: a blip per step,
 * a low "bonk" for a bump, a rising "ta-da" for the star. Kept quiet on purpose.
 */
final class Sounds {
    private static final int RATE = 22050;
    private final AudioTrack step, bump, win, tap;

    Sounds() {
        step = track(new float[]{660}, 0.07f, 0.22f);
        tap = track(new float[]{880}, 0.035f, 0.14f);
        bump = track(new float[]{196, 147}, 0.11f, 0.30f);
        win = track(new float[]{523.25f, 659.25f, 783.99f, 1046.5f}, 0.11f, 0.24f);
    }

    /** One short note per frequency, each with a soft attack and decay so nothing clicks. */
    private static AudioTrack track(float[] notes, float seconds, float volume) {
        int per = (int) (RATE * seconds);
        short[] pcm = new short[per * notes.length];
        for (int n = 0; n < notes.length; n++) {
            for (int i = 0; i < per; i++) {
                double t = i / (double) RATE;
                double env = Math.min(1, i / (RATE * 0.006)) * Math.pow(1 - i / (double) per, 2);
                double s = Math.sin(2 * Math.PI * notes[n] * t) + 0.3 * Math.sin(4 * Math.PI * notes[n] * t);
                pcm[n * per + i] = (short) (s / 1.3 * env * volume * Short.MAX_VALUE);
            }
        }
        try {
            AudioTrack a = new AudioTrack(AudioManager.STREAM_MUSIC, RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    pcm.length * 2, AudioTrack.MODE_STATIC);
            a.write(pcm, 0, pcm.length);
            return a;
        } catch (RuntimeException e) {
            return null; // no audio is fine; the game does not depend on it
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

    void step() {
        play(step);
    }

    void tap() {
        play(tap);
    }

    void bump() {
        play(bump);
    }

    void win() {
        play(win);
    }

    void release() {
        for (AudioTrack a : new AudioTrack[]{step, tap, bump, win}) if (a != null) a.release();
    }
}
