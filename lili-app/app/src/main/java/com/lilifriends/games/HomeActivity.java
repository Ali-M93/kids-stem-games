package com.lilifriends.games;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/** The four games as big picture cards. A card opens that game's journey map for this child. */
public class HomeActivity extends Activity {
    private int player;
    private Typeface face;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        player = getIntent().getIntExtra(ProfilesActivity.EXTRA_PLAYER, 1);
        face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");
        Store store = new Store(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        // Keep everything inside the status and navigation bars on every Android version,
        // including Android 15+, which draws apps edge to edge.
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Draw.SKY_TOP);
        TopBar bar = new TopBar(this, face, getString(R.string.app_name), store.avatar(player));
        bar.setOnBack(this::finish);
        root.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(new CardsView(this), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void open(Game g) {
        startActivity(new Intent(this, MapActivity.class)
                .putExtra(ProfilesActivity.EXTRA_PLAYER, player)
                .putExtra(MapActivity.EXTRA_GAME, g.id));
    }

    private class CardsView extends View {
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sky = new Paint();
        private final float dp;
        private final Bitmap[] pics = new Bitmap[Game.ALL.length];
        private final float[][] box = new float[Game.ALL.length][4];
        private int down = -1;

        CardsView(Context ctx) {
            super(ctx);
            dp = getResources().getDisplayMetrics().density;
            text.setTypeface(face);
            text.setColor(0xFFFFFFFF);
            text.setTextAlign(Paint.Align.CENTER);
            for (int i = 0; i < pics.length; i++) pics[i] = Draw.load(getResources(), Game.ALL[i].picture);
            setContentDescription("games");
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            sky.setShader(new LinearGradient(0, 0, 0, h, Draw.SKY_TOP, Draw.SKY_BOTTOM, Shader.TileMode.CLAMP));
            boolean wide = w > h * 1.3f;
            int cols = wide ? 4 : 2, rows = wide ? 1 : 2;
            float pad = 14 * dp;
            float cw = (w - pad * (cols + 1)) / cols, ch = (h - pad * (rows + 1)) / rows;
            boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
            for (int i = 0; i < box.length; i++) {
                int col = i % cols, row = i / cols;
                if (rtl) col = cols - 1 - col;
                box[i][0] = pad + col * (cw + pad);
                box[i][1] = pad + row * (ch + pad);
                box[i][2] = box[i][0] + cw;
                box[i][3] = box[i][1] + ch;
            }
        }

        private int hit(float x, float y) {
            for (int i = 0; i < box.length; i++) {
                if (x >= box[i][0] && x <= box[i][2] && y >= box[i][1] && y <= box[i][3]) return i;
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
                case MotionEvent.ACTION_UP:
                    int i = down;
                    down = -1;
                    invalidate();
                    if (i >= 0 && hit(e.getX(), e.getY()) == i) open(Game.ALL[i]);
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
            c.drawRect(0, 0, getWidth(), getHeight(), sky);
            for (int i = 0; i < box.length; i++) {
                Game g = Game.ALL[i];
                float l = box[i][0], t = box[i][1], r = box[i][2], b = box[i][3];
                float s = i == down ? 0.94f : 1f;
                c.save();
                c.scale(s, s, (l + r) / 2, (t + b) / 2);
                Draw.card(c, l, t, r, b - 6 * dp, 24 * dp, g.fill, g.deep, 6 * dp);
                float labelH = Math.min(30 * dp, (b - t) * 0.16f);
                Draw.picture(c, pics[i], l + 10 * dp, t + 10 * dp, r - 10 * dp, b - 6 * dp - labelH * 1.6f);
                text.setTextSize(Math.min(labelH, (r - l) * 0.12f));
                c.drawText(getResources().getString(g.title), (l + r) / 2, b - 6 * dp - labelH * 0.55f, text);
                c.restore();
            }
        }
    }
}
