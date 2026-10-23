package io.github.foadsf.patternmachine;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pattern Machine: what comes next? The machine shows a row like ● ▲ ● ▲ ● ? and she picks the
 * token that fits. Every token has its own note, so a pattern is also a tune, and the machine
 * plays it back when she gets it right.
 *
 * A wrong pick wobbles and greys out, so she narrows it down. After two wrong picks the repeats
 * are underlined. Five puzzles make a round and earn a sticker; a round with at most two wrong
 * picks moves her up a stage (see {@link Puzzle} for the stages).
 *
 * In Make mode she builds her own row; Play sings it, and if her row repeats, the machine finds
 * the rule and carries on with it.
 */
public class GameActivity extends Activity {
    private static final String PREFS = "progress";
    private static final int PER_ROUND = 5, MAX_SLIPS_TO_ADVANCE = 2, MAKE_MAX = 8;
    /** The tokens offered in Make mode: one of each shape, each in its own colour. */
    private static final int[] PALETTE = {
            Tokens.token(Tokens.CIRCLE, 0), Tokens.token(Tokens.TRIANGLE, 2),
            Tokens.token(Tokens.SQUARE, 1), Tokens.token(Tokens.STAR, 3),
            Tokens.token(Tokens.HEART, 4), Tokens.token(Tokens.DIAMOND, 5)};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private SharedPreferences prefs;
    private Sounds sounds;
    private Typeface face;
    private Screen screen;
    private MachineView machine;
    private TextView title;
    private CandyButton home, make, undo, play;
    private final List<CandyButton> choices = new ArrayList<>(), paletteButtons = new ArrayList<>();
    private StickerView sticker;
    private LinearLayout stickerButtons;

    private Puzzle puzzle;
    private int gapIndex, slips, roundDone, roundSlips;
    private boolean busy, making;
    private final List<Integer> made = new ArrayList<>();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        // In the Lili & Friends app each child has their own progress file, and the journey map
        // passes the stage to play; on its own the game keeps one file and resumes.
        String file = getIntent().getStringExtra(EXTRA_PREFS);
        prefs = getSharedPreferences(file != null ? file : PREFS, MODE_PRIVATE);
        playStage = Math.max(0, Math.min(getIntent().getIntExtra(EXTRA_STAGE, prefs.getInt("stage", 0)),
                prefs.getInt("stage", 0)));
        sounds = new Sounds();
        Art.load(getResources());
        face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");
        Resources res = getResources();

        screen = new Screen(this);
        screen.addView(new Backdrop(this));
        title = new TextView(this);
        title.setTypeface(face);
        title.setTextColor(Art.PLUM);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setText(R.string.pattern_title);
        screen.addView(title);
        home = new CandyButton(this, (c, x, y, s) -> Art.house(c, x, y, s, 0xFFFFFFFF),
                Art.PINK, Art.PINK_D, null, face);
        home.setContentDescription(res.getString(R.string.cd_home));
        home.setOnClickListener(v -> {
            if (making) leaveMake();
            else finish();
        });
        screen.addView(home);
        make = new CandyButton(this, (c, x, y, s) -> Art.palette(c, x, y, s, 0xFFFFE2B8),
                Art.PEACH, Art.PEACH_D, null, face);
        make.setContentDescription(res.getString(R.string.cd_make));
        make.setOnClickListener(v -> enterMake());
        screen.addView(make);
        machine = new MachineView(this);
        screen.addView(machine);

        for (final int t : PALETTE) {
            CandyButton b = new CandyButton(this, (c, x, y, s) -> Art.token(c, x, y, s * 1.2f, t, 255),
                    0xFFFFFFFF, 0xFFD9D2F0, null, face);
            b.setContentDescription("add " + Tokens.name(t));
            b.setOnClickListener(v -> addToMade(t));
            b.setVisibility(View.GONE);
            paletteButtons.add(b);
            screen.addView(b);
        }
        undo = new CandyButton(this, (c, x, y, s) -> Art.undo(c, x, y, s * 0.9f, 0xFFFFFFFF),
                0xFFC9CDE0, 0xFF9AA0BC, null, face);
        undo.setContentDescription(res.getString(R.string.cd_undo));
        undo.setOnClickListener(v -> {
            if (!made.isEmpty() && !busy) {
                made.remove(made.size() - 1);
                showMade();
            }
        });
        play = new CandyButton(this, (c, x, y, s) -> Art.play(c, x, y, s, 0xFFFFFFFF),
                Art.MINT, Art.MINT_D, res.getString(R.string.play), face);
        play.setContentDescription(res.getString(R.string.play));
        play.setOnClickListener(v -> playMade());
        undo.setVisibility(View.GONE);
        play.setVisibility(View.GONE);
        screen.addView(undo);
        screen.addView(play);

        FrameLayout root = new FrameLayout(this);
        root.addView(screen);
        sticker = new StickerView(this, face);
        sticker.setVisibility(View.GONE);
        root.addView(sticker);
        stickerButtons = stickerButtons(this, res);
        root.addView(stickerButtons, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        ((FrameLayout.LayoutParams) stickerButtons.getLayoutParams()).bottomMargin =
                Math.round(40 * res.getDisplayMetrics().density);
        setContentView(frame(root));
        newPuzzle();
    }

    private LinearLayout stickerButtons(Context ctx, Resources res) {
        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER);
        float d = res.getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(150 * d), Math.round(72 * d));
        int m = Math.round(10 * d);
        lp.setMargins(m, 0, m, 0);
        CandyButton more = new CandyButton(ctx, null, Art.MINT, Art.MINT_D, res.getString(R.string.more), face);
        more.setContentDescription(res.getString(R.string.more));
        more.setOnClickListener(v -> {
            hideSticker();
            newPuzzle();
        });
        CandyButton done = new CandyButton(ctx, null, Art.PEACH, Art.PEACH_D, res.getString(R.string.done), face);
        done.setContentDescription(res.getString(R.string.done));
        done.setOnClickListener(v -> finish());
        row.addView(more, lp);
        row.addView(done, new LinearLayout.LayoutParams(lp));
        row.setVisibility(View.GONE);
        return row;
    }

    // ---- puzzles ------------------------------------------------------------------------

    /** Intent extras: the progress file to use, and the stage to play (at most the furthest). */
    public static final String EXTRA_PREFS = "prefs", EXTRA_STAGE = "stage";
    /** The stage being played now; prefs "stage" is the furthest one reached. */
    private int playStage;

    private int stage() {
        return playStage;
    }

    /** Moves on to {@code stage} and remembers it if it is further than before. */
    private void reachStage(int stage) {
        playStage = stage;
        if (stage > prefs.getInt("stage", 0)) prefs.edit().putInt("stage", stage).apply();
    }

    /** The note a token sings: by colour when only the colour changes, else by shape. */
    private int pitch(int t) {
        return puzzle != null && puzzle.kind == 5 && !making ? Tokens.colourOf(t) : Tokens.shapeOf(t);
    }

    private void newPuzzle() {
        puzzle = Puzzle.make(stage(), rnd);
        int[] shown = puzzle.seq.clone();
        for (int g : puzzle.gaps) shown[g] = MachineView.EMPTY;
        machine.setRow(shown, Integer.MAX_VALUE);
        machine.setState("stage " + stage() + " puzzle " + (roundDone + 1));
        gapIndex = 0;
        slips = 0;
        busy = false;
        for (CandyButton b : choices) screen.removeView(b);
        choices.clear();
        for (final int t : puzzle.choices) {
            final CandyButton b = new CandyButton(this, (c, x, y, s) -> Art.token(c, x, y, s * 1.25f, t, 255),
                    0xFFFFFFFF, 0xFFD9D2F0, null, face);
            b.setContentDescription("choice " + Tokens.name(t));
            b.setOnClickListener(v -> pick(t, b));
            choices.add(b);
            screen.addView(b);
        }
        screen.requestLayout();
    }

    private void pick(int t, CandyButton b) {
        if (busy || making) return;
        int want = puzzle.seq[puzzle.gaps[gapIndex]];
        if (t != want) {
            slips++;
            roundSlips++;
            sounds.boop();
            b.shake();
            b.setEnabled(false);
            machine.setFace(Art.FACE_OOPS);
            handler.postDelayed(() -> machine.setFace(Art.FACE_CALM), 700);
            if (slips >= 2) machine.showUnits(puzzle.unit);
            return;
        }
        sounds.note(pitch(t));
        machine.fill(puzzle.gaps[gapIndex], t);
        gapIndex++;
        for (CandyButton c : choices) c.setEnabled(true);  // a fresh start for the next gap
        if (gapIndex < puzzle.gaps.length) return;

        // Solved: the machine sings the whole row, then on to the next one.
        busy = true;
        for (CandyButton c : choices) c.setEnabled(false);
        machine.setFace(Art.FACE_HAPPY);
        machine.setState("solved");
        handler.postDelayed(() -> machine.playAll(tok -> sounds.note(pitch(tok)), 260, () -> {
            sounds.fanfare();
            roundDone++;
            int stars = prefs.getInt("stars", 0) + 1;
            prefs.edit().putInt("stars", stars).apply();
            handler.postDelayed(this::afterSolved, 900);
        }), 450);
    }

    private void afterSolved() {
        if (roundDone < PER_ROUND) {
            newPuzzle();
            return;
        }
        int stage = stage();
        if (roundSlips <= MAX_SLIPS_TO_ADVANCE) stage++;
        int stickers = prefs.getInt("stickers", 0) + 1;
        reachStage(stage);
        prefs.edit().putInt("stickers", stickers).apply();
        roundDone = 0;
        roundSlips = 0;
        sticker.show(stickers);
        stickerButtons.setVisibility(View.VISIBLE);
    }

    private void hideSticker() {
        sticker.setVisibility(View.GONE);
        stickerButtons.setVisibility(View.GONE);
    }

    // ---- make your own --------------------------------------------------------------------

    private void enterMake() {
        if (busy) return;
        making = true;
        holdBack(true);
        made.clear();
        title.setText(R.string.make_title);
        make.setVisibility(View.GONE);
        for (CandyButton b : choices) b.setVisibility(View.GONE);
        for (CandyButton b : paletteButtons) b.setVisibility(View.VISIBLE);
        undo.setVisibility(View.VISIBLE);
        play.setVisibility(View.VISIBLE);
        showMade();
    }

    private void leaveMake() {
        machine.cancel();
        making = false;
        holdBack(false);
        busy = false;
        title.setText(R.string.pattern_title);
        make.setVisibility(View.VISIBLE);
        for (CandyButton b : paletteButtons) b.setVisibility(View.GONE);
        undo.setVisibility(View.GONE);
        play.setVisibility(View.GONE);
        newPuzzle();
    }

    private void showMade() {
        int[] row = new int[made.size()];
        for (int i = 0; i < row.length; i++) row[i] = made.get(i);
        machine.setRow(row, Integer.MAX_VALUE);
        machine.setState("make");
        boolean room = made.size() < MAKE_MAX;
        for (CandyButton b : paletteButtons) b.setEnabled(room);
        undo.setEnabled(!made.isEmpty());
        play.setEnabled(!made.isEmpty());
    }

    private void addToMade(int t) {
        if (busy || made.size() >= MAKE_MAX) return;
        made.add(t);
        sounds.note(Tokens.shapeOf(t));
        showMade();
    }

    /** Sing her row; if it repeats, find the rule and carry on with up to four more. */
    private void playMade() {
        if (busy || made.isEmpty()) return;
        busy = true;
        int n = made.size();
        int[] mine = new int[n];
        for (int i = 0; i < n; i++) mine[i] = made.get(i);
        int p = Puzzle.period(mine, n);
        int extra = p > 0 ? Math.min(4, 12 - n) : 0;
        int[] row = new int[n + extra];
        for (int i = 0; i < row.length; i++) row[i] = i < n ? mine[i] : mine[i % p];
        machine.setRow(row, p > 0 ? n : Integer.MAX_VALUE);
        machine.setState(p > 0 ? "make rule " + p : "make no rule");
        machine.setFace(p > 0 ? Art.FACE_HAPPY : Art.FACE_THINK);
        machine.playAll(tok -> sounds.note(Tokens.shapeOf(tok)), 280, () -> {
            if (p > 0) sounds.fanfare();
            handler.postDelayed(() -> {
                busy = false;
                showMade();
            }, 1400);
        });
    }

    /** Back leaves "make your own" before it leaves the game. Android 4.4-12 calls this. */
    @SuppressWarnings("deprecation")
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (making) leaveMake();
        else super.onBackPressed();
    }

    /** Android 13+ sends back through a callback instead; hold one only while making. */
    private Object backCallback;

    private void holdBack(boolean on) {
        if (android.os.Build.VERSION.SDK_INT < 33) return;
        if (on && backCallback == null) backCallback = Back33.register(this, this::leaveMake);
        else if (!on && backCallback != null) {
            Back33.unregister(this, backCallback);
            backCallback = null;
        }
    }

    @android.annotation.TargetApi(33)
    private static final class Back33 {
        static Object register(Activity a, Runnable r) {
            android.window.OnBackInvokedCallback cb = r::run;
            a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
            return cb;
        }

        static void unregister(Activity a, Object cb) {
            a.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback) cb);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        machine.cancel();
        sounds.release();
        super.onDestroy();
    }

    // ---- layout ---------------------------------------------------------------------------

    /**
     * Header, machine, then the buttons. The same stack works in portrait and landscape: the
     * machine takes about half the height and the buttons size themselves to what is left,
     * between 56 and 120 dp.
     */
    private class Screen extends ViewGroup {
        private final float dp;

        Screen(Context c) {
            super(c);
            dp = c.getResources().getDisplayMetrics().density;
            setClipChildren(false);
        }

        private int px(float d) {
            return Math.round(d * dp);
        }

        private void at(View v, int x, int y, int w, int h) {
            v.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            v.setTag(R.id.tag_frame, new int[]{x, y, w, h});
        }

        @Override
        protected void onMeasure(int ws, int hs) {
            int w = MeasureSpec.getSize(ws), h = MeasureSpec.getSize(hs);
            setMeasuredDimension(w, h);
            Resources res = getResources();
            int sb = 0 /* the frame from frame() already sits below the status bar */;
            int top = (sb > 0 ? res.getDimensionPixelSize(sb) : 0) + px(4);
            int pad = px(12), gap = px(10), head = px(56);
            boolean land = w > h;
            at(getChildAt(0), 0, 0, w, h);  // backdrop
            at(home, pad, top, head, head);
            at(make, w - pad - head, top, head, head);
            at(title, pad + head + gap, top, w - 2 * (pad + head + gap), head);
            int y = top + head + gap;
            int inner = Math.min(w - 2 * pad, px(900));
            int x0 = (w - inner) / 2;
            int avail = h - y - pad;
            int machineH = Math.round(avail * (land ? 0.55f : 0.5f));
            machineH = Math.min(machineH, Math.round(inner * 0.75f));
            at(machine, x0, y, inner, machineH);
            y += machineH + gap * 2;
            int left = h - y - pad;

            // Game: one row of choices.
            int n = Math.max(1, choices.size());
            int b = Math.min(px(120), Math.min((inner - (n - 1) * gap) / n, left));
            b = Math.max(b, px(56));
            int x = (w - (n * b + (n - 1) * gap)) / 2;
            int cy = y + Math.max(0, (left - b) / 3);
            for (CandyButton c : choices) {
                at(c, x, cy, b, b);
                x += b + gap;
            }

            // Make: palette (one row, or two of three), then undo + play.
            int pn = paletteButtons.size();
            int per = (inner - (pn - 1) * gap) / pn >= px(56) ? pn : 3;
            int prow = (pn + per - 1) / per;
            int pb = Math.min(px(96), Math.min((inner - (per - 1) * gap) / per,
                    (left - prow * gap) / (prow + 1)));
            pb = Math.max(pb, px(48));
            for (int i = 0; i < pn; i++) {
                int r = i / per, c = i % per;
                int px0 = (w - (per * pb + (per - 1) * gap)) / 2;
                at(paletteButtons.get(i), px0 + c * (pb + gap), y + r * (pb + gap), pb, pb);
            }
            int ay = y + prow * (pb + gap);
            int rowW = Math.min(inner, px(420));
            at(undo, (w - rowW) / 2, ay, pb, pb);
            at(play, (w - rowW) / 2 + pb + gap, ay, rowW - pb - gap, pb);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);
                int[] f = (int[]) v.getTag(R.id.tag_frame);
                if (f != null) v.layout(f[0], f[1], f[0] + f[2], f[1] + f[3]);
            }
        }
    }

    /**
     * Wraps the screen in a frame that keeps it inside the status and navigation bars on every
     * Android version, including Android 15+ which always draws apps edge to edge. The bars sit
     * over the same soft gradient as the backdrop.
     */
    private android.view.View frame(android.view.View content) {
        android.widget.FrameLayout f = new android.widget.FrameLayout(this);
        f.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFFDCEB, 0xFFEBDFFF, 0xFFD2EEFF}));
        f.setFitsSystemWindows(true);
        f.addView(content);
        return f;
    }
}
