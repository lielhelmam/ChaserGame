package com.example.chasergame.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpinnerView extends View {
    private Paint outerPaint, progressPaint, innerPaint, textPaint, glowPaint, timePaint, particlePaint;
    private float totalRotation = 0;
    private float lastTouchAngle = 0;
    private boolean isActive = false;
    private long startTime = 0;
    private long endTime = 0;
    private SpinnerListener listener;
    private int spinsRequired = 3;
    private float visualRotation = 0;
    private float pulseScale = 1.0f;
    private RectF arcBounds = new RectF();
    private List<SpinnerParticle> particles = new ArrayList<>();
    private Random random = new Random();

    public SpinnerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerPaint.setStyle(Paint.Style.STROKE);
        outerPaint.setStrokeWidth(8f);
        outerPaint.setColor(Color.WHITE);
        outerPaint.setAlpha(40);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(40f);
        glowPaint.setColor(Color.parseColor("#00FFFF")); // Cyan Glow
        glowPaint.setAlpha(30);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(25f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.STROKE);
        innerPaint.setStrokeWidth(4f);
        innerPaint.setColor(Color.CYAN);
        innerPaint.setAlpha(100);

        timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setStyle(Paint.Style.STROKE);
        timePaint.setStrokeWidth(10f);
        timePaint.setColor(Color.RED);
        timePaint.setAlpha(180);

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(80f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(15, 0, 0, Color.CYAN);
    }

    public void start(long duration, SpinnerListener listener) {
        this.listener = listener;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + duration;
        this.totalRotation = 0;
        this.visualRotation = 0;
        this.isActive = true;
        this.particles.clear();
        this.setVisibility(View.VISIBLE);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isActive) return;

        long now = System.currentTimeMillis();
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = (Math.min(cx, cy) * 0.75f) * pulseScale;
        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // 1. Draw Tech Background (Hexagon pattern simplified)
        drawHexGrid(canvas, cx, cy, radius);

        // 2. Draw Time Limit Ring (Outer)
        float timeProgress = 1.0f - (float)(now - startTime) / (endTime - startTime);
        if (timeProgress > 0) {
            canvas.drawArc(cx - radius - 30, cy - radius - 30, cx + radius + 30, cy + radius + 30, 
                    -90, timeProgress * 360f, false, timePaint);
        }

        // 3. Draw Glow & Main Circle
        canvas.drawCircle(cx, cy, radius, glowPaint);
        canvas.drawCircle(cx, cy, radius, outerPaint);

        // 4. Draw Neon Progress Arc
        float progress = Math.min(1.0f, totalRotation / (360f * spinsRequired));
        int colorStart = Color.parseColor("#00FFFF"); // Cyan
        int colorEnd = Color.parseColor("#FF00FF");   // Magenta
        
        // Setup Gradient on the fly for rotation
        SweepGradient gradient = new SweepGradient(cx, cy, 
                new int[]{colorStart, colorEnd, colorStart}, 
                new float[]{0, 0.5f, 1f});
        progressPaint.setShader(gradient);
        
        canvas.save();
        canvas.rotate(visualRotation, cx, cy);
        canvas.drawArc(arcBounds, -90, progress * 360f, false, progressPaint);
        canvas.restore();
        progressPaint.setShader(null);

        // 5. Particles
        updateAndDrawParticles(canvas);

        // 6. Draw "Core" Spokes
        canvas.save();
        canvas.rotate(visualRotation * 1.5f, cx, cy);
        for (int i = 0; i < 8; i++) {
            canvas.drawLine(cx, cy - radius + 40, cx, cy - radius + 80, innerPaint);
            canvas.rotate(45, cx, cy);
        }
        canvas.restore();

        // 7. Text Overlay
        textPaint.setTextSize(70f * pulseScale);
        textPaint.setAlpha(progress >= 1.0 ? 255 : (int)(150 + 105 * Math.sin(now / 100.0)));
        canvas.drawText(progress >= 1.0 ? "MAX POWER!!" : "OVERDRIVE!!", cx, cy + 25, textPaint);

        if (now > endTime) {
            isActive = false;
            this.setVisibility(View.GONE);
            if (listener != null) listener.onFinished(progress >= 1.0);
        } else {
            pulseScale = 1.0f + (progress * 0.15f);
            invalidate();
        }
    }

    private void drawHexGrid(Canvas canvas, float cx, float cy, float radius) {
        Paint hexPaint = new Paint(innerPaint);
        hexPaint.setAlpha(20);
        float size = 40f;
        for (float x = cx - radius; x < cx + radius; x += size * 1.5f) {
            for (float y = cy - radius; y < cy + radius; y += size * 0.86f) {
                // Check if inside circle
                if (Math.sqrt(Math.pow(x-cx, 2) + Math.pow(y-cy, 2)) < radius) {
                    drawHex(canvas, x, y, size * 0.5f, hexPaint);
                }
            }
        }
    }

    private void drawHex(Canvas canvas, float x, float y, float size, Paint paint) {
        Path path = new Path();
        for (int i = 0; i < 6; i++) {
            float angle = (float) (Math.PI / 3 * i);
            float px = (float) (x + size * Math.cos(angle));
            float py = (float) (y + size * Math.sin(angle));
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    private void updateAndDrawParticles(Canvas canvas) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            SpinnerParticle p = particles.get(i);
            p.update();
            if (p.alpha <= 0) {
                particles.remove(i);
            } else {
                particlePaint.setColor(p.color);
                particlePaint.setAlpha(p.alpha);
                canvas.drawCircle(p.x, p.y, p.size, particlePaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isActive) return false;

        float x = event.getX() - getWidth() / 2f;
        float y = event.getY() - getHeight() / 2f;
        float angle = (float) Math.toDegrees(Math.atan2(y, x));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchAngle = angle;
                break;
            case MotionEvent.ACTION_MOVE:
                float delta = angle - lastTouchAngle;
                if (delta > 180) delta -= 360;
                if (delta < -180) delta += 360;

                float absDelta = Math.abs(delta);
                totalRotation += absDelta;
                visualRotation += delta;
                lastTouchAngle = angle;

                // Spawn sparks when spinning
                if (absDelta > 2) {
                    float cx = getWidth() / 2f;
                    float cy = getHeight() / 2f;
                    float radius = (Math.min(cx, cy) * 0.75f);
                    float rad = (float)Math.toRadians(angle);
                    float px = cx + (float)Math.cos(rad) * radius;
                    float py = cy + (float)Math.sin(rad) * radius;
                    particles.add(new SpinnerParticle(px, py));
                }

                if (listener != null) listener.onSpin(totalRotation / (360f * spinsRequired));
                break;
        }
        return true;
    }

    private class SpinnerParticle {
        float x, y, vx, vy, size;
        int alpha = 255;
        int color;

        SpinnerParticle(float x, float y) {
            this.x = x;
            this.y = y;
            this.vx = (random.nextFloat() - 0.5f) * 15f;
            this.vy = (random.nextFloat() - 0.5f) * 15f;
            this.size = 5f + random.nextFloat() * 10f;
            this.color = random.nextBoolean() ? Color.CYAN : Color.MAGENTA;
        }

        void update() {
            x += vx;
            y += vy;
            alpha -= 15;
            size *= 0.95f;
        }
    }

    public interface SpinnerListener {
        void onSpin(float progress);
        void onFinished(boolean success);
    }
}
