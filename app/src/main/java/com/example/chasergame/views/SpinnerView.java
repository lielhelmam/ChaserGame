package com.example.chasergame.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SpinnerView extends View {
    private Paint outerPaint, progressPaint, innerPaint, textPaint, glowPaint;
    private float totalRotation = 0;
    private float lastTouchAngle = 0;
    private boolean isActive = false;
    private long endTime = 0;
    private SpinnerListener listener;
    private int spinsRequired = 3;
    private float visualRotation = 0;
    private float pulseScale = 1.0f;

    public SpinnerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerPaint.setStyle(Paint.Style.STROKE);
        outerPaint.setStrokeWidth(12f);
        outerPaint.setColor(Color.WHITE);
        outerPaint.setAlpha(80);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(30f);
        glowPaint.setColor(Color.parseColor("#FFD700"));
        glowPaint.setAlpha(40);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(20f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.STROKE);
        innerPaint.setStrokeWidth(6f);
        innerPaint.setColor(Color.WHITE);
        innerPaint.setAlpha(150);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(70f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(10, 0, 0, Color.YELLOW);
    }

    public void start(long duration, SpinnerListener listener) {
        this.listener = listener;
        this.endTime = System.currentTimeMillis() + duration;
        this.totalRotation = 0;
        this.visualRotation = 0;
        this.isActive = true;
        this.setVisibility(View.VISIBLE);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isActive) return;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = (Math.min(cx, cy) * 0.7f) * pulseScale;

        // Draw Glow
        canvas.drawCircle(cx, cy, radius + 10, glowPaint);

        // Draw Static Outer Ring
        canvas.drawCircle(cx, cy, radius, outerPaint);

        // Calculate progress and dynamic color
        float progress = Math.min(1.0f, totalRotation / (360f * spinsRequired));
        int color = interpolateColor(Color.WHITE, Color.parseColor("#FFD700"), progress);
        progressPaint.setColor(color);

        // Draw Progress Arc
        RectF rect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(rect, -90, progress * 360f, false, progressPaint);

        // Draw Rotating Inner Core
        canvas.save();
        canvas.rotate(visualRotation, cx, cy);
        for (int i = 0; i < 4; i++) {
            canvas.drawLine(cx, cy - radius + 20, cx, cy - radius + 60, innerPaint);
            canvas.rotate(90, cx, cy);
        }
        canvas.restore();

        // Draw Text with pulse
        textPaint.setTextSize(70f * pulseScale);
        canvas.drawText(progress >= 1.0 ? "CLEAR!!" : "SPIN!!", cx, cy + 25, textPaint);

        if (System.currentTimeMillis() > endTime) {
            isActive = false;
            this.setVisibility(View.GONE);
            if (listener != null) listener.onFinished(progress >= 1.0);
        } else {
            // Decay pulse slightly for smoothness
            pulseScale = 1.0f + (progress * 0.1f);
            invalidate();
        }
    }

    private int interpolateColor(int color1, int color2, float factor) {
        int r = (int) (Color.red(color1) + factor * (Color.red(color2) - Color.red(color1)));
        int g = (int) (Color.green(color1) + factor * (Color.green(color2) - Color.green(color1)));
        int b = (int) (Color.blue(color1) + factor * (Color.blue(color2) - Color.blue(color1)));
        return Color.rgb(r, g, b);
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
                visualRotation += delta; // Core follows finger
                lastTouchAngle = angle;

                if (listener != null) listener.onSpin(totalRotation / (360f * spinsRequired));
                break;
        }
        return true;
    }

    public interface SpinnerListener {
        void onSpin(float progress);

        void onFinished(boolean success);
    }
}
