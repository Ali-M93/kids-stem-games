package io.github.foadsf.robotpath;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.Random;

/**
 * The grid, the robot and the star. {@link #run} plays a program one step at a time and
 * reports what happened; it never decides anything about scoring.
 */
class BoardView extends View {
    interface Listener {
        void onStep(int index);

        /** Step {@code index} walked into a rock or off the board. */
        void onBump(int index);

        void onWin(int stepsUsed);

        /** Every step ran, but the robot is not on the star yet. */
        void onShort();
    }

    private static final long STEP_MS = 430;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;
    private Level level;
    private float robotX, robotY;  // in cells, fractional while moving
    private int face = Art.FACE_CALM, look = -1;
    private boolean[] trail;
    private int[] hint;
    private String last = "";  // outcome of the previous run, kept in the description
    private int hintSteps;
    private boolean starEaten;
    private ValueAnimator anim;
    private float confetti = -1;  // 0..1 while celebrating
    private final float[][] bits = new float[40][5];

    BoardView(Context ctx) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
    }

    void setLevel(Level l) {
        level = l;
        hint = null;
        last = "";
        reset();
    }

    /** Robot back on the start square, calm, trail wiped. */
    void reset() {
        if (anim != null) anim.cancel();
        robotX = level.start % level.w;
        robotY = level.start / level.w;
        face = Art.FACE_CALM;
        look = -1;
        trail = new boolean[level.w * level.h];
        starEaten = false;
        confetti = -1;
        describe("ready");
        invalidate();
    }

    /** Ghost dots along the first {@code steps} moves of a shortest route. */
    void showHint(int[] solution, int steps) {
        hint = solution;
        hintSteps = Math.min(steps, solution.length);
        invalidate();
    }

    boolean isRunning() {
        return anim != null && anim.isRunning();
    }

    /** "board level 3 ready, last bump 1": what TalkBack reads, and what tools/verify.py checks. */
    private void describe(String state) {
        if (!state.equals("ready") && !state.equals("running")) last = state;
        setContentDescription("board level " + level.number + " " + state
                + (last.isEmpty() || last.equals(state) ? "" : ", last " + last));
    }

    void run(final int[] program, final Listener l) {
        reset();
        describe("running");
        stepFrom(level.start, program, 0, l);
    }

    private void stepFrom(final int cell, final int[] program, final int i, final Listener l) {
        if (i >= program.length) {
            face = Art.FACE_THINK;
            describe("short");
            invalidate();
            l.onShort();
            return;
        }
        final int d = program[i];
        final int next = level.step(cell, d);
        final float fx = cell % level.w, fy = cell / level.w;
        look = d;
        l.onStep(i);
        if (next < 0) {
            // Bump: lurch a third of the way, then spring back dizzy.
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(STEP_MS);
            anim.setInterpolator(new LinearInterpolator());
            anim.addUpdateListener(a -> {
                float t = (Float) a.getAnimatedValue();
                float k = (float) Math.sin(t * Math.PI) * 0.32f;
                robotX = fx + Level.DX[d] * k;
                robotY = fy + Level.DY[d] * k;
                if (t > 0.45f) face = Art.FACE_OOPS;
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
                    robotX = fx;
                    robotY = fy;
                    describe("bump " + i);
                    invalidate();
                    l.onBump(i);
                }
            });
            anim.start();
            return;
        }
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(STEP_MS);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float t = (Float) a.getAnimatedValue();
            robotX = fx + Level.DX[d] * t;
            robotY = fy + Level.DY[d] * t;
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
                trail[cell] = true;
                if (next == level.goal) {
                    win(i + 1, l);
                } else {
                    stepFrom(next, program, i + 1, l);
                }
            }
        });
        anim.start();
    }

    private void win(int used, Listener l) {
        face = Art.FACE_HAPPY;
        look = -1;
        starEaten = true;
        describe("won");
        Random rnd = new Random();
        for (float[] b : bits) {
            b[0] = rnd.nextFloat();                 // x
            b[1] = -0.1f - rnd.nextFloat() * 0.3f;  // y start
            b[2] = 0.6f + rnd.nextFloat() * 0.8f;   // speed
            b[3] = rnd.nextInt(6);                  // colour
            b[4] = rnd.nextFloat() * 360;           // spin
        }
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1800);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            confetti = (Float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
        l.onWin(used);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (level == null) return;
        float w = getWidth(), h = getHeight();
        float cell = Math.min(w / level.w, h / level.h);
        float bw = cell * level.w, bh = cell * level.h;
        float ox = (w - bw) / 2f, oy = (h - bh) / 2f;
        float pad = cell * 0.05f;

        // Board: a soft green mat with a darker lip.
        paint.setColor(Art.MINT_D);
        r.set(ox, oy + 5 * dp, ox + bw, oy + bh + 5 * dp);
        c.drawRoundRect(r, cell * 0.25f, cell * 0.25f, paint);
        paint.setColor(0xFFD9F7E9);
        r.set(ox, oy, ox + bw, oy + bh);
        c.drawRoundRect(r, cell * 0.25f, cell * 0.25f, paint);
        for (int y = 0; y < level.h; y++) {
            for (int x = 0; x < level.w; x++) {
                int i = y * level.w + x;
                paint.setColor(trail[i] ? 0xFFFFF1B8 : (x + y) % 2 == 0 ? 0xFFFFFFFF : 0xFFF0FBF5);
                r.set(ox + x * cell + pad, oy + y * cell + pad,
                        ox + (x + 1) * cell - pad, oy + (y + 1) * cell - pad);
                c.drawRoundRect(r, cell * 0.18f, cell * 0.18f, paint);
                if (level.rock[i]) Art.rock(c, ox + (x + .5f) * cell, oy + (y + .5f) * cell, cell * 0.85f);
            }
        }

        // Hint: dotted footprints along the first few moves.
        if (hint != null) {
            int at = level.start;
            paint.setColor(0x997CC8FF);
            for (int k = 0; k < hintSteps; k++) {
                int nx = level.step(at, hint[k]);
                if (nx < 0) break;
                float ax = at % level.w + .5f, ay = at / level.w + .5f;
                float bx = nx % level.w + .5f, by = nx / level.w + .5f;
                for (int dot = 1; dot <= 3; dot++) {
                    float t = dot / 3f;
                    c.drawCircle(ox + (ax + (bx - ax) * t) * cell, oy + (ay + (by - ay) * t) * cell,
                            cell * (dot == 3 ? 0.09f : 0.06f), paint);
                }
                at = nx;
            }
        }

        // The star, gently pulsing, until the robot reaches it.
        float gx = ox + (level.goal % level.w + .5f) * cell, gy = oy + (level.goal / level.w + .5f) * cell;
        if (!starEaten) {
            float pulse = 1f + 0.06f * (float) Math.sin(System.currentTimeMillis() / 220.0);
            Art.star(c, gx, gy, cell * 0.36f * pulse, Art.YELLOW, Art.YELLOW_D);
            if (isShown()) postInvalidateDelayed(50);
        }

        float rx = ox + (robotX + .5f) * cell, ry = oy + (robotY + .5f) * cell;
        float bob = starEaten ? -Math.abs((float) Math.sin(confetti * Math.PI * 6)) * cell * 0.12f : 0;
        Art.robot(c, rx, ry + bob, cell * 0.78f, face, look);
        if (starEaten) Art.star(c, rx + cell * 0.32f, ry - cell * 0.38f + bob, cell * 0.16f, Art.YELLOW, Art.YELLOW_D);

        if (confetti >= 0 && confetti < 1) {
            int[] colours = {Art.PINK, Art.YELLOW, Art.LILAC, Art.BLUE, Art.MINT, Art.PEACH};
            for (float[] b : bits) {
                float x = ox + b[0] * bw, y = oy + (b[1] + confetti * b[2] * 1.3f) * bh;
                if (y > oy + bh) continue;
                paint.setColor(colours[(int) b[3]]);
                c.save();
                c.rotate(b[4] + confetti * 540, x, y);
                r.set(x - cell * 0.06f, y - cell * 0.03f, x + cell * 0.06f, y + cell * 0.03f);
                c.drawRect(r, paint);
                c.restore();
            }
        }
    }
}
