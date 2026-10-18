package io.github.foadsf.robotpath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/** A big rounded button that squishes onto its darker lip when pressed, like the launcher's tiles. */
class CandyButton extends View {
    static final int ICON_ARROW = 0, ICON_PLAY = 1, ICON_UNDO = 2, ICON_HOME = 3, ICON_NONE = 4;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float dp;
    private final int icon, dir, fill, deep;
    private String label;

    CandyButton(Context ctx, int icon, int dir, int fill, int deep, String label, Typeface face) {
        super(ctx);
        this.icon = icon;
        this.dir = dir;
        this.fill = fill;
        this.deep = deep;
        this.label = label;
        dp = getResources().getDisplayMetrics().density;
        text.setTypeface(face);
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
        setFocusable(true);
    }

    void setLabel(String s) {
        label = s;
        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.4f);
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) return;
        animate().cancel();
        if (pressed) {
            animate().scaleX(0.9f).scaleY(0.9f).setDuration(60)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else {
            animate().scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(new OvershootInterpolator(4f)).start();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), edge = Math.max(3 * dp, h * 0.07f);
        float rad = Math.min(w, h) * 0.3f;
        float sink = isPressed() ? edge * 0.7f : 0;
        paint.setColor(deep);
        r.set(0, edge, w, h);
        c.drawRoundRect(r, rad, rad, paint);
        paint.setColor(fill);
        r.set(0, sink, w, h - edge + sink);
        c.drawRoundRect(r, rad, rad, paint);
        paint.setColor(0x4DFFFFFF);
        r.set(rad * 0.5f, sink + h * 0.08f, w * 0.5f, sink + h * 0.18f);
        c.drawRoundRect(r, h * 0.05f, h * 0.05f, paint);

        float bodyH = h - edge, cy = sink + bodyH / 2f;
        float s = Math.min(w, bodyH) * 0.62f;
        boolean withLabel = label != null && !label.isEmpty();
        float cx = withLabel ? Math.min(w / 2f, bodyH * 0.55f) : w / 2f;
        switch (icon) {
            case ICON_ARROW: Art.arrow(c, cx, cy, s, dir, 0xFFFFFFFF); break;
            case ICON_PLAY: Art.play(c, cx, cy, s, 0xFFFFFFFF); break;
            case ICON_UNDO: Art.undo(c, cx, cy, s * 0.9f, 0xFFFFFFFF); break;
            case ICON_HOME: Art.house(c, cx, cy, s, 0xFFFFFFFF); break;
            default: cx = w / 2f;
        }
        if (withLabel) {
            float left = icon == ICON_NONE ? 0 : cx + s * 0.55f;
            float room = w - left - 8 * dp;
            text.setTextSize(Math.min(bodyH * 0.42f, 34 * dp));
            float tw = text.measureText(label);
            if (tw > room) text.setTextSize(text.getTextSize() * room / tw);
            c.drawText(label, left + (w - left) / 2f - (icon == ICON_NONE ? 0 : 4 * dp),
                    cy - (text.ascent() + text.descent()) / 2f, text);
        }
    }
}
