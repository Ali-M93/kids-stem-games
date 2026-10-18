package io.github.foadsf.kangaroohops;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Everything in the game is drawn here, as code: the kangaroo, stones, buttons, stickers. */
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

    /** A chunky question mark for the empty slot. */
    static void question(Canvas c, float cx, float cy, float s, int color) {
        float u = s / 10f;
        R.set(cx - 2.6f * u, cy - 4.4f * u, cx + 2.6f * u, cy + 0.6f * u);
        stroke(color, 1.5f * u);
        c.drawArc(R, 180, 250, false, P);
        c.drawLine(cx + 0.2f * u, cy + 0.6f * u, cx, cy + 1.6f * u, P);
        c.drawCircle(cx, cy + 3.8f * u, 0.9f * u, fill(color));
    }

    // ---- the kangaroo and her world ------------------------------------------------------

    static final int FUR = 0xFFE9A266, FUR_D = 0xFFC97E43, BELLY = 0xFFF8DDB8;

    /**
     * A friendly kangaroo standing with her feet at (cx, feetY), about {@code s} tall, facing
     * right ({@code dir} = 1) or left (-1). {@code squash} 0..1 crouches her before a hop.
     * {@code happy} closes her eyes into smiles.
     */
    static void kangaroo(Canvas c, float cx, float feetY, float s, int dir, float squash, boolean happy) {
        float u = s / 10f;
        c.save();
        c.scale(dir, 1, cx, feetY);
        c.scale(1 + 0.12f * squash, 1 - 0.18f * squash, cx, feetY);
        // tail
        PATH.reset();
        PATH.moveTo(cx - 1.4f * u, feetY - 2.4f * u);
        PATH.quadTo(cx - 4.6f * u, feetY - 1.6f * u, cx - 5.2f * u, feetY - 0.2f * u);
        c.drawPath(PATH, stroke(FUR_D, 1.3f * u));
        // big feet
        R.set(cx - 1.6f * u, feetY - 0.9f * u, cx + 2.6f * u, feetY);
        c.drawRoundRect(R, 0.5f * u, 0.5f * u, fill(FUR_D));
        // body and belly
        R.set(cx - 2.6f * u, feetY - 6.4f * u, cx + 2.4f * u, feetY - 0.4f * u);
        c.drawOval(R, fill(FUR));
        R.set(cx - 1.2f * u, feetY - 5.0f * u, cx + 1.9f * u, feetY - 1.0f * u);
        c.drawOval(R, fill(BELLY));
        // pouch line
        R.set(cx - 0.8f * u, feetY - 3.4f * u, cx + 1.6f * u, feetY - 1.6f * u);
        c.drawArc(R, 10, 160, false, stroke(FUR_D, 0.25f * u));
        // little arm
        c.drawLine(cx + 1.3f * u, feetY - 4.4f * u, cx + 2.4f * u, feetY - 3.6f * u, stroke(FUR_D, 0.55f * u));
        // ears
        for (float ex : new float[]{-0.4f, 0.7f}) {
            R.set(cx + (ex - 0.45f) * u, feetY - 10.0f * u, cx + (ex + 0.45f) * u, feetY - 7.6f * u);
            c.drawOval(R, fill(FUR));
            R.set(cx + (ex - 0.22f) * u, feetY - 9.6f * u, cx + (ex + 0.22f) * u, feetY - 8.0f * u);
            c.drawOval(R, fill(0xFFF4B6A6));
        }
        // head with a snout
        c.drawCircle(cx + 0.3f * u, feetY - 7.0f * u, 1.6f * u, fill(FUR));
        R.set(cx + 0.6f * u, feetY - 7.2f * u, cx + 2.8f * u, feetY - 5.8f * u);
        c.drawOval(R, fill(BELLY));
        c.drawCircle(cx + 2.6f * u, feetY - 6.7f * u, 0.32f * u, fill(PLUM));
        if (happy) {
            R.set(cx + 0.2f * u, feetY - 7.9f * u, cx + 1.2f * u, feetY - 7.1f * u);
            c.drawArc(R, 200, 140, false, stroke(PLUM, 0.28f * u));
        } else {
            c.drawCircle(cx + 0.75f * u, feetY - 7.4f * u, 0.36f * u, fill(PLUM));
            c.drawCircle(cx + 0.87f * u, feetY - 7.55f * u, 0.12f * u, fill(0xFFFFFFFF));
        }
        c.drawCircle(cx + 1.1f * u, feetY - 6.4f * u, 0.35f * u, fill(0x66FF8FA3));
        c.restore();
    }

    /** A little flag on a pole, planted at (x, groundY). */
    static void flag(Canvas c, float x, float groundY, float s, int color) {
        float u = s / 10f;
        c.drawLine(x, groundY, x, groundY - 9 * u, stroke(PLUM, 0.6f * u));
        PATH.reset();
        PATH.moveTo(x, groundY - 9 * u);
        PATH.lineTo(x + 5 * u, groundY - 7.6f * u);
        PATH.lineTo(x, groundY - 6.2f * u);
        PATH.close();
        c.drawPath(PATH, fill(color));
    }

    private Art() {
    }
}
