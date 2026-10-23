package com.lilifriends.games;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.Locale;

/**
 * A game's journey: a winding path of numbered stops. Every stop the child has reached is open
 * and can be played again; the ones after it are locked. Lili stands on the furthest stop, and
 * the map opens scrolled to her.
 */
public class MapActivity extends Activity {
    static final String EXTRA_GAME = "game";

    private int player;
    private Game game;
    private Store store;
    private SharedPreferences progress;
    private ScrollView scroll;
    private MapView map;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        player = getIntent().getIntExtra(ProfilesActivity.EXTRA_PLAYER, 1);
        game = Game.byId(getIntent().getStringExtra(EXTRA_GAME));
        store = new Store(this);
        progress = store.progress(player, game);
        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Draw.SKY_TOP);
        TopBar bar = new TopBar(this, face, getString(game.title), store.avatar(player));
        bar.setOnBack(this::finish);
        int sb = getResources().getIdentifier("status_bar_height", "dimen", "android");
        bar.setPadding(0, sb > 0 ? getResources().getDimensionPixelSize(sb) : 0, 0, 0);
        root.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        map = new MapView(this, face);
        scroll.addView(map);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.refresh();
        // Bring Lili's stop into view once the map has its new size.
        scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, (int) map.yOf(map.furthest) - scroll.getHeight() / 2)));
    }

    private void play(int stop) {
        startActivity(game.start(this, player, stop, progress));
    }

    private class MapView extends View {
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint trail = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float dp, gap, top, r;
        private final Bitmap lili;
        int shown = 1, furthest = 1;
        private int down = -1;

        MapView(Context ctx, Typeface face) {
            super(ctx);
            dp = getResources().getDisplayMetrics().density;
            gap = 104 * dp;
            top = 136 * dp;
            r = 32 * dp;
            text.setTypeface(face);
            text.setTextAlign(Paint.Align.CENTER);
            trail.setStyle(Paint.Style.STROKE);
            trail.setStrokeCap(Paint.Cap.ROUND);
            trail.setStrokeWidth(9 * dp);
            trail.setPathEffect(new DashPathEffect(new float[]{1, 20 * dp}, 0));
            lili = Draw.load(getResources(), R.drawable.home_calm);
        }

        void refresh() {
            furthest = game.furthest(progress);
            shown = game.shown(progress);
            setContentDescription("map " + game.id + " furthest " + furthest + " of " + shown);
            requestLayout();
            invalidate();
        }

        float yOf(int n) {
            return top + (n - 1) * gap;
        }

        float xOf(int n) {
            float w = getWidth();
            return w / 2f + (float) Math.sin(n * 0.95) * Math.min(w * 0.28f, 170 * dp);
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int h = Math.round(yOf(shown) + gap);
            setMeasuredDimension(MeasureSpec.getSize(wSpec), Math.max(h, MeasureSpec.getSize(hSpec)));
        }

        private int hit(float x, float y) {
            for (int n = 1; n <= shown; n++) {
                float dx = x - xOf(n), dy = y - yOf(n);
                if (dx * dx + dy * dy <= r * r * 1.6f) return n;
            }
            return -1;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down = hit(e.getX(), e.getY());
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (down >= 0 && hit(e.getX(), e.getY()) != down) {
                        down = -1;
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    int n = down;
                    down = -1;
                    invalidate();
                    if (n >= 1 && n <= furthest) play(n);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    down = -1;
                    invalidate();
                    return true;
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas c) {
            // The trail: coloured up to Lili, grey beyond.
            for (int n = 1; n < shown; n++) {
                path.reset();
                float ax = xOf(n), ay = yOf(n), bx = xOf(n + 1), by = yOf(n + 1);
                path.moveTo(ax, ay);
                path.cubicTo(ax, ay + gap * 0.5f, bx, by - gap * 0.5f, bx, by);
                trail.setColor(n < furthest ? game.deep : 0xFFC9CDE0);
                c.drawPath(path, trail);
            }
            for (int n = 1; n <= shown; n++) {
                float x = xOf(n), y = yOf(n);
                boolean open = n <= furthest, here = n == furthest;
                float s = (n == down ? 0.9f : 1f) * (here ? 1.18f : 1f);
                c.save();
                c.scale(s, s, x, y);
                if (open) {
                    c.drawCircle(x, y + 5 * dp, r, Draw.fill(game.deep));
                    c.drawCircle(x, y, r, Draw.fill(here ? Draw.YELLOW : game.fill));
                    c.drawCircle(x - r * 0.35f, y - r * 0.4f, r * 0.15f, Draw.fill(0x66FFFFFF));
                    text.setColor(here ? Draw.PLUM : 0xFFFFFFFF);
                    text.setTextSize(r * 0.95f);
                    String label = String.format(Locale.getDefault(), "%d", n);
                    c.drawText(label, x, y - (text.ascent() + text.descent()) / 2f, text);
                    if (!here) Draw.star(c, x + r * 0.72f, y - r * 0.72f, r * 0.3f, Draw.YELLOW_D);
                } else {
                    c.drawCircle(x, y + 5 * dp, r, Draw.fill(0xFFB9BED3));
                    c.drawCircle(x, y, r, Draw.fill(0xFFE4E7F1));
                    Draw.lock(c, x, y, r * 0.9f, 0xFF9AA0BC);
                }
                c.restore();
            }
            // Lili waits on the furthest stop.
            float lx = xOf(furthest), ly = yOf(furthest);
            float lh = r * 2.4f;
            Draw.picture(c, lili, lx - lh * 0.4f, ly - r * 1.1f - lh, lx + lh * 0.4f, ly - r * 1.1f);
        }
    }
}
