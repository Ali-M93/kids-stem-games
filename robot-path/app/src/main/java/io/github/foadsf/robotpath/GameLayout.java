package io.github.foadsf.robotpath;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Places the game's pieces for whatever screen it gets: a 4.3" KitKat phone in portrait, the
 * same phone sideways, or a tablet either way.
 *
 * Portrait: header, board, program, then a row of four arrows plus undo, then a wide Go.
 * Landscape: board on the left; program, a cross of arrows, undo and Go on the right.
 * Buttons stay between 56 and 96 dp, so they are always easy for a small finger.
 */
class GameLayout extends ViewGroup {
    View backdrop, home, title, board, program, up, down, left, right, undo, go;
    private final float dp;
    private int topInset;

    GameLayout(Context ctx) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
        setClipChildren(false);
    }

    void setTopInset(int px) {
        topInset = px;
        requestLayout();
    }

    private int px(float dps) {
        return Math.round(dps * dp);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void put(View v, int x, int y, int w, int h) {
        v.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        v.setTag(R.id.tag_frame, new int[]{x, y, w, h});
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws), h = MeasureSpec.getSize(hs);
        setMeasuredDimension(w, h);
        int pad = px(12), gap = px(8), header = px(56);
        int top = topInset + px(4);
        put(backdrop, 0, 0, w, h);
        put(home, pad, top, header, header);
        put(title, pad + header + gap, top, w - 2 * (pad + header + gap), header);
        int y0 = top + header + gap;

        if (w <= h * 1.05f) {
            // ---- portrait --------------------------------------------------------------
            int inner = w - 2 * pad;
            int btn = clamp((inner - 4 * gap) / 5, px(56), px(96));
            program.measure(MeasureSpec.makeMeasureSpec(inner, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int progH = program.getMeasuredHeight();
            int goH = clamp(btn, px(56), px(80));
            int boardH = h - y0 - progH - btn - goH - 3 * gap - pad;
            int boardS = Math.max(px(120), Math.min(inner, boardH));
            put(board, (w - boardS) / 2, y0, boardS, boardS);
            int y = y0 + boardS + gap + Math.max(0, (boardH - boardS) / 3);
            put(program, pad, y, inner, progH);
            y += progH + gap;
            int rowW = 5 * btn + 4 * gap, x = (w - rowW) / 2;
            View[] row = {left, up, down, right, undo};
            for (View v : row) {
                put(v, x, y, btn, btn);
                x += btn + gap;
            }
            y += btn + gap;
            put(go, (w - rowW) / 2, y, rowW, goH);
        } else {
            // ---- landscape -------------------------------------------------------------
            int availH = h - y0 - pad;
            int boardS = Math.min(availH, Math.round(w * 0.52f));
            put(board, pad, y0, boardS, boardS);
            int px0 = pad + boardS + px(16), panelW = w - px0 - pad;
            program.measure(MeasureSpec.makeMeasureSpec(panelW, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int progH = program.getMeasuredHeight();
            int btn = clamp(Math.min((panelW - 2 * gap) / 3, (availH - progH - 4 * gap) / 4),
                    px(48), px(96));
            int crossW = 3 * btn + 2 * gap;
            int cx = px0 + (panelW - crossW) / 2;
            int y = y0;
            put(program, px0, y, panelW, progH);
            y += progH + gap;
            put(up, cx + btn + gap, y, btn, btn);
            put(left, cx, y + btn + gap, btn, btn);
            put(right, cx + 2 * (btn + gap), y + btn + gap, btn, btn);
            put(down, cx + btn + gap, y + 2 * (btn + gap), btn, btn);
            y += 3 * (btn + gap);
            put(undo, cx, y, btn, btn);
            put(go, cx + btn + gap, y, 2 * btn + gap, btn);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int[] f = (int[]) v.getTag(R.id.tag_frame);
            if (f != null) v.layout(f[0], f[1], f[0] + f[2], f[1] + f[3]);
        }
    }
}
