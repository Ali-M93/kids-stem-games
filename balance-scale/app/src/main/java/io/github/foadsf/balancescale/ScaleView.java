package io.github.foadsf.balancescale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * The balance: a post, a beam that tips towards the heavier side with a springy wobble, and two
 * hanging pans with the fruit stacked on them. Tapping a fruit she put on the right pan takes it
 * off again. While she drags a fruit from the basket, it follows her finger and the right pan
 * glows when it would land there.
 */
class ScaleView extends View {
    interface Listener {
        void onRemove(int index);
    }

    private static final float MAX_TILT = 20f, PER_APPLE = 7f;  // one apple must visibly tip it

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;
    private final List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
    private int locked;                 // the first `locked` fruits on the right came with the puzzle
    private float angle, velocity, target;
    private long lastFrame;
    private boolean balanced, latched;
    private int dragFruit = -1;
    private float dragX, dragY;
    private Listener listener;
    private String state = "";
    private final RectF rightPan = new RectF(), leftPan = new RectF();

    ScaleView(Context ctx) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
    }

    void setListener(Listener l) {
        listener = l;
    }

    /**
     * Latched: a peg holds the beam level whatever is on the pans, until she presses Weigh.
     * That makes her decide how many before she sees the answer.
     */
    void setLatched(boolean l) {
        if (latched == l) return;
        latched = l;
        int d = Weigh.weight(right) - Weigh.weight(left);
        target = l ? 0 : Math.max(-MAX_TILT, Math.min(MAX_TILT, d * PER_APPLE));
        if (!l && d != 0) velocity += Math.signum(d) * 60;  // released: it swings
        lastFrame = 0;
        describe();
        invalidate();
    }

    void setPans(List<Integer> l, List<Integer> rgt, int locked, boolean animate) {
        left.clear();
        left.addAll(l);
        right.clear();
        right.addAll(rgt);
        this.locked = locked;
        int d = Weigh.weight(right) - Weigh.weight(left);
        balanced = d == 0;
        target = latched ? 0 : Math.max(-MAX_TILT, Math.min(MAX_TILT, d * PER_APPLE));
        if (!animate || latched) {
            angle = target;
            velocity = 0;
        } else {
            velocity += d == 0 ? 0 : Math.signum(d) * 40;  // a little kick each time fruit lands
        }
        lastFrame = 0;
        describe();
        invalidate();
    }

    void setState(String s) {
        state = s;
        describe();
    }

    private void describe() {
        StringBuilder sb = new StringBuilder("scale ").append(state).append(": left");
        for (int f : left) sb.append(' ').append(Weigh.NAME[f]);
        sb.append(" = ").append(Weigh.weight(left)).append("; right");
        for (int f : right) sb.append(' ').append(Weigh.NAME[f]);
        sb.append(" = ").append(Weigh.weight(right)).append("; ");
        int d = Weigh.weight(right) - Weigh.weight(left);
        sb.append(d == 0 ? "balanced" : d > 0 ? "right heavier" : "left heavier");
        if (latched) sb.append("; latched");
        setContentDescription(sb.toString());
    }

    // ---- dragging from the basket (coordinates are this view's) ----------------------------

    void drag(int fruit, float x, float y) {
        dragFruit = fruit;
        dragX = x;
        dragY = y;
        invalidate();
    }

    /** Ends a drag; true if the fruit was let go over the right pan. */
    boolean drop() {
        boolean over = dragFruit >= 0 && overRight(dragX, dragY);
        dragFruit = -1;
        invalidate();
        return over;
    }

    private boolean overRight(float x, float y) {
        RectF z = new RectF(rightPan);
        z.inset(-z.width() * 0.25f, -z.height() * 1.6f);  // generous: small fingers, moving target
        return z.contains(x, y);
    }

    // ---- taps on the right pan's own fruit -------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) return true;
        if (e.getActionMasked() != MotionEvent.ACTION_UP) return true;
        // Nearest removable fruit on the right pan, within a finger's reach.
        int best = -1;
        float bestD = 36 * dp;
        float[] pos = new float[2];
        for (int i = locked; i < right.size(); i++) {
            fruitPos(rightPan, right, i, pos);
            float d = (float) Math.hypot(pos[0] - e.getX(), pos[1] - e.getY());
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        if (best >= 0 && listener != null) {
            listener.onRemove(best);
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    // ---- drawing ----------------------------------------------------------------------------

    private float fruitSize(RectF pan) {
        return Math.min(pan.width() / 3.3f, 64 * dp);
    }

    /** Fruit i of a pan sits in rows of up to three, bottom row first. */
    private void fruitPos(RectF pan, List<Integer> fruit, int i, float[] out) {
        float s = fruitSize(pan);
        int perRow = 3, row = i / perRow, inRow = Math.min(perRow, fruit.size() - row * perRow);
        int col = i % perRow;
        float rowW = inRow * s * 0.92f;
        out[0] = pan.centerX() - rowW / 2 + s * 0.46f + col * s * 0.92f;
        out[1] = pan.top - s * 0.4f - row * s * 0.78f;
    }

    private void step() {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        // A damped spring towards the target tilt.
        float acc = -40f * (angle - target) - 7f * velocity;
        velocity += acc * dt;
        angle += velocity * dt;
        if (Math.abs(angle - target) > 0.05f || Math.abs(velocity) > 0.05f) {
            postInvalidateOnAnimation();
        } else {
            angle = target;
            velocity = 0;
            lastFrame = 0;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        step();
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, pivotY = h * 0.30f;
        // Pans hang from the beam ends and must stay on screen even fully tipped.
        float half = Math.min(w * 0.29f, h * 0.85f);
        float string = h * 0.26f;
        float panW = Math.min(w * 0.40f, 260 * dp), panH = panW * 0.16f;

        // Post and base.
        paint.setColor(0xFFB98A5E);
        r.set(cx - 7 * dp, pivotY, cx + 7 * dp, h - 22 * dp);
        c.drawRoundRect(r, 6 * dp, 6 * dp, paint);
        paint.setColor(0xFF9A6D44);
        r.set(cx - w * 0.16f, h - 26 * dp, cx + w * 0.16f, h - 8 * dp);
        c.drawRoundRect(r, 9 * dp, 9 * dp, paint);

        // Beam ends, rotated about the pivot.
        double rad = Math.toRadians(angle);
        float dx = (float) Math.cos(rad) * half, dy = (float) Math.sin(rad) * half;
        float lx = cx - dx, ly = pivotY - dy, rx = cx + dx, ry = pivotY + dy;

        // Strings and pans (pans always hang level).
        leftPan.set(lx - panW / 2, ly + string, lx + panW / 2, ly + string + panH);
        rightPan.set(rx - panW / 2, ry + string, rx + panW / 2, ry + string + panH);
        paint.setColor(0xFF7A6A8A);
        paint.setStrokeWidth(2.5f * dp);
        for (RectF p : new RectF[]{leftPan, rightPan}) {
            float px = p.centerX(), top = p.top - string;
            c.drawLine(px, top, p.left + panW * 0.08f, p.top, paint);
            c.drawLine(px, top, p.right - panW * 0.08f, p.top, paint);
        }
        boolean glow = dragFruit >= 0 && overRight(dragX, dragY);
        drawPan(c, leftPan, Art.LILAC, Art.LILAC_D, false);
        drawPan(c, rightPan, Art.PEACH, Art.PEACH_D, glow);

        // Beam on top of the strings.
        c.save();
        c.rotate(angle, cx, pivotY);
        paint.setColor(Art.PLUM);
        r.set(cx - half - 6 * dp, pivotY - 7 * dp, cx + half + 6 * dp, pivotY + 7 * dp);
        c.drawRoundRect(r, 7 * dp, 7 * dp, paint);
        c.restore();
        if (latched) {  // the red peg that holds the beam
            paint.setColor(Art.PINK_D);
            r.set(cx - 5 * dp, pivotY - 30 * dp, cx + 5 * dp, pivotY + 26 * dp);
            c.drawRoundRect(r, 5 * dp, 5 * dp, paint);
            c.drawCircle(cx, pivotY - 30 * dp, 9 * dp, paint);
        }
        paint.setColor(Art.YELLOW_D);
        c.drawCircle(cx, pivotY, 11 * dp, paint);
        paint.setColor(Art.YELLOW);
        c.drawCircle(cx, pivotY, 7 * dp, paint);

        // Fruit.
        float[] pos = new float[2];
        float fs = fruitSize(leftPan);
        for (int i = 0; i < left.size(); i++) {
            fruitPos(leftPan, left, i, pos);
            Art.fruit(c, pos[0], pos[1], fs, left.get(i), 255);
        }
        for (int i = 0; i < right.size(); i++) {
            fruitPos(rightPan, right, i, pos);
            Art.fruit(c, pos[0], pos[1], fs, right.get(i), 255);
            if (i < locked) {  // fruit that came with the puzzle: a small dot says "this one stays"
                paint.setColor(0x80FFFFFF);
                c.drawCircle(pos[0] + fs * 0.32f, pos[1] + fs * 0.3f, 3 * dp, paint);
            }
        }

        if (balanced && !latched && Math.abs(angle) < 1f) Art.equals(c, cx, pivotY - 44 * dp, 64 * dp, Art.MINT_D);

        // The fruit being dragged, a little bigger, under her finger but lifted so she sees it.
        if (dragFruit >= 0) Art.fruit(c, dragX, dragY - 40 * dp, fs * 1.3f, dragFruit, 230);
    }

    private void drawPan(Canvas c, RectF p, int fill, int deep, boolean glow) {
        if (glow) {
            paint.setColor(0x66FFD166);
            r.set(p.left - 12 * dp, p.top - p.width() * 0.5f, p.right + 12 * dp, p.bottom + 10 * dp);
            c.drawRoundRect(r, 24 * dp, 24 * dp, paint);
        }
        paint.setColor(deep);
        r.set(p.left, p.top, p.right, p.bottom + p.height() * 0.6f);
        c.drawArc(r, 0, 180, true, paint);
        paint.setColor(fill);
        r.set(p.left, p.top - p.height() * 0.4f, p.right, p.top + p.height() * 0.6f);
        c.drawOval(r, paint);
    }
}
