package io.github.foadsf.patternmachine;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Everything in the game is drawn here, as code: tokens, the robot, stars, buttons, stickers. */
final class Art {
    static final int PINK = 0xFFFF8FA3, PINK_D = 0xFFE8607E;
    static final int YELLOW = 0xFFFFD166, YELLOW_D = 0xFFF0A830;
    static final int LILAC = 0xFFB79CFF, LILAC_D = 0xFF8466E0;
    static final int BLUE = 0xFF7CC8FF, BLUE_D = 0xFF3F95E0;
    static final int MINT = 0xFF7EE0B5, MINT_D = 0xFF34B57F;
    static final int PEACH = 0xFFFFAD7A, PEACH_D = 0xFFEE7F45;
    static final int PLUM = 0xFF5B3E8C;
    static final int RED = 0xFFFF5A6E;

    /** Directions the robot can glance. */
    static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;

    /** Robot faces. */
    static final int FACE_CALM = 0, FACE_HAPPY = 1, FACE_OOPS = 2, FACE_THINK = 3;

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF R = new RectF();
    private static final Path PATH = new Path();
    private static final Paint BMP = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    /** Lili's faces, indexed by FACE_*, plus her cheering pose. Art © Lili &amp; Friends, not GPL. */
    private static final Bitmap[] LILI = new Bitmap[4];
    private static Bitmap liliCheer;

    /** Loads Lili's pictures once; call before anything is drawn. */
    static void load(Resources res) {
        if (liliCheer != null) return;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        LILI[FACE_CALM] = BitmapFactory.decodeResource(res, io.github.foadsf.patternmachine.R.drawable.lili_calm, o);
        LILI[FACE_HAPPY] = BitmapFactory.decodeResource(res, io.github.foadsf.patternmachine.R.drawable.lili_happy, o);
        LILI[FACE_OOPS] = BitmapFactory.decodeResource(res, io.github.foadsf.patternmachine.R.drawable.lili_oops, o);
        LILI[FACE_THINK] = BitmapFactory.decodeResource(res, io.github.foadsf.patternmachine.R.drawable.lili_think, o);
        liliCheer = BitmapFactory.decodeResource(res, io.github.foadsf.patternmachine.R.drawable.lili_cheer, o);
    }

    /** Draws {@code b} with its bottom-centre at (cx, bottom), {@code h} tall. */
    private static void picture(Canvas c, Bitmap b, float cx, float bottom, float h) {
        if (b == null) return;
        float w = h * b.getWidth() / b.getHeight();
        R.set(cx - w / 2f, bottom - h, cx + w / 2f, bottom);
        c.drawBitmap(b, null, R, BMP);
    }

    /** Lili's bust, {@code h} tall, standing on (cx, bottom), showing {@code face}. */
    static void lili(Canvas c, float cx, float bottom, float h, int face) {
        picture(c, LILI[face >= 0 && face < LILI.length ? face : FACE_CALM], cx, bottom, h);
    }

    /** Lili pointing up with joy, for the sticker screen. */
    static void liliCheer(Canvas c, float cx, float bottom, float h) {
        picture(c, liliCheer, cx, bottom, h);
    }

    private static Paint fill(int color) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        return P;
    }

    private static Paint stroke(int color, float width) {
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(width);
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        P.setColor(color);
        return P;
    }

    /**
     * A round little robot centred on (cx, cy), about {@code s} tall. {@code look} is the
     * direction its eyes glance (-1 for straight ahead).
     */
    static void robot(Canvas c, float cx, float cy, float s, int face, int look) {
        float u = s / 10f;
        // Wheels.
        c.drawCircle(cx - 2.6f * u, cy + 3.9f * u, 1.25f * u, fill(PLUM));
        c.drawCircle(cx + 2.6f * u, cy + 3.9f * u, 1.25f * u, fill(PLUM));
        // Antenna.
        c.drawLine(cx, cy - 3.6f * u, cx, cy - 4.9f * u, stroke(0xFF9AA6B8, 0.6f * u));
        c.drawCircle(cx, cy - 5.1f * u, 0.85f * u, fill(face == FACE_OOPS ? RED : YELLOW_D));
        // Body with a darker lip.
        R.set(cx - 4.2f * u, cy - 3.4f * u, cx + 4.2f * u, cy + 4.0f * u);
        c.drawRoundRect(R, 2.2f * u, 2.2f * u, fill(BLUE_D));
        R.set(cx - 4.2f * u, cy - 3.6f * u, cx + 4.2f * u, cy + 3.4f * u);
        c.drawRoundRect(R, 2.2f * u, 2.2f * u, fill(BLUE));
        // Face screen.
        R.set(cx - 3.0f * u, cy - 2.4f * u, cx + 3.0f * u, cy + 1.6f * u);
        c.drawRoundRect(R, 1.4f * u, 1.4f * u, fill(0xFF2E3A59));
        float ex = look == LEFT ? -0.5f * u : look == RIGHT ? 0.5f * u : 0;
        float ey = look == UP ? -0.4f * u : look == DOWN ? 0.3f * u : 0;
        float lx = cx - 1.3f * u + ex, rx = cx + 1.3f * u + ex, y = cy - 0.6f * u + ey;
        switch (face) {
            case FACE_HAPPY: // ^ ^ and a big smile
                stroke(MINT, 0.55f * u);
                c.drawLine(lx - 0.6f * u, y + 0.3f * u, lx, y - 0.3f * u, P);
                c.drawLine(lx, y - 0.3f * u, lx + 0.6f * u, y + 0.3f * u, P);
                c.drawLine(rx - 0.6f * u, y + 0.3f * u, rx, y - 0.3f * u, P);
                c.drawLine(rx, y - 0.3f * u, rx + 0.6f * u, y + 0.3f * u, P);
                R.set(cx - 1.2f * u, y + 0.2f * u, cx + 1.2f * u, y + 1.8f * u);
                c.drawArc(R, 20, 140, false, P);
                break;
            case FACE_OOPS: // x x and a wobbly mouth
                stroke(RED, 0.5f * u);
                for (float ox : new float[]{lx, rx}) {
                    c.drawLine(ox - 0.45f * u, y - 0.45f * u, ox + 0.45f * u, y + 0.45f * u, P);
                    c.drawLine(ox - 0.45f * u, y + 0.45f * u, ox + 0.45f * u, y - 0.45f * u, P);
                }
                c.drawCircle(cx, y + 1.3f * u, 0.4f * u, fill(RED));
                break;
            case FACE_THINK: // one eye squinting, "hmm"
                c.drawCircle(lx, y, 0.6f * u, fill(BLUE));
                c.drawLine(rx - 0.5f * u, y, rx + 0.5f * u, y, stroke(BLUE, 0.5f * u));
                c.drawLine(cx - 0.7f * u, y + 1.3f * u, cx + 0.7f * u, y + 1.1f * u, P);
                break;
            default: // calm round eyes with a glint
                c.drawCircle(lx, y, 0.7f * u, fill(BLUE));
                c.drawCircle(rx, y, 0.7f * u, fill(BLUE));
                c.drawCircle(lx + 0.25f * u, y - 0.25f * u, 0.22f * u, fill(0xFFFFFFFF));
                c.drawCircle(rx + 0.25f * u, y - 0.25f * u, 0.22f * u, fill(0xFFFFFFFF));
                R.set(cx - 0.8f * u, y + 0.5f * u, cx + 0.8f * u, y + 1.4f * u);
                c.drawArc(R, 20, 140, false, stroke(BLUE, 0.4f * u));
        }
        // Rosy cheeks.
        c.drawCircle(cx - 3.3f * u, cy + 2.3f * u, 0.5f * u, fill(0x66FF8FA3));
        c.drawCircle(cx + 3.3f * u, cy + 2.3f * u, 0.5f * u, fill(0x66FF8FA3));
    }

    static void star(Canvas c, float cx, float cy, float r, int color, int edge) {
        PATH.reset();
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(-90 + i * 36);
            float rr = i % 2 == 0 ? r : r * 0.48f;
            float x = cx + (float) Math.cos(a) * rr, y = cy + (float) Math.sin(a) * rr;
            if (i == 0) PATH.moveTo(x, y);
            else PATH.lineTo(x, y);
        }
        PATH.close();
        c.drawPath(PATH, stroke(edge, r * 0.16f));
        c.drawPath(PATH, fill(color));
        c.drawCircle(cx - r * 0.18f, cy - r * 0.2f, r * 0.12f, fill(0x99FFFFFF));
    }

    static void rock(Canvas c, float cx, float cy, float s) {
        float u = s / 10f;
        R.set(cx - 4.4f * u, cy - 2.6f * u, cx + 4.4f * u, cy + 3.6f * u);
        c.drawOval(R, fill(0xFF8C8FA6));
        R.set(cx - 4.0f * u, cy - 3.2f * u, cx + 3.6f * u, cy + 2.6f * u);
        c.drawOval(R, fill(0xFFB4B8CC));
        R.set(cx - 2.6f * u, cy - 2.4f * u, cx - 0.2f * u, cy - 1.0f * u);
        c.drawOval(R, fill(0x80FFFFFF));
    }

    /** A chunky arrow pointing in direction {@code d}, filling a box of side {@code s}. */
    static void arrow(Canvas c, float cx, float cy, float s, int d, int color) {
        c.save();
        c.rotate(d * 90, cx, cy);
        float u = s / 10f;
        PATH.reset();
        PATH.moveTo(cx, cy - 4.2f * u);
        PATH.lineTo(cx + 3.8f * u, cy);
        PATH.lineTo(cx + 1.5f * u, cy);
        PATH.lineTo(cx + 1.5f * u, cy + 4.0f * u);
        PATH.lineTo(cx - 1.5f * u, cy + 4.0f * u);
        PATH.lineTo(cx - 1.5f * u, cy);
        PATH.lineTo(cx - 3.8f * u, cy);
        PATH.close();
        c.drawPath(PATH, stroke(color, 1.3f * u));
        c.drawPath(PATH, fill(color));
        c.restore();
    }

    static void play(Canvas c, float cx, float cy, float s, int color) {
        float u = s / 10f;
        PATH.reset();
        PATH.moveTo(cx - 2.4f * u, cy - 3.6f * u);
        PATH.lineTo(cx + 3.6f * u, cy);
        PATH.lineTo(cx - 2.4f * u, cy + 3.6f * u);
        PATH.close();
        c.drawPath(PATH, stroke(color, 1.6f * u));
        c.drawPath(PATH, fill(color));
    }

    /** A "rub it out" arrow pointing left with an x in it: the undo key. */
    static void undo(Canvas c, float cx, float cy, float s, int color) {
        float u = s / 10f;
        PATH.reset();
        PATH.moveTo(cx - 4.4f * u, cy);
        PATH.lineTo(cx - 1.8f * u, cy - 3.2f * u);
        PATH.lineTo(cx + 4.2f * u, cy - 3.2f * u);
        PATH.lineTo(cx + 4.2f * u, cy + 3.2f * u);
        PATH.lineTo(cx - 1.8f * u, cy + 3.2f * u);
        PATH.close();
        c.drawPath(PATH, stroke(color, 1.0f * u));
        c.drawPath(PATH, fill(color));
        stroke(0xFFFFFFFF, 0.9f * u);
        c.drawLine(cx - 0.3f * u, cy - 1.4f * u, cx + 2.5f * u, cy + 1.4f * u, P);
        c.drawLine(cx - 0.3f * u, cy + 1.4f * u, cx + 2.5f * u, cy - 1.4f * u, P);
    }

    static void house(Canvas c, float cx, float cy, float s, int color) {
        float u = s / 10f;
        PATH.reset();
        PATH.moveTo(cx, cy - 4.2f * u);
        PATH.lineTo(cx + 4.4f * u, cy - 0.4f * u);
        PATH.lineTo(cx + 3.2f * u, cy - 0.4f * u);
        PATH.lineTo(cx + 3.2f * u, cy + 4.0f * u);
        PATH.lineTo(cx - 3.2f * u, cy + 4.0f * u);
        PATH.lineTo(cx - 3.2f * u, cy - 0.4f * u);
        PATH.lineTo(cx - 4.4f * u, cy - 0.4f * u);
        PATH.close();
        c.drawPath(PATH, stroke(color, 1.0f * u));
        c.drawPath(PATH, fill(color));
        R.set(cx - 1.1f * u, cy + 1.0f * u, cx + 1.1f * u, cy + 4.0f * u);
        c.drawRoundRect(R, 0.8f * u, 0.8f * u, fill(0xFFFFFFFF));
    }

    /** Stickers, earned every three wins: a rocket, a planet, a flower and a heart. */
    static void sticker(Canvas c, float cx, float cy, float s, int kind) {
        float u = s / 10f;
        c.drawCircle(cx, cy, 5f * u, fill(0xFFFFFFFF));
        c.drawCircle(cx, cy, 5f * u, stroke(0x33000000, 0.25f * u));
        switch (kind % 4) {
            case 0: // rocket
                c.save();
                c.rotate(35, cx, cy);
                R.set(cx - 1.4f * u, cy - 3.6f * u, cx + 1.4f * u, cy + 2.4f * u);
                c.drawRoundRect(R, 1.4f * u, 1.4f * u, fill(BLUE));
                c.drawCircle(cx, cy - 1.0f * u, 0.7f * u, fill(0xFFFFFFFF));
                PATH.reset();
                PATH.moveTo(cx - 1.4f * u, cy + 0.6f * u);
                PATH.lineTo(cx - 2.6f * u, cy + 2.8f * u);
                PATH.lineTo(cx - 1.4f * u, cy + 2.4f * u);
                PATH.moveTo(cx + 1.4f * u, cy + 0.6f * u);
                PATH.lineTo(cx + 2.6f * u, cy + 2.8f * u);
                PATH.lineTo(cx + 1.4f * u, cy + 2.4f * u);
                c.drawPath(PATH, fill(PINK_D));
                PATH.reset();
                PATH.moveTo(cx - 0.9f * u, cy + 2.4f * u);
                PATH.lineTo(cx, cy + 4.3f * u);
                PATH.lineTo(cx + 0.9f * u, cy + 2.4f * u);
                c.drawPath(PATH, fill(YELLOW_D));
                c.restore();
                break;
            case 1: // ringed planet
                c.drawCircle(cx, cy, 2.6f * u, fill(LILAC));
                R.set(cx - 4.2f * u, cy - 1.0f * u, cx + 4.2f * u, cy + 1.0f * u);
                c.save();
                c.rotate(-18, cx, cy);
                c.drawOval(R, stroke(PEACH_D, 0.6f * u));
                c.restore();
                c.drawCircle(cx - 0.9f * u, cy - 0.9f * u, 0.5f * u, fill(0x66FFFFFF));
                break;
            case 2: // flower
                for (int i = 0; i < 5; i++) {
                    double a = Math.toRadians(i * 72 - 90);
                    c.drawCircle(cx + (float) Math.cos(a) * 2.1f * u,
                            cy + (float) Math.sin(a) * 2.1f * u, 1.5f * u, fill(PINK));
                }
                c.drawCircle(cx, cy, 1.4f * u, fill(YELLOW_D));
                break;
            default: // heart
                PATH.reset();
                PATH.moveTo(cx, cy + 3.0f * u);
                PATH.cubicTo(cx - 4.6f * u, cy - 0.2f * u, cx - 2.4f * u, cy - 4.2f * u, cx, cy - 1.6f * u);
                PATH.cubicTo(cx + 2.4f * u, cy - 4.2f * u, cx + 4.6f * u, cy - 0.2f * u, cx, cy + 3.0f * u);
                c.drawPath(PATH, fill(PINK_D));
        }
    }

    // ---- pattern tokens -----------------------------------------------------------------
    // See Tokens for how a token packs a shape and a colour.

    static final int[] FILL = {PINK, YELLOW, BLUE, MINT, LILAC, PEACH};
    static final int[] EDGE = {PINK_D, YELLOW_D, BLUE_D, MINT_D, LILAC_D, PEACH_D};

    /** Draws token {@code t} filling a box of side {@code s}. {@code alpha} 0-255. */
    static void token(Canvas c, float cx, float cy, float s, int t, int alpha) {
        int fill = (FILL[Tokens.colourOf(t)] & 0x00FFFFFF) | (alpha << 24);
        int edge = (EDGE[Tokens.colourOf(t)] & 0x00FFFFFF) | (alpha << 24);
        float u = s / 10f;
        PATH.reset();
        switch (Tokens.shapeOf(t)) {
            case Tokens.CIRCLE:
                PATH.addCircle(cx, cy, 4.0f * u, Path.Direction.CW);
                break;
            case Tokens.TRIANGLE:
                PATH.moveTo(cx, cy - 4.2f * u);
                PATH.lineTo(cx + 4.4f * u, cy + 3.4f * u);
                PATH.lineTo(cx - 4.4f * u, cy + 3.4f * u);
                PATH.close();
                break;
            case Tokens.SQUARE:
                R.set(cx - 3.6f * u, cy - 3.6f * u, cx + 3.6f * u, cy + 3.6f * u);
                PATH.addRoundRect(R, 1.0f * u, 1.0f * u, Path.Direction.CW);
                break;
            case Tokens.STAR:
                for (int i = 0; i < 10; i++) {
                    double a = Math.toRadians(-90 + i * 36);
                    float rr = (i % 2 == 0 ? 4.6f : 2.1f) * u;
                    float x = cx + (float) Math.cos(a) * rr, y = cy + 0.3f * u + (float) Math.sin(a) * rr;
                    if (i == 0) PATH.moveTo(x, y);
                    else PATH.lineTo(x, y);
                }
                PATH.close();
                break;
            case Tokens.HEART:
                PATH.moveTo(cx, cy + 3.9f * u);
                PATH.cubicTo(cx - 6.0f * u, cy - 0.2f * u, cx - 3.0f * u, cy - 5.6f * u, cx, cy - 2.0f * u);
                PATH.cubicTo(cx + 3.0f * u, cy - 5.6f * u, cx + 6.0f * u, cy - 0.2f * u, cx, cy + 3.9f * u);
                PATH.close();
                break;
            default: // diamond
                PATH.moveTo(cx, cy - 4.6f * u);
                PATH.lineTo(cx + 3.6f * u, cy);
                PATH.lineTo(cx, cy + 4.6f * u);
                PATH.lineTo(cx - 3.6f * u, cy);
                PATH.close();
        }
        c.drawPath(PATH, stroke(edge, 1.1f * u));
        c.drawPath(PATH, fill(fill));
        // a little shine
        c.drawCircle(cx - 1.3f * u, cy - 1.2f * u, 0.7f * u, fill((alpha * 3 / 5) << 24 | 0xFFFFFF));
    }

    /** A chunky question mark for the empty slot. */
    static void question(Canvas c, float cx, float cy, float s, int color) {
        float u = s / 10f;
        R.set(cx - 2.6f * u, cy - 4.4f * u, cx + 2.6f * u, cy + 0.6f * u);
        stroke(color, 1.5f * u);
        c.drawArc(R, 180, 250, false, P);
        c.drawLine(cx + 0.2f * u, cy + 0.6f * u, cx, cy + 1.6f * u, P);
        c.drawCircle(cx, cy + 3.8f * u, 0.9f * u, fill(color));
    }

    /** A paint palette: the "make your own pattern" button. */
    static void palette(Canvas c, float cx, float cy, float s, int color) {
        float u = s / 10f;
        R.set(cx - 4.4f * u, cy - 3.6f * u, cx + 4.4f * u, cy + 3.6f * u);
        c.drawOval(R, fill(color));
        int[] dots = {PINK_D, YELLOW_D, BLUE_D, MINT_D};
        float[][] at = {{-2.4f, -1.2f}, {-0.4f, -2.2f}, {1.8f, -1.6f}, {2.4f, 0.6f}};
        for (int i = 0; i < 4; i++) c.drawCircle(cx + at[i][0] * u, cy + at[i][1] * u, 0.9f * u, fill(dots[i]));
        c.drawCircle(cx - 1.6f * u, cy + 1.6f * u, 1.0f * u, fill(0x00000000 | 0x55000000));
    }

    private Art() {
    }
}
