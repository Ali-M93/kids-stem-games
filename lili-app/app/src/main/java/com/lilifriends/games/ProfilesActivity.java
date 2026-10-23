package com.lilifriends.games;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

/**
 * "Who is playing?": one big badge per child on this device, and a "+" to add one. Nothing to
 * type, so a child who cannot read yet can still find their own badge. Grown-ups remove a
 * player (and all their progress) by pressing and holding the badge.
 */
public class ProfilesActivity extends Activity {
    static final String EXTRA_PLAYER = "player";

    private Store store;
    private Typeface face;
    private PlayersView view;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        store = new Store(this);
        face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");
        view = new PlayersView(this);
        // Keep everything inside the status and navigation bars on every Android version.
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(Draw.SKY_TOP);
        root.setFitsSystemWindows(true);
        root.addView(view);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.refresh();
    }

    private void open(int player) {
        startActivity(new Intent(this, HomeActivity.class).putExtra(EXTRA_PLAYER, player));
    }

    private void confirmRemove(final int player) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.remove_q)
                .setPositiveButton(R.string.remove, (d, w) -> {
                    store.remove(player);
                    view.refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private class PlayersView extends View {
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sky = new Paint();
        private final float dp;
        private final Bitmap lili;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private List<Integer> players;
        private float[] cx = new float[0], cy = new float[0];
        private float radius;
        private int down = -1;
        private boolean longFired;
        private final Runnable longPress = () -> {
            if (down >= 0 && down < players.size()) {
                longFired = true;
                confirmRemove(players.get(down));
                down = -1;
                invalidate();
            }
        };

        PlayersView(Context ctx) {
            super(ctx);
            dp = getResources().getDisplayMetrics().density;
            text.setTypeface(face);
            text.setColor(Draw.PLUM);
            text.setTextAlign(Paint.Align.CENTER);
            small.setTypeface(face);
            small.setColor(0x995B3E8C);
            small.setTextAlign(Paint.Align.CENTER);
            lili = Draw.load(getResources(), R.drawable.home_happy);
        }

        void refresh() {
            players = store.players();
            setContentDescription(getResources().getString(R.string.who_plays) + " " + players.size());
            requestLayout();
            invalidate();
        }

        private int count() {
            return players.size() + (players.size() < Store.MAX_PLAYERS ? 1 : 0);
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            sky.setShader(new LinearGradient(0, 0, 0, h, Draw.SKY_TOP, Draw.SKY_BOTTOM, Shader.TileMode.CLAMP));
        }

        private void layoutBadges(float w, float h) {
            int n = count();
            boolean wide = w > h;
            int cols = wide ? Math.min(n, 4) : Math.min(n, 2);
            if (n == 1) cols = 1;
            int rows = (n + cols - 1) / cols;
            float top = h * (wide ? 0.30f : 0.34f), bottom = h * 0.88f;
            float cell = Math.min(w * 0.9f / cols, (bottom - top) / rows);
            radius = Math.min(cell * 0.36f, 80 * dp);
            cx = new float[n];
            cy = new float[n];
            for (int i = 0; i < n; i++) {
                int row = i / cols, col = i % cols;
                int inRow = Math.min(cols, n - row * cols);
                float x0 = (w - inRow * cell) / 2f;
                cx[i] = x0 + (col + 0.5f) * cell;
                cy[i] = top + (row + 0.5f) * cell;
            }
        }

        private int hit(float x, float y) {
            for (int i = 0; i < cx.length; i++) {
                float dx = x - cx[i], dy = y - cy[i];
                if (dx * dx + dy * dy <= radius * radius * 1.3f) return i;
            }
            return -1;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down = hit(e.getX(), e.getY());
                    longFired = false;
                    if (down >= 0 && down < players.size()) handler.postDelayed(longPress, 900);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (down >= 0 && hit(e.getX(), e.getY()) != down) {
                        handler.removeCallbacks(longPress);
                        down = -1;
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(longPress);
                    int i = down;
                    down = -1;
                    invalidate();
                    if (longFired || i < 0) return true;
                    if (i < players.size()) {
                        open(players.get(i));
                    } else {
                        int id = store.add();
                        refresh();
                        if (id >= 0 && players.size() == 1) open(id);  // the very first child goes straight in
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(longPress);
                    down = -1;
                    invalidate();
                    return true;
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            c.drawRect(0, 0, w, h, sky);
            if (players == null) return;
            layoutBadges(w, h);
            boolean wide = w > h;

            // Lili says hello at the top, with the question beside her.
            float lh = h * (wide ? 0.24f : 0.2f) - getPaddingTop() * 0.5f;
            float lt = getPaddingTop() + h * 0.01f;
            float titleY = h * (wide ? 0.13f : 0.25f);
            if (wide) {
                Draw.picture(c, lili, w * 0.05f, lt, w * 0.05f + lh * 0.8f, lt + lh);
            } else {
                Draw.picture(c, lili, w / 2f - lh * 0.45f, lt, w / 2f + lh * 0.45f, lt + lh);
            }
            text.setTextSize(Math.min(36 * dp, w * 0.085f));
            c.drawText(getResources().getString(R.string.who_plays), w / 2f, titleY, text);

            for (int i = 0; i < cx.length; i++) {
                float s = i == down ? 0.92f : 1f;
                c.save();
                c.scale(s, s, cx[i], cy[i]);
                if (i < players.size()) {
                    Draw.avatar(c, cx[i], cy[i], radius, store.avatar(players.get(i)));
                } else {
                    c.drawCircle(cx[i], cy[i] + radius * 0.08f, radius, Draw.fill(0xFFC9CDE0));
                    c.drawCircle(cx[i], cy[i], radius, Draw.fill(0xFFFFFFFF));
                    Draw.stroke(0xFFC9CDE0, radius * 0.12f);
                    c.drawLine(cx[i] - radius * 0.4f, cy[i], cx[i] + radius * 0.4f, cy[i], Draw.stroke(Draw.MINT_D, radius * 0.16f));
                    c.drawLine(cx[i], cy[i] - radius * 0.4f, cx[i], cy[i] + radius * 0.4f, Draw.stroke(Draw.MINT_D, radius * 0.16f));
                }
                c.restore();
            }
            if (!players.isEmpty()) {
                small.setTextSize(Math.min(15 * dp, w * 0.035f));
                c.drawText(getResources().getString(R.string.grown_ups_hint), w / 2f, h - 18 * dp, small);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            handler.removeCallbacksAndMessages(null);
            super.onDetachedFromWindow();
        }
    }
}
