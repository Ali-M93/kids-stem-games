package io.github.foadsf.robotpath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * The program: a row of slots that fill left to right as she taps arrows. Tapping a filled slot
 * takes that step out, which is how she "fixes the program" after a bump. Wraps onto a second row
 * when it is too narrow. Always left-to-right, even in Farsi: it is a sequence in time, and the
 * arrows on it are absolute directions on the board.
 */
class ProgramView extends View {
    interface Listener {
        void onChanged();
    }

    static final int MARK_NONE = 0, MARK_NOW = 1, MARK_ERROR = 2, MARK_NEXT = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;
    private final List<Integer> steps = new ArrayList<>();
    private int slots = 3, mark = -1, markKind = MARK_NONE, cols = 1;
    private float slot, gap;
    private boolean locked;
    private Listener listener;

    ProgramView(Context ctx) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
    }

    void setListener(Listener l) {
        listener = l;
    }

    void setSlots(int n) {
        slots = n;
        steps.clear();
        clearMark();
        requestLayout();
        update();
    }

    int slots() {
        return slots;
    }

    boolean add(int dir) {
        if (locked || steps.size() >= slots) return false;
        steps.add(dir);
        clearMark();
        update();
        return true;
    }

    boolean removeLast() {
        if (locked || steps.isEmpty()) return false;
        steps.remove(steps.size() - 1);
        clearMark();
        update();
        return true;
    }

    int size() {
        return steps.size();
    }

    int[] program() {
        int[] p = new int[steps.size()];
        for (int i = 0; i < p.length; i++) p[i] = steps.get(i);
        return p;
    }

    void setLocked(boolean l) {
        locked = l;
    }

    void mark(int index, int kind) {
        mark = index;
        markKind = kind;
        invalidate();
    }

    void clearMark() {
        mark(-1, MARK_NONE);
    }

    private void update() {
        StringBuilder sb = new StringBuilder("program");
        String[] names = {"up", "right", "down", "left"};
        for (int d : steps) sb.append(' ').append(names[d]);
        setContentDescription(sb.toString());
        invalidate();
        if (listener != null) listener.onChanged();
    }

    /** Height for a given width: slots shrink to 44 dp before wrapping onto more rows. */
    @Override
    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        // 64 dp on a phone, growing to 96 dp on a tablet so the program matches the bigger board.
        float max = Math.max(64 * dp, Math.min(96 * dp, w / 8f)), min = 44 * dp;
        gap = 6 * dp;
        slot = Math.min(max, (w - (slots - 1) * gap) / slots);
        cols = slots;
        if (slot < min) {
            cols = (int) ((w + gap) / (min + gap));
            slot = Math.min(max, (w - (cols - 1) * gap) / cols);
        }
        int rows = (slots + cols - 1) / cols;
        int h = (int) Math.ceil(rows * slot + (rows - 1) * gap + 4 * dp);
        if (MeasureSpec.getMode(hs) == MeasureSpec.EXACTLY) h = MeasureSpec.getSize(hs);
        setMeasuredDimension(w, h);
    }

    private void cellRect(int i) {
        int rows = (slots + cols - 1) / cols;
        int row = i / cols, col = i % cols;
        int inRow = row == rows - 1 ? slots - row * cols : cols;
        float rowW = inRow * slot + (inRow - 1) * gap;
        float x = (getWidth() - rowW) / 2f + col * (slot + gap);
        float y = row * (slot + gap);
        r.set(x, y, x + slot, y + slot);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP && !locked) {
            for (int i = 0; i < steps.size(); i++) {
                cellRect(i);
                r.inset(-gap / 2, -gap / 2);
                if (r.contains(e.getX(), e.getY())) {
                    steps.remove(i);
                    clearMark();
                    update();
                    performClick();
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas c) {
        int[] fills = {Art.PINK, Art.YELLOW, Art.BLUE, Art.LILAC};
        int[] deeps = {Art.PINK_D, Art.YELLOW_D, Art.BLUE_D, Art.LILAC_D};
        for (int i = 0; i < slots; i++) {
            cellRect(i);
            float rad = slot * 0.22f;
            if (i < steps.size()) {
                int d = steps.get(i);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(deeps[d]);
                c.drawRoundRect(r, rad, rad, paint);
                r.bottom -= 3 * dp;
                paint.setColor(fills[d]);
                c.drawRoundRect(r, rad, rad, paint);
                Art.arrow(c, r.centerX(), r.centerY(), slot * 0.6f, d, 0xFFFFFFFF);
                r.bottom += 3 * dp;
            } else {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0x66FFFFFF);
                c.drawRoundRect(r, rad, rad, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2 * dp);
                paint.setColor(0x405B3E8C);
                r.inset(dp, dp);
                c.drawRoundRect(r, rad, rad, paint);
                r.inset(-dp, -dp);
            }
            if (i == mark && markKind != MARK_NONE) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4 * dp);
                paint.setColor(markKind == MARK_ERROR ? Art.RED
                        : markKind == MARK_NEXT ? Art.MINT_D : Art.PLUM);
                r.inset(-3 * dp, -3 * dp);
                c.drawRoundRect(r, rad + 3 * dp, rad + 3 * dp, paint);
            }
        }
    }
}
