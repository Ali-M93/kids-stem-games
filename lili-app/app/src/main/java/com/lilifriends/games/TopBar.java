package com.lilifriends.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** A title strip with a big back button on the start side (and the player's badge beside it). */
class TopBar extends View {
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private final String title;
    private final int avatar;
    private boolean pressed;
    private Runnable onBack;

    /** {@code avatar} < 0 for none. */
    TopBar(Context ctx, Typeface face, String title, int avatar) {
        super(ctx);
        this.title = title;
        this.avatar = avatar;
        dp = getResources().getDisplayMetrics().density;
        text.setTypeface(face);
        text.setColor(Draw.PLUM);
        text.setTextAlign(Paint.Align.CENTER);
        setContentDescription(getResources().getString(R.string.cd_back));
    }

    void setOnBack(Runnable r) {
        onBack = r;
    }

    private boolean rtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), Math.round(76 * dp) + getPaddingTop());
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float bw = (avatar >= 0 ? 130 : 80) * dp;
        boolean inBack = rtl() ? e.getX() > getWidth() - bw : e.getX() < bw;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = inBack;
                invalidate();
                return inBack;
            case MotionEvent.ACTION_UP:
                if (pressed && inBack && onBack != null) onBack.run();
                pressed = false;
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                invalidate();
                return true;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float s = 54 * dp, cy = getPaddingTop() + (h - getPaddingTop()) / 2f + 4 * dp;
        float bx = rtl() ? w - 14 * dp - s / 2 : 14 * dp + s / 2;
        float squish = pressed ? 0.9f : 1f;
        c.save();
        c.scale(squish, squish, bx, cy);
        Draw.card(c, bx - s / 2, cy - s / 2, bx + s / 2, cy + s / 2, s * 0.32f, Draw.PINK, Draw.PINK_D, 4 * dp);
        Draw.back(c, bx, cy, s * 0.6f, 0xFFFFFFFF, rtl());
        c.restore();
        float used = 14 * dp + s;  // width taken on the start side
        if (avatar >= 0) {
            float ax = bx + (rtl() ? -1 : 1) * (s / 2 + 34 * dp);
            Draw.avatar(c, ax, cy, 24 * dp, avatar);
            used += 10 * dp + 48 * dp;
        }
        // The title sits centred in the space left over, shrinking to fit; it never covers the badge.
        float room = w - used - 2 * 12 * dp - 14 * dp;
        float size = Math.min(28 * dp, w * 0.065f);
        text.setTextSize(size);
        float tw = text.measureText(title);
        if (tw > room && tw > 0) text.setTextSize(Math.max(14 * dp, size * room / tw));
        float tx = rtl() ? (w - used - 12 * dp + 14 * dp) / 2f : (used + 12 * dp + w - 14 * dp) / 2f;
        c.drawText(title, tx, cy - (text.ascent() + text.descent()) / 2f, text);
    }
}
