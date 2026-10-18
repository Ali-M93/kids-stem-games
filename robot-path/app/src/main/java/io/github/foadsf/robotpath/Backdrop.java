package io.github.foadsf.robotpath;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/** The same soft pink-to-sky gradient as Kid Launcher, with a few clouds. Static: no redraws. */
class Backdrop extends View {
    private final Paint sky = new Paint();
    private final Paint cloud = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;

    Backdrop(Context ctx) {
        super(ctx);
        dp = getResources().getDisplayMetrics().density;
        cloud.setColor(0xD9FFFFFF);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        sky.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFFFFDCEB, 0xFFEBDFFF, 0xFFD2EEFF}, new float[]{0, 0.5f, 1},
                Shader.TileMode.CLAMP));
    }

    private void cloud(Canvas c, float x, float y, float s) {
        float u = 10 * dp * s;
        c.drawCircle(x - 1.6f * u, y + 0.3f * u, 1.2f * u, cloud);
        c.drawCircle(x - 0.2f * u, y - 0.6f * u, 1.7f * u, cloud);
        c.drawCircle(x + 1.5f * u, y + 0.1f * u, 1.3f * u, cloud);
        c.drawRect(x - 2.6f * u, y + 0.2f * u, x + 2.6f * u, y + 1.4f * u, cloud);
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        c.drawRect(0, 0, w, h, sky);
        cloud(c, w * 0.15f, h * 0.30f, 0.9f);
        cloud(c, w * 0.85f, h * 0.55f, 1.0f);
        cloud(c, w * 0.30f, h * 0.88f, 0.8f);
    }
}
