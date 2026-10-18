package io.github.foadsf.patternmachine;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * The pattern machine: a candy-coloured box with three lights, a little robot peeking over the
 * top, and a belt of tokens in one or two rows. Gaps show a pulsing "?". It can play its row
 * like a tune, bouncing each token as its note sounds.
 */
class MachineView extends View {
    interface Player {
        void note(int token);
    }

    static final int EMPTY = -1;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int[] row = new int[0];     // tokens; EMPTY for a gap
    private int ghostFrom = Integer.MAX_VALUE;  // tokens from here on are the machine's continuation
    private int unit;                   // > 0: underline repeats (the hint)
    private int bounce = -1;            // index bouncing right now
    private float bounceT;
    private int pop = -1;               // index just filled
    private float popT = 1f;
    private int face = Art.FACE_CALM;
    private int lights;                 // which of the three lights glow (bitmask)
    private String state = "idle";

    MachineView(Context ctx) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
    }

    void setRow(int[] tokens, int ghostFrom) {
        row = tokens.clone();
        this.ghostFrom = ghostFrom;
        unit = 0;
        face = Art.FACE_CALM;
        lights = 0;
        bounce = -1;
        pop = -1;
        state = "idle";
        describe();
        requestLayout();
        invalidate();
    }

    int length() {
        return row.length;
    }

    void showUnits(int u) {
        unit = u;
        describe();
        invalidate();
    }

    void setFace(int f) {
        face = f;
        invalidate();
    }

    void setState(String s) {
        state = s;
        describe();
    }

    /** Puts {@code t} into slot {@code i} with a pop. */
    void fill(int i, int t) {
        row[i] = t;
        pop = i;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(380);
        a.setInterpolator(new OvershootInterpolator(3f));
        a.addUpdateListener(v -> {
            popT = (Float) v.getAnimatedValue();
            invalidate();
        });
        a.start();
        describe();
    }

    /** Plays the whole row left to right, one note every {@code stepMs}, then runs {@code done}. */
    void playAll(final Player p, final long stepMs, final Runnable done) {
        for (int i = 0; i <= row.length; i++) {
            final int k = i;
            handler.postDelayed(() -> {
                if (k == row.length) {
                    bounce = -1;
                    lights = 7;
                    invalidate();
                    if (done != null) done.run();
                    return;
                }
                bounce = k;
                lights = 1 << (k % 3);
                if (row[k] != EMPTY) p.note(row[k]);
                ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
                a.setDuration(stepMs);
                a.addUpdateListener(v -> {
                    bounceT = (Float) v.getAnimatedValue();
                    invalidate();
                });
                a.start();
            }, i * stepMs);
        }
    }

    void cancel() {
        handler.removeCallbacksAndMessages(null);
        bounce = -1;
    }

    @Override
    protected void onDetachedFromWindow() {
        cancel();
        super.onDetachedFromWindow();
    }

    /** "machine idle: circle-pink triangle-blue ? | hint 2": read by TalkBack and tools/verify.py. */
    private void describe() {
        StringBuilder sb = new StringBuilder("machine ").append(state).append(':');
        for (int i = 0; i < row.length; i++) {
            sb.append(' ');
            if (i == ghostFrom) sb.append("| then ");
            sb.append(row[i] == EMPTY ? "?" : Tokens.name(row[i]));
        }
        if (unit > 0) sb.append(" | hint ").append(unit);
        setContentDescription(sb.toString());
    }

    // ---- layout of the belt -------------------------------------------------------------

    private int cols() {
        int n = Math.max(1, row.length);
        float w = getWidth() - 2 * pad();
        // One row, so the sequence reads left to right, unless tokens would drop below 44 dp.
        return w / n >= 44 * dp ? n : (n + 1) / 2;
    }

    private float pad() {
        return Math.max(14 * dp, getWidth() * 0.05f);
    }

    private float cell() {
        int c = cols(), rows = (row.length + c - 1) / Math.max(1, c);
        float w = (getWidth() - 2 * pad()) / c;
        float h = (getHeight() * 0.62f) / Math.max(1, rows);
        return Math.min(Math.min(w, h), 96 * dp);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float top = h * 0.16f;  // room for the robot and lights above the box

        // The machine body.
        paint.setColor(Art.LILAC_D);
        r.set(0, top + 6 * dp, w, h);
        c.drawRoundRect(r, 26 * dp, 26 * dp, paint);
        paint.setColor(Art.LILAC);
        r.set(0, top, w, h - 6 * dp);
        c.drawRoundRect(r, 26 * dp, 26 * dp, paint);

        // Three lights on top; they chase while the machine plays.
        int[] lightOn = {Art.PINK, Art.YELLOW, Art.MINT};
        for (int i = 0; i < 3; i++) {
            float lx = w * 0.62f + i * 22 * dp, ly = top + 1 * dp;
            paint.setColor(Art.PLUM);
            c.drawCircle(lx, ly, 9 * dp, paint);
            paint.setColor((lights & (1 << i)) != 0 ? lightOn[i] : 0xFFD9D2F0);
            c.drawCircle(lx, ly, 7 * dp, paint);
        }
        // The robot operator, peeking over the top edge.
        float rs = Math.min(h * 0.32f, 84 * dp);
        Art.robot(c, w * 0.2f, top + rs * 0.05f, rs, face, -1);

        // The belt.
        int n = row.length;
        if (n == 0) return;
        int cols = cols();
        int rows = (n + cols - 1) / cols;
        float s = cell();
        float beltH = rows * s;
        float y0 = top + (h - top - 6 * dp - beltH) / 2f + 6 * dp;
        paint.setColor(0xFF4A4370);
        r.set(pad() * 0.5f, y0 - 6 * dp, w - pad() * 0.5f, y0 + beltH + 6 * dp);
        c.drawRoundRect(r, 18 * dp, 18 * dp, paint);

        for (int i = 0; i < n; i++) {
            int rr = i / cols, cc = i % cols;
            int inRow = Math.min(cols, n - rr * cols);
            float x0 = (w - inRow * s) / 2f;
            float cx = x0 + cc * s + s / 2f, cy = y0 + rr * s + s / 2f;
            float lift = 0;
            if (i == bounce) lift = -(float) Math.sin(bounceT * Math.PI) * s * 0.22f;
            float scale = i == pop ? Math.max(0.2f, popT) : 1f;
            // White card under each token.
            paint.setColor(i >= ghostFrom ? 0x66FFFFFF : 0xFFFFFFFF);
            r.set(cx - s * 0.44f, cy - s * 0.44f + lift, cx + s * 0.44f, cy + s * 0.44f + lift);
            c.drawRoundRect(r, s * 0.16f, s * 0.16f, paint);
            if (row[i] == EMPTY) {
                float pulse = 1f + 0.08f * (float) Math.sin(System.currentTimeMillis() / 180.0);
                Art.question(c, cx, cy + lift, s * 0.62f * pulse, Art.PLUM);
                if (isShown()) postInvalidateDelayed(50);
            } else {
                c.save();
                c.scale(scale, scale, cx, cy);
                Art.token(c, cx, cy + lift, s * 0.72f, row[i], i >= ghostFrom ? 150 : 255);
                c.restore();
            }
        }
        // Hint: a coloured bar under each repeat of the unit, alternating colours.
        if (unit > 0) {
            for (int i = 0; i < n; i++) {
                int rr = i / cols, cc = i % cols;
                int inRow = Math.min(cols, n - rr * cols);
                float x0 = (w - inRow * s) / 2f;
                paint.setColor((i / unit) % 2 == 0 ? Art.YELLOW_D : Art.MINT_D);
                r.set(x0 + cc * s + s * 0.08f, y0 + rr * s + s * 0.9f,
                        x0 + (cc + 1) * s - s * 0.08f, y0 + rr * s + s * 0.98f);
                c.drawRoundRect(r, 3 * dp, 3 * dp, paint);
            }
        }
    }
}
