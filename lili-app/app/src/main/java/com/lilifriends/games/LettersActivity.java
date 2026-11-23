package com.lilifriends.games;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Lili's Letters: one lesson of «قراءتي الجديدة» per stop on the journey map. Lili says the
 * letter, the child traces it with a finger, then the child hears syllables and words in Lili's voice and picks the one written
 * the same way, and last reads the lesson's sentences with her. A wrong pick only wobbles and
 * Lili says «try again»; finishing the lesson opens the next stop.
 */
public class LettersActivity extends Activity {
    /** Intent extras, as for the other games: the progress file, and the lesson (0-based). */
    public static final String EXTRA_PREFS = "prefs", EXTRA_STAGE = "stage";

    private static final int ROUNDS = 6;
    private static final int INTRO = 0, SOUNDS = 1, WORDS = 2, READ = 3, DONE = 4, TRACE = 5, BUILD = 6, ORDER = 7;

    private SharedPreferences prefs;
    private Lessons book;
    private Lessons.Lesson lesson;
    private int stage;
    private final Random rnd = new Random();
    private MediaPlayer player;
    private LessonView view;

    // Where the lesson is.
    private final List<Integer> steps = new ArrayList<>();
    private int step, round, rounds, sentence;
    private List<Lessons.Item> pool = new ArrayList<>();
    /** The questions of this step, in the order they will be asked. */
    private final List<Lessons.Item> seq = new ArrayList<>();
    private Lessons.Item target;
    private final List<Lessons.Item> options = new ArrayList<>();
    private int wrongPick = -1, rightPick = -1;
    /** Building a word: its syllables shuffled as cards, and how many are placed so far. */
    private final List<String> cards = new ArrayList<>();
    private final List<Boolean> used = new ArrayList<>();
    private int placed;
    private long wobbleAt;
    private boolean busy;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String file = getIntent().getStringExtra(EXTRA_PREFS);
        prefs = getSharedPreferences(file != null ? file : "letters", MODE_PRIVATE);
        try {
            book = new Lessons(readAsset("letters/lessons.json"));
        } catch (Exception e) {
            finish();
            return;
        }
        int last = book.lessons.size() - 1;
        int furthest = Math.min(prefs.getInt("stage", 0), last);
        // Only the debug build exports this screen, for tools/screenshots.sh.
        if (getIntent().getBooleanExtra("unlock", false)) furthest = last;
        stage = Math.max(0, Math.min(getIntent().getIntExtra(EXTRA_STAGE, furthest), furthest));
        lesson = book.lessons.get(stage);

        steps.add(INTRO);
        if (lesson.title.startsWith("حرف")) steps.add(TRACE);
        if (lesson.sounds().size() >= 2) steps.add(SOUNDS);
        if (!lesson.words.isEmpty()) steps.add(WORDS);
        if (lesson.buildable().size() >= 2) steps.add(BUILD);
        if (!lesson.sentences.isEmpty()) steps.add(READ);
        if (!lesson.orderable().isEmpty()) steps.add(ORDER);
        steps.add(DONE);

        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Draw.SKY_TOP);
        TopBar bar = new TopBar(this, face, lesson.title, -1);
        bar.setOnBack(this::finish);
        root.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        view = new LessonView(this);
        root.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        startStep(Math.max(0, Math.min(getIntent().getIntExtra("step", 0), steps.size() - 1)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSound();
    }

    private String readAsset(String name) throws IOException {
        InputStream in = getAssets().open(name);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    // ---- Sound -------------------------------------------------------------------------------

    private void stopSound() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    /** Plays one of Lili's recordings; {@code after} runs when it ends (or at once if it can't). */
    private void play(String sound, Runnable after) {
        stopSound();
        if (sound == null) {
            if (after != null) after.run();
            return;
        }
        MediaPlayer mp = new MediaPlayer();
        try {
            AssetFileDescriptor fd = getAssets().openFd("letters/" + sound);
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            mp.setOnCompletionListener(m -> {
                if (player == m) {
                    m.release();
                    player = null;
                }
                if (after != null) after.run();
            });
            mp.prepare();
            player = mp;
            mp.start();
        } catch (IOException | RuntimeException e) {
            mp.release();
            if (after != null) after.run();
        }
    }

    private Lessons.Item any(List<Lessons.Item> list) {
        return list.isEmpty() ? null : list.get(rnd.nextInt(list.size()));
    }

    // ---- Lesson flow -------------------------------------------------------------------------

    private int kind() {
        return steps.get(step);
    }

    private void startStep(int s) {
        step = s;
        round = 0;
        sentence = 0;
        busy = false;
        switch (kind()) {
            case TRACE:
                view.clearTrace();
                play(book.phrase("اُرْسُمِ الْحَرْف"), null);
                break;
            case INTRO:
                view.invalidate();
                play(lesson.intro.isEmpty() ? null : lesson.intro.get(0).sound, null);
                break;
            case SOUNDS:
                pool = lesson.sounds();
                rounds = Math.min(ROUNDS, pool.size());
                deal();
                play(book.phrase("اِسْمَعْ وَاخْتَرْ"), this::newRound);
                break;
            case WORDS:
                pool = new ArrayList<>(lesson.words);
                rounds = Math.min(ROUNDS, pool.size());
                deal();
                play(book.phrase("اِسْمَعْ وَاخْتَرْ"), this::newRound);
                break;
            case BUILD:
                pool = lesson.buildable();
                rounds = Math.min(4, pool.size());
                deal();
                play(book.phrase("رَكِّبِ الْمَقَاطِع"), this::newBuild);
                break;
            case ORDER:
                pool = lesson.orderable();
                rounds = Math.min(3, pool.size());
                deal();
                newBuild();
                break;
            case READ:
                view.invalidate();
                play(book.phrase("اِقْرَأْ مَعِي"), () -> play(lesson.sentences.get(0).sound, null));
                break;
            default:
                int reached = prefs.getInt("stage", 0);
                if (stage + 1 > reached) prefs.edit().putInt("stage", stage + 1).apply();
                view.invalidate();
                Lessons.Item ok = any(book.praise);
                play(ok != null ? ok.sound : null, null);
        }
        view.invalidate();
    }

    private void nextStep() {
        if (step + 1 < steps.size()) startStep(step + 1);
        else finish();
    }

    private void deal() {
        seq.clear();
        seq.addAll(pool);
        Collections.shuffle(seq, rnd);
    }

    /** A new question: three cards, one of them the one Lili says. */
    private void newRound() {
        wrongPick = -1;
        rightPick = -1;
        busy = false;
        List<Lessons.Item> order = new ArrayList<>(pool);
        Collections.shuffle(order, rnd);
        target = seq.get(round % seq.size());
        options.clear();
        options.add(target);
        for (Lessons.Item it : order) {
            if (options.size() >= 3) break;
            if (!it.text.equals(target.text) && !hasText(it.text)) options.add(it);
        }
        // A lesson with only two sounds or words borrows a card from an earlier lesson.
        for (int k = stage - 1; options.size() < 3 && k >= 0; k--) {
            Lessons.Lesson l = book.lessons.get(k);
            Lessons.Item it = any(kind() == SOUNDS ? l.sounds() : l.words);
            if (it != null && !hasText(it.text)) options.add(it);
        }
        Collections.shuffle(options, rnd);
        view.invalidate();
        play(target.sound, null);
    }

    /** A new word to build: Lili says it, and its syllables wait as shuffled cards. */
    private void newBuild() {
        busy = false;
        wrongPick = -1;
        target = seq.get(round % seq.size());
        cards.clear();
        used.clear();
        placed = 0;
        Collections.addAll(cards, target.parts);
        // Shuffle until the cards are not already in order (when that is possible).
        for (int t = 0; t < 8; t++) {
            Collections.shuffle(cards, rnd);
            boolean same = true;
            for (int i = 0; i < cards.size(); i++) if (!cards.get(i).equals(target.parts[i])) same = false;
            if (!same) break;
        }
        for (int i = 0; i < cards.size(); i++) used.add(false);
        view.invalidate();
        play(target.sound, null);
    }

    private void place(int i) {
        if (busy || i < 0 || i >= cards.size() || used.get(i)) return;
        if (cards.get(i).equals(target.parts[placed])) {
            used.set(i, true);
            placed++;
            wrongPick = -1;
            view.invalidate();
            if (placed == target.parts.length) {
                busy = true;
                play(target.sound, () -> {
                    Lessons.Item ok = any(book.praise);
                    play(ok != null ? ok.sound : null, () -> {
                        round++;
                        if (round < rounds) newBuild();
                        else nextStep();
                    });
                });
            }
        } else {
            wrongPick = i;
            wobbleAt = SystemClock.uptimeMillis();
            view.invalidate();
            Lessons.Item again = any(book.tryAgain);
            play(again != null ? again.sound : null, null);
        }
    }

    private boolean hasText(String t) {
        for (Lessons.Item it : options) if (it.text.equals(t)) return true;
        return false;
    }

    private void pick(int i) {
        if (busy || i < 0 || i >= options.size()) return;
        if (options.get(i) == target) {
            busy = true;
            rightPick = i;
            view.invalidate();
            Lessons.Item ok = any(book.praise);
            play(ok != null ? ok.sound : null, () -> {
                round++;
                if (round < rounds) newRound();
                else nextStep();
            });
        } else {
            wrongPick = i;
            wobbleAt = SystemClock.uptimeMillis();
            view.invalidate();
            Lessons.Item again = any(book.tryAgain);
            play(again != null ? again.sound : null, () -> play(target.sound, null));
        }
    }

    private void nextSentence() {
        if (sentence + 1 < lesson.sentences.size()) {
            sentence++;
            view.invalidate();
            play(lesson.sentences.get(sentence).sound, null);
        } else {
            nextStep();
        }
    }

    // ---- Screen ------------------------------------------------------------------------------

    private class LessonView extends View {
        private final float dp;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint para = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap lili;
        /** Buttons on screen now: left, top, right, bottom, and what they do (index or code). */
        private final List<float[]> boxes = new ArrayList<>();
        private int down = -1;
        private static final int B_SPEAK = -10, B_NEXT = -11, B_CLEAR = -12;
        // Tracing: the letter's ink as a mask, the child's finger path, and where it went.
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path finger = new Path();
        private final List<float[]> touches = new ArrayList<>();
        private float[] traceBox;
        private float[] samples = new float[0];
        private int sampleCount;
        private boolean traced, tracing;

        LessonView(Context ctx) {
            super(ctx);
            dp = getResources().getDisplayMetrics().density;
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            para.setTypeface(Typeface.DEFAULT_BOLD);
            para.setColor(Draw.PLUM);
            lili = Draw.load(getResources(), R.drawable.home_happy);
        }

        private void box(float l, float t, float r, float b, int what) {
            boxes.add(new float[]{l, t, r, b, what});
        }

        void clearTrace() {
            finger.reset();
            touches.clear();
            traced = false;
            invalidate();
        }

        /** Points inside the letter's ink, found once from an off-screen copy of the letter. */
        private void buildSamples(float l, float t, float r, float b) {
            int bw = Math.max(1, Math.round(r - l)), bh = Math.max(1, Math.round(b - t));
            Bitmap m = Bitmap.createBitmap(bw, bh, Bitmap.Config.ALPHA_8);
            Canvas mc = new Canvas(m);
            letterPaint(bw, bh);
            mc.drawText(lesson.big, bw / 2f, bh / 2f - (ink.ascent() + ink.descent()) / 2f, ink);
            int stepPx = Math.max(3, Math.round(Math.min(bw, bh) / 70f));
            float[] pts = new float[(bw / stepPx + 1) * (bh / stepPx + 1) * 2];
            int n = 0;
            for (int y = 0; y < bh; y += stepPx) {
                for (int x = 0; x < bw; x += stepPx) {
                    if (Color.alpha(m.getPixel(x, y)) > 128) {
                        pts[n++] = l + x;
                        pts[n++] = t + y;
                    }
                }
            }
            m.recycle();
            samples = pts;
            sampleCount = n / 2;
        }

        private void letterPaint(float bw, float bh) {
            ink.setTypeface(Typeface.DEFAULT_BOLD);
            ink.setTextAlign(Paint.Align.CENTER);
            ink.setStyle(Paint.Style.FILL);
            ink.setTextSize(Math.min(bw, bh) * 0.8f);
            float tw = ink.measureText(lesson.big);
            if (tw > bw * 0.9f && tw > 0) ink.setTextSize(ink.getTextSize() * bw * 0.9f / tw);
        }

        /** Share of the letter's ink that the finger has passed over. */
        private float coverage() {
            if (sampleCount == 0) return 0;
            float rad = 22 * dp, r2 = rad * rad;
            int hitCount = 0;
            for (int i = 0; i < sampleCount; i++) {
                float sx = samples[2 * i], sy = samples[2 * i + 1];
                for (float[] p : touches) {
                    float dx = p[0] - sx, dy = p[1] - sy;
                    if (dx * dx + dy * dy <= r2) {
                        hitCount++;
                        break;
                    }
                }
            }
            return hitCount / (float) sampleCount;
        }

        private boolean inTrace(float x, float y) {
            return traceBox != null && x >= traceBox[0] && x <= traceBox[2] && y >= traceBox[1] && y <= traceBox[3];
        }

        private boolean traceTouch(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (!inTrace(x, y) || traced) return false;
                    tracing = true;
                    finger.moveTo(x, y);
                    touches.add(new float[]{x, y});
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!tracing) return false;
                    finger.lineTo(x, y);
                    touches.add(new float[]{x, y});
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!tracing) return false;
                    tracing = false;
                    if (coverage() >= 0.72f) {
                        traced = true;
                        Lessons.Item ok = any(book.praise);
                        play(ok != null ? ok.sound : null, null);
                    }
                    invalidate();
                    return true;
            }
            return false;
        }

        private int hit(float x, float y) {
            for (float[] b : boxes) {
                if (x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]) return (int) b[4];
            }
            return Integer.MIN_VALUE;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (kind() == TRACE && (tracing || e.getActionMasked() == MotionEvent.ACTION_DOWN) && traceTouch(e)) return true;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down = hit(e.getX(), e.getY());
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    int w = down;
                    down = -1;
                    invalidate();
                    if (w != Integer.MIN_VALUE && hit(e.getX(), e.getY()) == w) act(w);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    down = -1;
                    invalidate();
                    return true;
            }
            return true;
        }

        private void act(int w) {
            int k = kind();
            if (w == B_CLEAR) {
                clearTrace();
            } else if (w == B_SPEAK) {
                if (k == TRACE) play(lesson.intro.isEmpty() ? null : lesson.intro.get(0).sound, null);
                else if (k == INTRO && !lesson.intro.isEmpty()) play(lesson.intro.get(0).sound, null);
                else if ((k == SOUNDS || k == WORDS || k == BUILD || k == ORDER) && target != null) play(target.sound, null);
                else if (k == READ) play(lesson.sentences.get(sentence).sound, null);
            } else if (w == B_NEXT) {
                if (k == READ) nextSentence();
                else nextStep();
            } else if (w >= 0) {
                if (k == BUILD || k == ORDER) place(w);
                else pick(w);
            }
        }

        /** Draws {@code s} centred on (cx, cy), shrinking it to fit {@code maxW}. */
        private void label(Canvas c, String s, float cx, float cy, float size, float maxW, int color) {
            text.setTextSize(size);
            float tw = text.measureText(s);
            if (tw > maxW && tw > 0) text.setTextSize(size * maxW / tw);
            text.setColor(color);
            c.drawText(s, cx, cy - (text.ascent() + text.descent()) / 2f, text);
        }

        private void button(Canvas c, float l, float t, float r, float b, int fill, int deep, int what) {
            float s = down == what ? 0.94f : 1f;
            c.save();
            c.scale(s, s, (l + r) / 2, (t + b) / 2);
            Draw.card(c, l, t, r, b - 6 * dp, 20 * dp, fill, deep, 6 * dp);
            c.restore();
            box(l, t, r, b, what);
        }

        /** The round speaker button that plays the sound again. */
        private void speaker(Canvas c, float cx, float cy, float r) {
            button(c, cx - r, cy - r, cx + r, cy + r, Draw.YELLOW, Draw.YELLOW_D, B_SPEAK);
            float u = r / 6f, y = cy - 3 * dp;
            c.drawRect(cx - 3.2f * u, y - 1.3f * u, cx - 1.6f * u, y + 1.3f * u, Draw.fill(Draw.PLUM));
            android.graphics.Path p = new android.graphics.Path();
            p.moveTo(cx - 1.8f * u, y - 1.3f * u);
            p.lineTo(cx + 0.6f * u, y - 3.0f * u);
            p.lineTo(cx + 0.6f * u, y + 3.0f * u);
            p.lineTo(cx - 1.8f * u, y + 1.3f * u);
            p.close();
            c.drawPath(p, Draw.fill(Draw.PLUM));
            c.drawArc(new android.graphics.RectF(cx + 0.2f * u, y - 2.2f * u, cx + 2.6f * u, y + 2.2f * u),
                    -50, 100, false, Draw.stroke(Draw.PLUM, 0.6f * u));
        }

        private void next(Canvas c, float cx, float cy, float w, float h, String s) {
            button(c, cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, Draw.MINT, Draw.MINT_D, B_NEXT);
            label(c, s, cx, cy - 3 * dp, h * 0.42f, w * 0.8f, 0xFFFFFFFF);
        }

        @Override
        protected void onDraw(Canvas c) {
            boxes.clear();
            float w = getWidth(), h = getHeight(), pad = 18 * dp;
            c.drawColor(Draw.SKY_TOP);
            int k = kind();
            float btnH = Math.max(64 * dp, Math.min(84 * dp, h * 0.11f));
            if (k == TRACE) {
                btnH = Math.min(btnH, (w - 2 * pad) / 4.9f);
                float bottomRow = h - pad - btnH / 2;
                float l = pad, t = pad, r = w - pad, b = h - 2 * pad - btnH;
                if (traceBox == null || traceBox[2] != r || traceBox[3] != b) {
                    traceBox = new float[]{l, t, r, b};
                    buildSamples(l, t, r, b);
                }
                Draw.card(c, l, t, r, b - 8 * dp, 32 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                letterPaint(r - l, b - t);
                ink.setColor(traced ? Draw.MINT : 0xFFE9E1F5);
                c.drawText(lesson.big, (l + r) / 2f, (t + b) / 2f - (ink.ascent() + ink.descent()) / 2f, ink);
                Paint pen = Draw.stroke(traced ? Draw.MINT_D : Draw.PINK_D, 18 * dp);
                c.drawPath(finger, pen);
                // One row: clear (start side), next (middle), speaker (end side).
                if (!traced) {
                    float cx = w / 2 - 1.9f * btnH;
                    button(c, cx - 0.65f * btnH, bottomRow - btnH / 2, cx + 0.65f * btnH, bottomRow + btnH / 2,
                            Draw.BLUE, Draw.BLUE_D, B_CLEAR);
                    label(c, "اِمْسَحْ", cx, bottomRow - 3 * dp, btnH * 0.32f, btnH * 1.1f, 0xFFFFFFFF);
                }
                speaker(c, w / 2 + 1.7f * btnH, bottomRow, btnH / 2);
                next(c, w / 2, bottomRow, btnH * 1.9f, btnH, traced ? "هَيَّا" : "التَّالِي");
            } else if (k == INTRO) {
                float size = Math.min(w - 2 * pad, h * 0.5f);
                float cx = w / 2, top = pad;
                Draw.card(c, cx - size / 2, top, cx + size / 2, top + size - 8 * dp, 40 * dp, Draw.PINK, Draw.PINK_D, 8 * dp);
                label(c, lesson.big, cx, top + size / 2 - 4 * dp, size * 0.62f, size * 0.85f, 0xFFFFFFFF);
                float y = top + size + pad;
                String say = lesson.intro.isEmpty() ? lesson.title : lesson.intro.get(0).text;
                label(c, say, cx, y + 22 * dp, 30 * dp, w - 2 * pad, Draw.PLUM);
                float row = y + 44 * dp + pad + btnH / 2;
                speaker(c, cx + btnH * 0.9f, row, btnH / 2);
                next(c, cx - btnH * 1.1f, row, btnH * 2.2f, btnH, "هَيَّا");
            } else if (k == SOUNDS || k == WORDS) {
                boolean side = w > h * 1.2f;
                label(c, (round + 1) + " / " + rounds, w / 2, pad + 12 * dp, 18 * dp, w, 0xFF9AA0BC);
                float sr = side ? 34 * dp : Math.max(40 * dp, Math.min(56 * dp, h * 0.08f));
                if (side) speaker(c, pad + sr, pad + sr, sr);
                else speaker(c, w / 2, pad + 40 * dp + sr, sr);
                float top = side ? pad + 2 * sr + pad : pad + 60 * dp + 2 * sr + pad;
                boolean column = w < h * 0.8f && k == WORDS;
                int n = options.size();
                float wob = 0;
                long since = SystemClock.uptimeMillis() - wobbleAt;
                if (wrongPick >= 0 && since < 450) {
                    wob = (float) Math.sin(since / 30.0) * 10 * dp * (1 - since / 450f);
                    postInvalidateOnAnimation();
                }
                for (int i = 0; i < n; i++) {
                    float l, t, r, b;
                    if (column) {
                        float ch = Math.min((h - top - pad) / n - pad, 130 * dp);
                        l = pad;
                        r = w - pad;
                        t = top + i * (ch + pad);
                        b = t + ch;
                    } else {
                        float cw = (w - pad * (n + 1)) / n;
                        float ch = Math.min(h - top - pad, Math.max(120 * dp, cw * 0.9f));
                        int col = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? n - 1 - i : i;
                        l = pad + col * (cw + pad);
                        r = l + cw;
                        t = top;
                        b = top + ch;
                    }
                    int fill = Draw.LILAC, deep = Draw.LILAC_D;
                    if (i == rightPick) {
                        fill = Draw.MINT;
                        deep = Draw.MINT_D;
                    } else if (i == wrongPick) {
                        fill = 0xFFFFB3B3;
                        deep = 0xFFE07A7A;
                    }
                    c.save();
                    if (i == wrongPick) c.translate(wob, 0);
                    button(c, l, t, r, b, fill, deep, i);
                    label(c, options.get(i).text, (l + r) / 2, (t + b) / 2 - 3 * dp,
                            Math.min((b - t) * 0.5f, 72 * dp), (r - l) * 0.85f, 0xFFFFFFFF);
                    c.restore();
                }
            } else if (k == BUILD || k == ORDER) {
                label(c, (round + 1) + " / " + rounds, w / 2, pad + 12 * dp, 18 * dp, w, 0xFF9AA0BC);
                boolean side = w > h * 1.2f;
                if (k == ORDER && !side) label(c, "رَتِّبِ الْكَلِمَاتِ", w / 2, pad + 40 * dp, 22 * dp, w, Draw.PLUM);
                float sr = side ? 30 * dp : Math.max(36 * dp, Math.min(52 * dp, h * 0.07f));
                float spT = side ? pad : pad + (k == ORDER ? 60 : 40) * dp;
                if (side) speaker(c, pad + sr, pad + sr, sr);
                else speaker(c, w / 2, spT + sr, sr);
                if (side) spT = pad + 6 * dp - 2 * sr;
                if (target != null && target.parts != null) {
                    int n = cards.size();
                    int cols = n <= 3 ? n : (n + 1) / 2, rowsN = (n + cols - 1) / cols;
                    float cw = Math.min(170 * dp, (w - pad * (cols + 1)) / cols);
                    float avail = (h - spT - 2 * sr - 4 * pad) / 2f;
                    float ch = Math.max(40 * dp, Math.min(110 * dp, (avail - (rowsN - 1) * pad) / rowsN));
                    float rowW = cols * cw + (cols - 1) * pad, x0 = (w - rowW) / 2;
                    float slotT = spT + 2 * sr + pad;
                    // The slots, filled in reading order: right to left, then the next row.
                    for (int s = 0; s < n; s++) {
                        int col = s % cols, row = s / cols;
                        float l = x0 + (cols - 1 - col) * (cw + pad), r = l + cw;
                        float t = slotT + row * (ch + pad), b = t + ch;
                        if (s < placed) {
                            Draw.card(c, l, t, r, b - 6 * dp, 20 * dp, Draw.MINT, Draw.MINT_D, 6 * dp);
                            label(c, target.parts[s], (l + r) / 2, (t + b) / 2 - 3 * dp, ch * 0.4f, cw * 0.85f, 0xFFFFFFFF);
                        } else {
                            c.drawRoundRect(new android.graphics.RectF(l, t, r, b - 6 * dp), 20 * dp, 20 * dp,
                                    Draw.stroke(0xFFC9CDE0, 3 * dp));
                        }
                    }
                    float cardT = slotT + rowsN * (ch + pad) + pad;
                    float wob = 0;
                    long since = SystemClock.uptimeMillis() - wobbleAt;
                    if (wrongPick >= 0 && since < 450) {
                        wob = (float) Math.sin(since / 30.0) * 10 * dp * (1 - since / 450f);
                        postInvalidateOnAnimation();
                    }
                    for (int i = 0; i < n; i++) {
                        if (used.get(i)) continue;
                        int col = i % cols, row = i / cols;
                        float l = x0 + (cols - 1 - col) * (cw + pad), r = l + cw;
                        float t = cardT + row * (ch + pad);
                        c.save();
                        if (i == wrongPick) c.translate(wob, 0);
                        button(c, l, t, r, t + ch, i == wrongPick ? 0xFFFFB3B3 : Draw.LILAC,
                                i == wrongPick ? 0xFFE07A7A : Draw.LILAC_D, i);
                        label(c, cards.get(i), (l + r) / 2, t + ch / 2 - 3 * dp, ch * 0.4f, cw * 0.85f, 0xFFFFFFFF);
                        c.restore();
                    }
                }
            } else if (k == READ) {
                Lessons.Item s = lesson.sentences.get(sentence);
                label(c, (sentence + 1) + " / " + lesson.sentences.size(), w / 2, pad + 12 * dp, 18 * dp, w, 0xFF9AA0BC);
                float cardT = pad + 32 * dp, cardB = h - pad - btnH - pad;
                Draw.card(c, pad, cardT, w - pad, cardB - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                para.setTextSize(Math.min(56 * dp, Math.min(w, h) * 0.1f));
                int lw = (int) (w - 4 * pad);
                StaticLayout sl = StaticLayout.Builder.obtain(s.text, 0, s.text.length(), para, lw)
                        .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0, 1.25f).build();
                c.save();
                c.translate(2 * pad, cardT + Math.max(pad, (cardB - cardT - sl.getHeight()) / 2f));
                sl.draw(c);
                c.restore();
                float row = h - pad - btnH / 2;
                speaker(c, w / 2 + btnH * 0.9f, row, btnH / 2);
                next(c, w / 2 - btnH * 1.1f, row, btnH * 2.2f, btnH, "التَّالِي");
            } else {
                float ph = Math.min(h * 0.45f, w * 0.6f);
                Draw.picture(c, lili, w / 2 - ph / 2, pad, w / 2 + ph / 2, pad + ph);
                float y = pad + ph + pad;
                for (int i = 0; i < 3; i++) Draw.star(c, w / 2 + (i - 1) * 54 * dp, y + 26 * dp, 22 * dp, Draw.YELLOW_D);
                label(c, "أَحْسَنْتَ!", w / 2, y + 90 * dp, 40 * dp, w - 2 * pad, Draw.PLUM);
                next(c, w / 2, y + 150 * dp + btnH / 2, btnH * 2.4f, btnH, "تَمّ");
            }
        }
    }
}
