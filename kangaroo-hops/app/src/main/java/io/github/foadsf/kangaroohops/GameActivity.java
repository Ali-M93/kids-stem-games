package io.github.foadsf.kangaroohops;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Kangaroo Hops: where will she land? The card says "+3" or "−2"; she picks a number; then the
 * kangaroo hops one stone at a time, each landing counted aloud, so the answer is checked by
 * counting, in front of her. A wrong pick wobbles and greys out, the kangaroo hops back, and she
 * tries again; after two wrong picks dashed arcs show every hop on the line.
 *
 * "Which is more?" puzzles plant two flags on the line instead: the bigger number is the one
 * further along. Five puzzles make a round and earn a sticker; at most two wrong picks in a round
 * move her up a stage (see {@link Hop}).
 *
 * Counting language: the chip at the top right cycles Farsi, Dutch, English. It changes the
 * voice and the digits on the stones (Persian digits in Farsi).
 */
public class GameActivity extends Activity {
    private static final int PER_ROUND = 5, MAX_SLIPS_TO_ADVANCE = 2;
    private static final long HOP_MS = 520;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private SharedPreferences prefs;
    private Sounds sounds;
    private Voice voice;
    private Typeface face;
    private Screen screen;
    private LineView line;
    private Card card;
    private TextView title;
    private CandyButton home, langChip;
    private final List<CandyButton> choices = new ArrayList<>();
    private StickerView sticker;
    private LinearLayout stickerButtons;

    private String lang;
    private Hop hop;
    private int slips, roundDone, roundSlips;
    private boolean busy;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        // In the Lili & Friends app each child has their own progress file, and the journey map
        // passes the stage to play; on its own the game keeps one file and resumes.
        String file = getIntent().getStringExtra(EXTRA_PREFS);
        prefs = getSharedPreferences(file != null ? file : "progress", MODE_PRIVATE);
        playStage = Math.max(0, Math.min(getIntent().getIntExtra(EXTRA_STAGE, prefs.getInt("stage", 0)),
                prefs.getInt("stage", 0)));
        sounds = new Sounds();
        Art.load(getResources());
        voice = new Voice(this);
        voice.setOnReady(() -> {
            if (langChip != null) describeLang();
        });
        face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");
        Resources res = getResources();
        String sys = Locale.getDefault().getLanguage();
        lang = prefs.getString("lang", Arrays.asList(Numbers.LANGS).contains(sys) ? sys : "en");

        screen = new Screen(this);
        screen.addView(new Backdrop(this));
        title = new TextView(this);
        title.setTypeface(face);
        title.setTextColor(Art.PLUM);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setText(R.string.app_name);
        screen.addView(title);
        home = new CandyButton(this, (c, x, y, s) -> Art.house(c, x, y, s, 0xFFFFFFFF),
                Art.PINK, Art.PINK_D, null, face);
        home.setContentDescription(res.getString(R.string.cd_home));
        home.setOnClickListener(v -> finish());
        screen.addView(home);
        langChip = new CandyButton(this, null, Art.BLUE, Art.BLUE_D, label(), face);
        langChip.setOnClickListener(v -> nextLang());
        screen.addView(langChip);
        card = new Card(this);
        screen.addView(card);
        line = new LineView(this, face);
        line.setLang(lang);
        screen.addView(line);
        describeLang();

        FrameLayout root = new FrameLayout(this);
        root.addView(screen);
        sticker = new StickerView(this, face);
        sticker.setVisibility(View.GONE);
        root.addView(sticker);
        stickerButtons = stickerRow(res);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.bottomMargin = Math.round(40 * res.getDisplayMetrics().density);
        root.addView(stickerButtons, lp);
        setContentView(root);
        newPuzzle();
    }

    private String label() {
        return Numbers.LABEL[Arrays.asList(Numbers.LANGS).indexOf(lang)];
    }

    private void describeLang() {
        langChip.setContentDescription("counting language " + lang + " voice " + voice.source(lang));
    }

    private void nextLang() {
        int i = (Arrays.asList(Numbers.LANGS).indexOf(lang) + 1) % Numbers.LANGS.length;
        lang = Numbers.LANGS[i];
        prefs.edit().putString("lang", lang).apply();
        langChip.setLabel(label());
        line.setLang(lang);
        card.invalidate();
        for (int k = 0; k < choices.size(); k++) choices.get(k).setLabel(Numbers.digits(lang, hop.choices[k]));
        describeLang();
        say(1);
    }

    /** Counts {@code n} aloud if there is a voice for it; the stone note plays either way. */
    private void say(int n) {
        sounds.stone(n);
        voice.say(lang, n);
    }

    private LinearLayout stickerRow(Resources res) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        float d = res.getDisplayMetrics().density;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(150 * d), Math.round(72 * d));
        int m = Math.round(10 * d);
        lp.setMargins(m, 0, m, 0);
        CandyButton more = new CandyButton(this, null, Art.MINT, Art.MINT_D, res.getString(R.string.more), face);
        more.setContentDescription(res.getString(R.string.more));
        more.setOnClickListener(v -> {
            sticker.setVisibility(View.GONE);
            stickerButtons.setVisibility(View.GONE);
            newPuzzle();
        });
        CandyButton done = new CandyButton(this, null, Art.PEACH, Art.PEACH_D, res.getString(R.string.done), face);
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

    private void newPuzzle() {
        hop = Hop.make(stage(), rnd);
        slips = 0;
        busy = false;
        if (hop.op == Hop.MORE) {
            line.setup(hop.max, 0, Math.min(hop.a, hop.b), Math.max(hop.a, hop.b));
            line.setFlags(hop.a, hop.b);
        } else {
            line.setup(hop.max, hop.start, hop.start, hop.landing());
        }
        line.setState("stage " + stage() + " puzzle " + (roundDone + 1) + " " + describe(hop));
        card.invalidate();
        for (CandyButton b : choices) screen.removeView(b);
        choices.clear();
        int[] colours = {Art.PINK, Art.YELLOW, Art.LILAC};
        int[] deeps = {Art.PINK_D, Art.YELLOW_D, Art.LILAC_D};
        for (int k = 0; k < hop.choices.length; k++) {
            final int n = hop.choices[k];
            int fill = hop.op == Hop.MORE ? (n == hop.a ? Art.PINK : Art.BLUE) : colours[k];
            int deep = hop.op == Hop.MORE ? (n == hop.a ? Art.PINK_D : Art.BLUE_D) : deeps[k];
            final CandyButton b = new CandyButton(this, null, fill, deep, Numbers.digits(lang, n), face);
            b.setContentDescription("choice " + n);
            b.setOnClickListener(v -> pick(n, b));
            choices.add(b);
            screen.addView(b);
        }
        screen.requestLayout();
    }

    private static String describe(Hop h) {
        if (h.op == Hop.MORE) return "more of " + h.a + " and " + h.b;
        return h.start + (h.op == Hop.ADD ? " plus " : " minus ") + h.delta;
    }

    private void pick(final int n, final CandyButton b) {
        if (busy) return;
        busy = true;
        final boolean right = n == hop.answer;
        if (hop.op == Hop.MORE) {
            line.setMark(hop.answer);
            say(hop.a);
            handler.postDelayed(() -> say(hop.b), 700);
            handler.postDelayed(() -> finishPick(right, b), 1500);
            return;
        }
        line.setState("hopping");
        line.hopTo(hop.landing(), HOP_MS, this::say, () -> {
            line.setMark(hop.landing());
            finishPick(right, b);
        });
    }

    private void finishPick(boolean right, CandyButton b) {
        if (right) {
            line.setHappy(true);
            line.setState("solved");
            sounds.fanfare();
            for (CandyButton c : choices) c.setEnabled(c == b);
            roundDone++;
            prefs.edit().putInt("stars", prefs.getInt("stars", 0) + 1).apply();
            handler.postDelayed(this::afterSolved, 1600);
            return;
        }
        slips++;
        roundSlips++;
        sounds.boop();
        b.shake();
        b.setEnabled(false);
        Runnable again = () -> {
            busy = false;
            line.setMark(-1);
            if (slips >= 2 && hop.op != Hop.MORE) line.showHint(hop.start, hop.landing());
            line.setState("stage " + stage() + " puzzle " + (roundDone + 1) + " " + describe(hop)
                    + " slips " + slips + (slips >= 2 && hop.op != Hop.MORE ? " hint" : ""));
        };
        if (hop.op == Hop.MORE) handler.postDelayed(again, 600);
        else handler.postDelayed(() -> line.slideTo(hop.start, again), 700);
    }

    private void afterSolved() {
        if (roundDone < PER_ROUND) {
            newPuzzle();
            return;
        }
        int stage = stage() + (roundSlips <= MAX_SLIPS_TO_ADVANCE ? 1 : 0);
        int stickers = prefs.getInt("stickers", 0) + 1;
        reachStage(stage);
        prefs.edit().putInt("stickers", stickers).apply();
        roundDone = 0;
        roundSlips = 0;
        sticker.show(stickers);
        stickerButtons.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        sounds.release();
        voice.release();
        super.onDestroy();
    }

    // ---- the instruction card --------------------------------------------------------------

    /** "+3" with three hop arcs, "−2" with arcs going left, or "?" for "which is more?". */
    private class Card extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final float dp;

        Card(Context c) {
            super(c);
            dp = c.getResources().getDisplayMetrics().density;
            t.setTypeface(face);
            t.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onDraw(Canvas c) {
            if (hop == null) return;
            float w = getWidth(), h = getHeight();
            float cw = Math.min(w, h * 3.6f);
            float x0 = (w - cw) / 2;
            p.setStyle(Paint.Style.FILL);
            p.setColor(Art.YELLOW_D);
            r.set(x0, 5 * dp, x0 + cw, h);
            c.drawRoundRect(r, 22 * dp, 22 * dp, p);
            p.setColor(0xFFFFF6DA);
            r.set(x0, 0, x0 + cw, h - 5 * dp);
            c.drawRoundRect(r, 22 * dp, 22 * dp, p);
            float cy = (h - 5 * dp) / 2;
            if (hop.op == Hop.MORE) {
                t.setTextSize(Math.min(h * 0.42f, 34 * dp));
                t.setColor(Art.PLUM);
                String q = getString(R.string.which_more);
                float tw = t.measureText(q);
                if (tw > cw - 30 * dp) t.setTextSize(t.getTextSize() * (cw - 30 * dp) / tw);
                c.drawText(q, x0 + cw / 2, cy - (t.ascent() + t.descent()) / 2, t);
                return;
            }
            String sign = hop.op == Hop.ADD ? "+" : "−";
            String num = sign + Numbers.digits(lang, hop.delta);
            t.setTextSize(Math.min(h * 0.62f, 56 * dp));
            t.setColor(hop.op == Hop.ADD ? Art.MINT_D : Art.PINK_D);
            float numW = t.measureText(num);
            // Arcs for the hops, small enough to fit beside the number.
            float arcs = hop.delta, aw = Math.min(30 * dp, (cw - numW - 40 * dp) / Math.max(1, arcs));
            float total = numW + 16 * dp + arcs * aw;
            float x = x0 + (cw - total) / 2;
            c.drawText(num, x + numW / 2, cy - (t.ascent() + t.descent()) / 2, t);
            x += numW + 16 * dp;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2 * dp, aw * 0.12f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(hop.op == Hop.ADD ? Art.MINT_D : Art.PINK_D);
            for (int i = 0; i < hop.delta; i++) {
                r.set(x + i * aw + aw * 0.08f, cy - aw * 0.45f, x + (i + 1) * aw - aw * 0.08f, cy + aw * 0.45f);
                c.drawArc(r, 180, 180, false, p);
                // an arrow head shows the direction
                float hx = hop.op == Hop.ADD ? r.right : r.left;
                float sgn = hop.op == Hop.ADD ? -1 : 1;
                c.drawLine(hx, r.centerY(), hx + sgn * aw * 0.16f, r.centerY() - aw * 0.16f, p);
            }
        }
    }

    // ---- layout ---------------------------------------------------------------------------

    /** Header, card, number line, then the number buttons; one stack for any screen. */
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
            int sb = res.getIdentifier("status_bar_height", "dimen", "android");
            int top = (sb > 0 ? res.getDimensionPixelSize(sb) : 0) + px(4);
            int pad = px(12), gap = px(10), head = px(56);
            at(getChildAt(0), 0, 0, w, h);
            at(home, pad, top, head, head);
            at(langChip, w - pad - px(72), top, px(72), head);
            at(title, pad + head + gap, top, w - 2 * pad - head - px(72) - 2 * gap, head);
            int y = top + head + gap;
            int inner = Math.min(w - 2 * pad, px(1000));
            int x0 = (w - inner) / 2;
            int avail = h - y - pad;
            boolean land = w > h;
            int cardH = Math.min(px(90), Math.round(avail * (land ? 0.2f : 0.16f)));
            at(card, x0, y, inner, cardH);
            y += cardH + gap;
            int lineH = Math.round(avail * (land ? 0.42f : 0.4f));
            at(line, x0, y, inner, lineH);
            y += lineH + gap * 2;
            int left = h - y - pad;
            int n = Math.max(1, choices.size());
            int b = Math.max(px(56), Math.min(px(120), Math.min((inner - (n - 1) * gap) / n, left)));
            int x = (w - (n * b + (n - 1) * gap)) / 2;
            int by = y + Math.max(0, (left - b) / 3);
            for (CandyButton c : choices) {
                at(c, x, by, b, b);
                x += b + gap;
            }
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
}
