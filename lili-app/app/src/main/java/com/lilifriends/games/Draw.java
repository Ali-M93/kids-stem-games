package com.lilifriends.games;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Shared drawing for the app's own screens: the palette, player avatars, locks and pictures. */
final class Draw {
    static final int PINK = 0xFFFF8FA3, PINK_D = 0xFFE8607E;
    static final int YELLOW = 0xFFFFD166, YELLOW_D = 0xFFF0A830;
    static final int LILAC = 0xFFB79CFF, LILAC_D = 0xFF8466E0;
    static final int BLUE = 0xFF7CC8FF, BLUE_D = 0xFF3F95E0;
    static final int MINT = 0xFF7EE0B5, MINT_D = 0xFF34B57F;
    static final int PEACH = 0xFFFFAD7A, PEACH_D = 0xFFEE7F45;
    static final int PLUM = 0xFF5B3E8C;
    static final int SKY_TOP = 0xFFFFF6E0, SKY_BOTTOM = 0xFFE3F6FF;

    static final int AVATARS = 6;
    private static final int[] AV_FILL = {YELLOW, PINK, MINT, LILAC, PEACH, BLUE};
    private static final int[] AV_DEEP = {YELLOW_D, PINK_D, MINT_D, LILAC_D, PEACH_D, BLUE_D};

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint BMP = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final RectF RR = new RectF();
    private static final Path PATH = new Path();

    static Paint fill(int color) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        return P;
    }

    static Paint stroke(int color, float w) {
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(w);
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        P.setColor(color);
        return P;
    }

    static Bitmap load(Resources res, int id) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        return BitmapFactory.decodeResource(res, id, o);
    }

    /** Draws {@code b} fitted inside the box, centred, keeping its shape. */
    static void picture(Canvas c, Bitmap b, float l, float t, float r, float bottom) {
        if (b == null) return;
        float bw = r - l, bh = bottom - t;
        float k = Math.min(bw / b.getWidth(), bh / b.getHeight());
        float w = b.getWidth() * k, h = b.getHeight() * k;
        RR.set(l + (bw - w) / 2f, bottom - h, l + (bw + w) / 2f, bottom);
        c.drawBitmap(b, null, RR, BMP);
    }

    /** A candy card with a darker lip, like the games' buttons. */
    static void card(Canvas c, float l, float t, float r, float b, float radius, int fill, int deep, float lip) {
        RR.set(l, t + lip, r, b + lip);
        c.drawRoundRect(RR, radius, radius, fill(deep));
        RR.set(l, t, r, b);
        c.drawRoundRect(RR, radius, radius, fill(fill));
    }

    /** Player avatar {@code kind}: a coloured badge with a star, heart, flower, moon, sun or cloud. */
    static void avatar(Canvas c, float cx, float cy, float r, int kind) {
        int k = ((kind % AVATARS) + AVATARS) % AVATARS;
        c.drawCircle(cx, cy + r * 0.08f, r, fill(AV_DEEP[k]));
        c.drawCircle(cx, cy, r, fill(AV_FILL[k]));
        c.drawCircle(cx - r * 0.35f, cy - r * 0.4f, r * 0.16f, fill(0x55FFFFFF));
        float u = r / 5f;
        int w = 0xFFFFFFFF;
        switch (k) {
            case 0: star(c, cx, cy, 3.0f * u, w);
                break;
            case 1: // heart
                PATH.reset();
                PATH.moveTo(cx, cy + 2.6f * u);
                PATH.cubicTo(cx - 4.2f * u, cy - 0.2f * u, cx - 2.2f * u, cy - 3.8f * u, cx, cy - 1.4f * u);
                PATH.cubicTo(cx + 2.2f * u, cy - 3.8f * u, cx + 4.2f * u, cy - 0.2f * u, cx, cy + 2.6f * u);
                c.drawPath(PATH, fill(w));
                break;
            case 2: // flower
                for (int i = 0; i < 5; i++) {
                    double a = Math.toRadians(i * 72 - 90);
                    c.drawCircle(cx + (float) Math.cos(a) * 1.8f * u, cy + (float) Math.sin(a) * 1.8f * u,
                            1.3f * u, fill(w));
                }
                c.drawCircle(cx, cy, 1.1f * u, fill(YELLOW_D));
                break;
            case 3: // moon
                c.drawCircle(cx, cy, 2.8f * u, fill(w));
                c.drawCircle(cx + 1.4f * u, cy - 1.0f * u, 2.4f * u, fill(AV_FILL[k]));
                break;
            case 4: // sun
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    c.drawLine(cx + (float) Math.cos(a) * 2.2f * u, cy + (float) Math.sin(a) * 2.2f * u,
                            cx + (float) Math.cos(a) * 3.4f * u, cy + (float) Math.sin(a) * 3.4f * u,
                            stroke(w, 0.7f * u));
                }
                c.drawCircle(cx, cy, 1.8f * u, fill(w));
                break;
            default: // cloud
                c.drawCircle(cx - 1.5f * u, cy + 0.4f * u, 1.5f * u, fill(w));
                c.drawCircle(cx + 0.2f * u, cy - 0.6f * u, 2.0f * u, fill(w));
                c.drawCircle(cx + 1.9f * u, cy + 0.6f * u, 1.3f * u, fill(w));
                RR.set(cx - 3.0f * u, cy + 0.4f * u, cx + 3.2f * u, cy + 1.9f * u);
                c.drawRoundRect(RR, 0.8f * u, 0.8f * u, fill(w));
        }
    }

    static void star(Canvas c, float cx, float cy, float r, int color) {
        PATH.reset();
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-90 + i * 36);
            float rr = i % 2 == 0 ? r : r * 0.48f;
            float x = cx + (float) Math.cos(a) * rr, y = cy + (float) Math.sin(a) * rr;
            if (i == 0) PATH.moveTo(x, y);
            else PATH.lineTo(x, y);
        }
        PATH.close();
        c.drawPath(PATH, stroke(color, r * 0.18f));
        c.drawPath(PATH, fill(color));
    }

    /** A small padlock centred on (cx, cy), {@code s} tall. */
    static void lock(Canvas c, float cx, float cy, float s, int color) {
        float u = s / 10f;
        RR.set(cx - 2.6f * u, cy - 5.0f * u, cx + 2.6f * u, cy + 0.5f * u);
        c.drawArc(RR, 180, 180, false, stroke(color, 1.2f * u));
        c.drawLine(cx - 2.6f * u, cy - 2.2f * u, cx - 2.6f * u, cy, P);
        c.drawLine(cx + 2.6f * u, cy - 2.2f * u, cx + 2.6f * u, cy, P);
        RR.set(cx - 4.0f * u, cy - 0.6f * u, cx + 4.0f * u, cy + 5.0f * u);
        c.drawRoundRect(RR, 1.2f * u, 1.2f * u, fill(color));
        c.drawCircle(cx, cy + 2.0f * u, 0.9f * u, fill(0x66FFFFFF));
    }

    /** A chunky arrow pointing back (left in LTR, right in RTL). */
    static void back(Canvas c, float cx, float cy, float s, int color, boolean rtl) {
        float u = s / 10f, d = rtl ? -1 : 1;
        PATH.reset();
        PATH.moveTo(cx - 3.6f * u * d, cy);
        PATH.lineTo(cx + 0.2f * u * d, cy - 3.6f * u);
        PATH.lineTo(cx + 0.2f * u * d, cy - 1.4f * u);
        PATH.lineTo(cx + 3.8f * u * d, cy - 1.4f * u);
        PATH.lineTo(cx + 3.8f * u * d, cy + 1.4f * u);
        PATH.lineTo(cx + 0.2f * u * d, cy + 1.4f * u);
        PATH.lineTo(cx + 0.2f * u * d, cy + 3.6f * u);
        PATH.close();
        c.drawPath(PATH, stroke(color, 1.0f * u));
        c.drawPath(PATH, fill(color));
    }

    private Draw() {
    }
}
