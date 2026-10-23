package io.github.foadsf.kangaroohops;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * The end of a round: after every three stars she gets a sticker, sees her collection, and
 * chooses "More!" or "Done". A round has a clear end on purpose; it is the opposite of an
 * endless feed.
 */
class StickerView extends View {
    private final Paint dim = new Paint();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private int count;
    private float pop = 1f;

    StickerView(Context ctx, Typeface face) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
        dim.setColor(0xCCFFFFFF);
        text.setTypeface(face);
        text.setColor(Art.PLUM);
        text.setTextAlign(Paint.Align.CENTER);
        setContentDescription("sticker");
    }

    void show(int stickers) {
        count = stickers;
        setVisibility(VISIBLE);
        setContentDescription("sticker " + stickers);
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(700);
        a.setInterpolator(new android.view.animation.OvershootInterpolator(2.5f));
        a.addUpdateListener(v -> {
            pop = (Float) v.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    /** Swallow touches so nothing underneath reacts while the sticker is up. */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        c.drawRect(0, 0, w, h, dim);
        float big = Math.min(w, h) * 0.42f;
        float cy = h * 0.34f;
        c.save();
        c.scale(pop, pop, w / 2f, cy);
        c.rotate(-8 + 8 * pop, w / 2f, cy);
        Art.sticker(c, w / 2f, cy, big, count - 1);
        c.restore();
        // Lili cheers beside the new sticker.
        Art.liliCheer(c, w / 2f - big * 0.62f, cy + big * 0.5f, big * 0.95f * pop);
        text.setTextSize(Math.min(w * 0.075f, 34 * dp));
        c.drawText(getResources().getString(R.string.sticker_title), w / 2f, cy + big * 0.62f, text);

        // Her collection so far, newest last, up to two rows.
        int show = Math.min(count, 12);
        float s = Math.min(w / 8f, 56 * dp), gap = s * 0.15f;
        int perRow = Math.max(1, (int) ((w - 2 * gap) / (s + gap)));
        perRow = Math.min(perRow, 6);
        float y = cy + big * 0.62f + 20 * dp + s / 2f;
        for (int i = 0; i < show; i++) {
            int row = i / perRow, col = i % perRow;
            int inRow = Math.min(perRow, show - row * perRow);
            float x0 = (w - inRow * s - (inRow - 1) * gap) / 2f;
            Art.sticker(c, x0 + col * (s + gap) + s / 2f, y + row * (s + gap), s, count - show + i);
        }
    }
}
