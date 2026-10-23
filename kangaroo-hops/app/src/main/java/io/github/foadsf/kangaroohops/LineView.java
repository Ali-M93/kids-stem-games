package io.github.foadsf.kangaroohops;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * The number line: a row of numbered stepping stones across a meadow, with the kangaroo on one.
 *
 * When the whole line does not fit with stones at least 36 dp apart (0-20 on a phone), it shows
 * a window of the line that contains everything the puzzle needs, with the numbers still in
 * order; the cut ends fade out so it reads as part of a longer line.
 */
class LineView extends View {
    interface Hopper {
        /** The kangaroo has just landed on {@code n}. */
        void landed(int n);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dash = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    private final float dp;
    private String lang = "en";
    private int max = 10, lo = 0, hi = 10;
    private int need0 = 0, need1 = 10;   // numbers that must be in view
    private float pos;                    // kangaroo position, as a fractional number
    private float jumpT = -1;             // 0..1 during a hop
    private int dir = 1;
    private boolean happy;
    private int start = -1, mark = -1;    // highlighted stones
    private int flagA = -1, flagB = -1;   // "which is more" flags
    private int hintFrom = -1, hintTo = -1;
    private boolean showRoo = true;       // she steps aside for "which is more?" so both flags show
    private int popN = -1;                // number shown big above the kangaroo
    private float popT;
    private ValueAnimator anim;
    private String state = "";

    LineView(Context ctx, Typeface face) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
        text.setTypeface(face);
        text.setTextAlign(Paint.Align.CENTER);
        dash.setStyle(Paint.Style.STROKE);
        dash.setStrokeWidth(4.5f * dp);
        dash.setStrokeCap(Paint.Cap.ROUND);
        dash.setColor(0xFF3F95E0);
        dash.setPathEffect(new DashPathEffect(new float[]{9 * dp, 7 * dp}, 0));
    }

    void setLang(String l) {
        lang = l;
        invalidate();
    }

    /** A fresh puzzle: line 0..max, kangaroo on {@code at}; keep {@code a..b} in view. */
    void setup(int max, int at, int a, int b) {
        if (anim != null) anim.cancel();
        this.max = max;
        need0 = Math.max(0, Math.min(a, b));
        need1 = Math.min(max, Math.max(a, b));
        pos = at;
        start = at;
        mark = -1;
        flagA = flagB = -1;
        showRoo = true;
        hintFrom = hintTo = -1;
        happy = false;
        jumpT = -1;
        popN = -1;
        window();
        invalidate();
    }

    void setFlags(int a, int b) {
        flagA = a;
        flagB = b;
        showRoo = a < 0;
        invalidate();
    }

    void showHint(int from, int to) {
        hintFrom = from;
        hintTo = to;
        invalidate();
    }

    void setHappy(boolean h) {
        happy = h;
        invalidate();
    }

    void setMark(int n) {
        mark = n;
        invalidate();
    }

    void setState(String s) {
        state = s;
        setContentDescription("line " + s + " kangaroo at " + Math.round(pos));
    }

    boolean busy() {
        return anim != null && anim.isRunning();
    }

    /** How many stones fit at 36 dp or more; the window is that wide (at least 11). */
    private int span() {
        int fit = (int) ((getWidth() - 24 * dp) / (36 * dp));
        return Math.max(11, Math.min(max + 1, fit));
    }

    private void window() {
        int span = Math.min(max + 1, span());
        int mid = (need0 + need1) / 2;
        lo = Math.max(0, Math.min(max + 1 - span, mid - span / 2));
        hi = lo + span - 1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        window();
    }

    /** Hops one stone at a time from the current spot to {@code to}, then runs {@code done}. */
    void hopTo(final int to, final long stepMs, final Hopper h, final Runnable done) {
        final int from = Math.round(pos);
        if (from == to) {
            if (done != null) done.run();
            return;
        }
        dir = to > from ? 1 : -1;
        final int next = from + dir;
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(stepMs);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            jumpT = (Float) a.getAnimatedValue();
            pos = from + dir * jumpT;
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator a) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator a) {
                if (cancelled) return;
                jumpT = -1;
                pos = next;
                popN = next;
                popT = 0;
                ValueAnimator p = ValueAnimator.ofFloat(0f, 1f);
                p.setDuration(stepMs);
                p.addUpdateListener(v -> {
                    popT = (Float) v.getAnimatedValue();
                    invalidate();
                });
                p.start();
                if (h != null) h.landed(next);
                setState(state);
                hopTo(to, stepMs, h, done);
            }
        });
        anim.start();
    }

    /** Slides straight back (no counting) after a wrong guess. */
    void slideTo(final int to, final Runnable done) {
        final float from = pos;
        dir = to > from ? 1 : -1;
        popN = -1;
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(500);
        anim.addUpdateListener(a -> {
            float t = (Float) a.getAnimatedValue();
            pos = from + (to - from) * t;
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                pos = to;
                dir = 1;
                setState(state);
                if (done != null) done.run();
            }
        });
        anim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    private float xOf(float n) {
        float w = getWidth(), pad = 18 * dp;
        int count = hi - lo;
        return pad + (n - lo) * (w - 2 * pad) / Math.max(1, count);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float step = (xOf(hi) - xOf(lo)) / Math.max(1, hi - lo);
        float ground = h * 0.8f;

        // Meadow.
        paint.setColor(0xFFC9F2DC);
        r.set(0, ground - 14 * dp, w, h);
        c.drawRoundRect(r, 24 * dp, 24 * dp, paint);
        paint.setColor(0xFFA6E6C5);
        r.set(0, ground + 10 * dp, w, h);
        c.drawRoundRect(r, 24 * dp, 24 * dp, paint);

        // Stones with their numbers.
        float sw = Math.min(step * 0.92f, 72 * dp), sh = sw * 0.72f;
        text.setTextSize(Math.min(sw * 0.56f, 30 * dp));
        for (int n = lo; n <= hi; n++) {
            float x = xOf(n);
            boolean edge = (n == lo && lo > 0) || (n == hi && hi < max);
            int alpha = edge ? 90 : 255;
            int face = n == mark ? Art.YELLOW : n == start ? 0xFFFFF1B8 : 0xFFEDEFF7;
            paint.setColor((0x00FFFFFF & 0xFFB4B8CC) | (alpha << 24));
            r.set(x - sw / 2, ground - sh / 2 + 4 * dp, x + sw / 2, ground + sh / 2 + 4 * dp);
            c.drawOval(r, paint);
            paint.setColor((face & 0x00FFFFFF) | (alpha << 24));
            r.set(x - sw / 2, ground - sh / 2, x + sw / 2, ground + sh / 2);
            c.drawOval(r, paint);
            text.setColor((Art.PLUM & 0x00FFFFFF) | (alpha << 24));
            c.drawText(Numbers.digits(lang, n), x, ground - (text.ascent() + text.descent()) / 2f, text);
        }

        // "Which is more?" flags.
        float fs = Math.min(step * 1.6f, h * 0.45f);
        if (flagA >= 0) Art.flag(c, xOf(flagA), ground - sh / 2, fs, Art.PINK_D);
        if (flagB >= 0) Art.flag(c, xOf(flagB), ground - sh / 2, fs, Art.BLUE_D);

        // The kangaroo, arcing between stones when she hops.
        float ks = Math.min(h * 0.58f, 150 * dp);
        float lift = jumpT >= 0 ? (float) Math.sin(jumpT * Math.PI) * Math.max(step * 1.2f, ks * 0.4f) : 0;
        float squash = jumpT >= 0 && jumpT < 0.15f ? 1 - jumpT / 0.15f : 0;
        float kx = xOf(pos);
        if (showRoo) {
            // A soft shadow on the ground that shrinks as she rises.
            float shw = ks * 0.32f * (1 - 0.4f * Math.min(1f, lift / Math.max(1f, ks * 0.4f)));
            paint.setColor(0x26000000);
            r.set(kx - shw, ground - sh * 0.15f - ks * 0.04f, kx + shw, ground - sh * 0.15f + ks * 0.04f);
            c.drawOval(r, paint);
            Art.lili(c, kx, ground - sh * 0.15f - lift, ks, dir, squash, happy, jumpT >= 0);
        }

        // Hint: one dashed arc per hop, drawn over the kangaroo so she never hides it.
        if (hintFrom >= 0 && hintTo != hintFrom) {
            int d = hintTo > hintFrom ? 1 : -1;
            for (int n = hintFrom; n != hintTo; n += d) {
                float a = xOf(n), b = xOf(n + d);
                path.reset();
                path.moveTo(a, ground - sh / 2);
                path.quadTo((a + b) / 2, ground - sh / 2 - Math.max(step * 1.6f, 40 * dp), b, ground - sh / 2);
                c.drawPath(path, dash);
            }
        }

        // The number she just landed on, popping up big.
        if (popN >= 0 && popT < 1) {
            text.setTextSize(Math.min(ks * 0.5f, 54 * dp));
            text.setColor(((int) (255 * (1 - popT * popT)) << 24) | (Art.PLUM & 0x00FFFFFF));
            c.drawText(Numbers.digits(lang, popN), kx, ground - ks * 1.05f - popT * 18 * dp, text);
        }
    }
}
