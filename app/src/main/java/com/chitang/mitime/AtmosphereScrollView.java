package com.chitang.mitime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class AtmosphereScrollView extends ScrollView {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private boolean lightTheme;

    public AtmosphereScrollView(Context context) {
        super(context);
        init();
    }

    public AtmosphereScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AtmosphereScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
    }

    public void setLightTheme(boolean lightTheme) {
        this.lightTheme = lightTheme;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawAtmosphere(canvas, getWidth(), getHeight());
        super.onDraw(canvas);
    }

    private void drawAtmosphere(Canvas canvas, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        if (lightTheme) {
            drawLight(canvas, width, height);
        } else {
            drawDark(canvas, width, height);
        }
    }

    private void drawDark(Canvas canvas, int width, int height) {
        paint.setShader(new LinearGradient(
                0, 0, 0, height,
                new int[]{
                        Color.rgb(3, 11, 28),
                        Color.rgb(7, 22, 54),
                        Color.rgb(10, 18, 49),
                        Color.rgb(2, 7, 20)
                },
                new float[]{0f, 0.38f, 0.68f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, width, height, paint);

        paint.setShader(new RadialGradient(
                width * -0.05f, height * 0.54f, width * 0.62f,
                new int[]{
                        Color.argb(210, 255, 132, 176),
                        Color.argb(132, 116, 78, 214),
                        Color.argb(0, 4, 10, 28)
                },
                new float[]{0f, 0.42f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(width * -0.05f, height * 0.54f, width * 0.62f, paint);

        paint.setShader(new RadialGradient(
                width * 0.78f, height * 0.38f, width * 0.55f,
                new int[]{
                        Color.argb(120, 37, 111, 255),
                        Color.argb(36, 64, 76, 180),
                        Color.argb(0, 4, 10, 28)
                },
                null,
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(width * 0.78f, height * 0.38f, width * 0.55f, paint);
        paint.setShader(null);

        drawStars(canvas, width, height);
        drawHorizon(canvas, width, height);
    }

    private void drawLight(Canvas canvas, int width, int height) {
        paint.setShader(new LinearGradient(
                0, 0, 0, height,
                new int[]{
                        Color.rgb(249, 252, 255),
                        Color.rgb(230, 241, 255),
                        Color.rgb(255, 250, 244),
                        Color.rgb(247, 251, 255)
                },
                new float[]{0f, 0.42f, 0.65f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);
    }

    private void drawStars(Canvas canvas, int width, int height) {
        paint.setShader(null);
        paint.setColor(Color.argb(150, 201, 222, 255));
        float[][] stars = {
                {0.09f, 0.22f, 1.2f}, {0.29f, 0.20f, 1.6f}, {0.33f, 0.27f, 1.8f},
                {0.80f, 0.23f, 1.0f}, {0.92f, 0.24f, 1.5f}, {0.93f, 0.32f, 0.9f}
        };
        for (float[] star : stars) {
            canvas.drawCircle(width * star[0], height * star[1], star[2], paint);
        }
    }

    private void drawHorizon(Canvas canvas, int width, int height) {
        float base = height * 0.58f;

        paint.setColor(Color.argb(160, 3, 12, 36));
        path.reset();
        path.moveTo(0, base);
        path.cubicTo(width * 0.14f, base - 28, width * 0.22f, base + 10, width * 0.34f, base - 10);
        path.cubicTo(width * 0.50f, base + 18, width * 0.62f, base - 4, width * 0.78f, base + 4);
        path.cubicTo(width * 0.88f, base - 12, width * 0.94f, base - 6, width, base - 20);
        path.lineTo(width, height);
        path.lineTo(0, height);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(Color.argb(78, 98, 99, 180));
        for (int i = 0; i < 7; i++) {
            float y = base + 18 + i * 7;
            canvas.drawLine(0, y, width, y + 2, paint);
        }
    }
}
