package io.github.foadsf.kangaroohops;

import android.content.Context;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Counts aloud. In order of preference:
 *   1. a recording by her family: voices/<lang>/<n>.<m4a|3ga|ogg|mp3|wav> in the app's own folder
 *      on the phone (Android/data/io.github.foadsf.kangaroohops/files/voices/). That folder needs
 *      no storage permission. A README.txt there says what to put where.
 *   2. the device's speech engine, if it has one for that language
 *   3. nothing: the game still shows the number big and plays a note
 */
final class Voice implements TextToSpeech.OnInitListener {
    private static final String[] EXT = {"m4a", "3ga", "ogg", "mp3", "wav", "aac", "amr"};
    private final File root;
    private TextToSpeech tts;
    private boolean ttsReady;
    private MediaPlayer player;
    private Runnable onReady;

    /** Runs (on the main thread) once the speech engine has finished starting, or failed to. */
    void setOnReady(Runnable r) {
        onReady = r;
    }

    Voice(Context ctx) {
        File base = ctx.getExternalFilesDir(null);
        root = base == null ? null : new File(base, "voices");
        if (root != null) {
            for (String l : Numbers.LANGS) //noinspection ResultOfMethodCallIgnored
                new File(root, l).mkdirs();
            File readme = new File(root, "README.txt");
            if (!readme.exists()) {
                try (FileOutputStream out = new FileOutputStream(readme)) {
                    out.write(("Kangaroo Hops counts aloud with YOUR voice if you put recordings here.\n"
                            + "One short file per number, named by the number, in the folder for its language:\n"
                            + "  fa/0.m4a fa/1.m4a ... fa/20.m4a   (Farsi: sefr, yek, do ...)\n"
                            + "  nl/0.m4a ... nl/20.m4a            (Dutch: nul, een, twee ...)\n"
                            + "  en/0.m4a ... en/20.m4a            (English: zero, one, two ...)\n"
                            + "m4a, 3ga (Samsung Voice Recorder), ogg, mp3, wav, aac and amr all work.\n"
                            + "Missing numbers fall back to the phone's speech engine, or to a note.\n")
                            .getBytes(Charset.forName("UTF-8")));
                } catch (IOException ignored) {
                }
            }
        }
        try {
            tts = new TextToSpeech(ctx, this);
        } catch (RuntimeException e) {
            tts = null;
        }
    }

    @Override
    public void onInit(int status) {
        ttsReady = status == TextToSpeech.SUCCESS;
        if (onReady != null) onReady.run();  // TextToSpeech calls onInit on the main thread
    }

    private File recording(String lang, int n) {
        if (root == null) return null;
        for (String e : EXT) {
            File f = new File(root, lang + "/" + n + "." + e);
            if (f.isFile() && f.length() > 0) return f;
        }
        return null;
    }

    private boolean ttsCan(String lang) {
        if (!ttsReady || tts == null) return false;
        try {
            return tts.isLanguageAvailable(new Locale(lang)) >= TextToSpeech.LANG_AVAILABLE;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** "recorded", "speech" or "none": what {@link #say} will use for this language. */
    String source(String lang) {
        if (recording(lang, 1) != null) return "recorded";
        return ttsCan(lang) ? "speech" : "none";
    }

    /** Says {@code n} in {@code lang}. Returns false if there is no way to (the caller shows it). */
    @SuppressWarnings("deprecation") // speak(String, int, HashMap) is the API 19 form
    boolean say(String lang, int n) {
        File f = recording(lang, n);
        if (f != null) {
            stop();
            try {
                MediaPlayer p = new MediaPlayer();
                p.setDataSource(f.getAbsolutePath());
                p.setOnCompletionListener(MediaPlayer::release);
                p.prepare();
                p.start();
                player = p;
                return true;
            } catch (IOException | RuntimeException e) {
                // fall through to speech
            }
        }
        if (ttsCan(lang)) {
            tts.setLanguage(new Locale(lang));
            tts.speak(Numbers.word(lang, n), TextToSpeech.QUEUE_FLUSH, null);
            return true;
        }
        return false;
    }

    private void stop() {
        if (player != null) {
            try {
                player.release();
            } catch (RuntimeException ignored) {
            }
            player = null;
        }
    }

    void release() {
        stop();
        if (tts != null) tts.shutdown();
    }
}
