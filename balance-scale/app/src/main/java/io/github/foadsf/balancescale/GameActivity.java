package io.github.foadsf.balancescale;

import android.annotation.SuppressLint;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Balance Scale: make both sides weigh the same. Fruit sits on the left pan; she drags fruit from
 * the basket onto the right pan (or just taps it), and the beam tips and wobbles as the weights
 * change. When both sides weigh the same the beam levels out and a big "=" appears. Tapping a
 * fruit she added takes it off again; undo takes off the last one.
 *
 * Heavier fruit come with a fact card shown as a small balanced scale (one watermelon = three
 * apples), so she can reason "the watermelon is like three apples": early algebra, with fruit.
 *
 * From stage 1 on, a red peg holds the beam level while she loads the pan, and she presses Weigh
 * to release it: she has to decide how many before she sees the answer. A weigh that does not
 * balance is a slip; the peg goes back in as soon as she changes anything. (Stage 0 tips live,
 * so she first learns what tipping means.) Five puzzles make a round and earn a sticker; a round
 * with at most three slips moves her up a stage (see {@link Weigh}).
 */
public class GameActivity extends Activity {
    private static final int PER_ROUND = 5, MAX_SLIPS_TO_ADVANCE = 3, PAN_MAX = 10;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private SharedPreferences prefs;
    private Sounds sounds;
    private Typeface face;
    private Screen screen;
    private ScaleView scale;
    private Facts facts;
    private TextView title;
    private CandyButton home, undo, weigh;
    private final List<CandyButton> basket = new ArrayList<>();
    private StickerView sticker;
    private LinearLayout stickerButtons;

    private Weigh puzzle;
    private final List<Integer> right = new ArrayList<>();
    private int slips, roundDone, roundSlips;
    private boolean live, done;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("progress", MODE_PRIVATE);
        sounds = new Sounds();
        face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");
        Resources res = getResources();

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
        facts = new Facts(this);
        screen.addView(facts);
        scale = new ScaleView(this);
        scale.setListener(this::removeAt);
        screen.addView(scale);
        undo = new CandyButton(this, (c, x, y, s) -> Art.undo(c, x, y, s * 0.9f, 0xFFFFFFFF),
                0xFFC9CDE0, 0xFF9AA0BC, null, face);
        undo.setContentDescription(res.getString(R.string.cd_undo));
        undo.setOnClickListener(v -> {
            if (!done && right.size() > puzzle.rightGiven.size()) removeAt(right.size() - 1);
        });
        screen.addView(undo);
        weigh = new CandyButton(this, (c, x, y, s) -> Art.equals(c, x, y, s, 0xFFFFFFFF),
                Art.MINT, Art.MINT_D, res.getString(R.string.weigh), face);
        weigh.setContentDescription(res.getString(R.string.weigh));
        weigh.setOnClickListener(v -> weighNow());
        screen.addView(weigh);

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
        CandyButton fin = new CandyButton(this, null, Art.PEACH, Art.PEACH_D, res.getString(R.string.done), face);
        fin.setContentDescription(res.getString(R.string.done));
        fin.setOnClickListener(v -> finish());
        row.addView(more, lp);
        row.addView(fin, new LinearLayout.LayoutParams(lp));
        row.setVisibility(View.GONE);
        return row;
    }

    // ---- puzzles ------------------------------------------------------------------------

    private int stage() {
        return prefs.getInt("stage", 0);
    }

    private String where() {
        return "stage " + stage() + " puzzle " + (roundDone + 1);
    }

    private void newPuzzle() {
        puzzle = Weigh.make(stage(), rnd);
        right.clear();
        right.addAll(puzzle.rightGiven);
        slips = 0;
        done = false;
        live = puzzle.kind == 0;
        scale.setLatched(!live);
        scale.setPans(puzzle.left, right, puzzle.rightGiven.size(), false);
        weigh.setVisibility(live ? View.GONE : View.VISIBLE);
        scale.setState(where());
        facts.invalidate();
        for (CandyButton b : basket) screen.removeView(b);
        basket.clear();
        for (int f : puzzle.basket) {
            CandyButton b = new CandyButton(this, (c, x, y, s) -> Art.fruit(c, x, y, s * 1.15f, f, 255),
                    0xFFFFFFFF, 0xFFD9D2F0, null, face);
            b.setContentDescription("add " + Weigh.NAME[f]);
            b.setOnClickListener(v -> add(f));
            dragFrom(b, f);
            basket.add(b);
            screen.addView(b);
        }
        refresh();
        screen.requestLayout();
    }

    /** Lets a basket fruit be dragged onto the right pan; a tap without moving still adds it. */
    @SuppressLint("ClickableViewAccessibility")
    private void dragFrom(final CandyButton b, final int fruit) {
        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        b.setOnTouchListener(new View.OnTouchListener() {
            float x0, y0;
            boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                int[] at = new int[2];
                scale.getLocationOnScreen(at);
                float sx = e.getRawX() - at[0], sy = e.getRawY() - at[1];
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        x0 = e.getRawX();
                        y0 = e.getRawY();
                        dragging = false;
                        return false;  // let the button show its press
                    case MotionEvent.ACTION_MOVE:
                        if (!dragging && Math.hypot(e.getRawX() - x0, e.getRawY() - y0) > slop * 2) {
                            dragging = true;
                            b.setPressed(false);
                        }
                        if (dragging) {
                            scale.drag(fruit, sx, sy);
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                        if (dragging) {
                            dragging = false;
                            if (scale.drop()) add(fruit);
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_CANCEL:
                        if (dragging) scale.drop();
                        dragging = false;
                        return false;
                    default:
                        return false;
                }
            }
        });
    }

    private void refresh() {
        boolean room = right.size() < PAN_MAX && !done;
        for (CandyButton b : basket) b.setEnabled(room);
        undo.setEnabled(!done && right.size() > puzzle.rightGiven.size());
        weigh.setEnabled(!done && right.size() > puzzle.rightGiven.size());
    }

    private void add(int fruit) {
        if (done || right.size() >= PAN_MAX) return;
        right.add(fruit);
        sounds.plop(fruit);
        changed();
    }

    private void removeAt(int i) {
        if (done || i < puzzle.rightGiven.size() || i >= right.size()) return;
        right.remove(i);
        sounds.pop();
        changed();
    }

    private void changed() {
        if (!live) scale.setLatched(true);   // any change puts the peg back in
        scale.setPans(puzzle.left, right, puzzle.rightGiven.size(), true);
        scale.setState(where() + (slips > 0 ? " slips " + slips : ""));
        if (live && Weigh.weight(right) == Weigh.weight(puzzle.left)) solved();
        refresh();
    }

    /** Pull the peg: the beam shows the truth. */
    private void weighNow() {
        if (done) return;
        scale.setLatched(false);
        if (Weigh.weight(right) == Weigh.weight(puzzle.left)) {
            solved();
        } else {
            slips++;
            roundSlips++;
            scale.setState(where() + " slips " + slips);
        }
        refresh();
    }

    private void solved() {
        done = true;
        scale.setState("solved");
        handler.postDelayed(sounds::fanfare, 450);
        roundDone++;
        prefs.edit().putInt("stars", prefs.getInt("stars", 0) + 1).apply();
        handler.postDelayed(this::afterSolved, 2000);
    }

    private void afterSolved() {
        if (roundDone < PER_ROUND) {
            newPuzzle();
            return;
        }
        int stage = stage() + (roundSlips <= MAX_SLIPS_TO_ADVANCE ? 1 : 0);
        int stickers = prefs.getInt("stickers", 0) + 1;
        prefs.edit().putInt("stage", stage).putInt("stickers", stickers).apply();
        roundDone = 0;
        roundSlips = 0;
        sticker.show(stickers);
        stickerButtons.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        sounds.release();
        super.onDestroy();
    }

    // ---- fact cards ----------------------------------------------------------------------------

    /** One small balanced scale per heavy fruit in play: 🍉 = 🍎🍎🍎, 🍌 = 🍎🍎, 🍐 = 🍎. */
    private class Facts extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final float dp;

        Facts(Context c) {
            super(c);
            dp = c.getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onDraw(Canvas c) {
            if (puzzle == null || puzzle.facts.length == 0) return;
            float w = getWidth(), h = getHeight();
            int n = puzzle.facts.length;
            float cardW = Math.min((w - (n - 1) * 10 * dp) / n, 260 * dp);
            float x0 = (w - (n * cardW + (n - 1) * 10 * dp)) / 2;
            StringBuilder desc = new StringBuilder("facts:");
            for (int i = 0; i < n; i++) {
                int f = puzzle.facts[i], k = Weigh.WEIGHT[f];
                desc.append(' ').append(Weigh.NAME[f]).append('=').append(k);
                float x = x0 + i * (cardW + 10 * dp);
                p.setColor(Art.YELLOW_D);
                r.set(x, 4 * dp, x + cardW, h);
                c.drawRoundRect(r, 18 * dp, 18 * dp, p);
                p.setColor(0xFFFFF6DA);
                r.set(x, 0, x + cardW, h - 4 * dp);
                c.drawRoundRect(r, 18 * dp, 18 * dp, p);
                float s = Math.min((h - 4 * dp) * 0.7f, cardW / (k + 2.6f));
                float cy = (h - 4 * dp) / 2;
                float total = s + s * 0.9f + k * s * 0.9f;
                float cx = x + (cardW - total) / 2 + s / 2;
                Art.fruit(c, cx, cy, s, f, 255);
                cx += s * 0.95f;
                Art.equals(c, cx, cy, s * 0.65f, Art.MINT_D);
                cx += s * 0.95f;
                for (int j = 0; j < k; j++) Art.fruit(c, cx + j * s * 0.9f, cy, s * 0.9f, Weigh.APPLE, 255);
            }
            setContentDescription(desc.toString());
        }
    }

    // ---- layout ---------------------------------------------------------------------------

    /** Header, fact cards (only when there are any), the scale, then the basket and undo. */
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
            at(title, pad + head + gap, top, w - 2 * (pad + head + gap), head);
            int y = top + head + gap;
            int inner = Math.min(w - 2 * pad, px(1000));
            int x0 = (w - inner) / 2;
            int avail = h - y - pad;
            boolean hasFacts = puzzle != null && puzzle.facts.length > 0;
            int factH = hasFacts ? Math.min(px(72), Math.round(avail * 0.14f)) : 0;
            at(facts, x0, y, inner, Math.max(1, factH));
            y += factH + (hasFacts ? gap : 0);
            int btn = Math.max(px(64), Math.min(px(120), Math.round(avail * 0.2f)));
            int scaleH = h - y - pad - btn - gap * 2;
            at(scale, x0, y, inner, scaleH);
            y += scaleH + gap;
            // basket fruit, undo, and (when the peg is in) a wide Weigh button
            boolean wb = weigh.getVisibility() == View.VISIBLE;
            int n = basket.size() + 1 + (wb ? 2 : 0);
            int b = Math.min(btn, (inner - (n - 1) * gap) / n);
            int x = (w - (n * b + (n - 1) * gap)) / 2;
            for (CandyButton c : basket) {
                at(c, x, y, b, b);
                x += b + gap;
            }
            at(undo, x, y, b, b);
            x += b + gap;
            at(weigh, x, y, wb ? 2 * b + gap : 1, b);
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
