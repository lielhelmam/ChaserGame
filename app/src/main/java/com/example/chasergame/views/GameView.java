package com.example.chasergame.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.chasergame.R;
import com.example.chasergame.models.Note;
import com.example.chasergame.models.Skin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameView extends ConstraintLayout {

    private static final long VISIBLE_DURATION = 1500L;
    private static final int PERFECT_WINDOW = 250;
    private static final int GOOD_WINDOW = 500;
    private static final int MISS_WINDOW = 600;
    private static final int SLIDER_GRACE_PERIOD = 250;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final List<ActiveNote> activeNotes = new ArrayList<>();
    private final Map<Integer, Boolean> lanePressed = new HashMap<>();
    private final Map<Integer, ActiveNote> activeSliders = new HashMap<>();
    private FrameLayout laneLeft, laneRight;
    private View targetLeft, targetRight;
    private Skin equippedSkin;
    private List<Note> allNotes = new ArrayList<>();
    private boolean isGameRunning = false;
    private boolean isSpinnerActive = false;
    private long currentSongTime = 0;
    private long lastTickTime = 0;
    private GameEventListener listener;
    private List<String> activeMods = new ArrayList<>();
    private long lastGlitchTime = 0;
    private float offsetX = 0, offsetY = 0;
    private final Paint staticPaint = new Paint();
    
    // Visualizer variables
    private final float[] vizHeights = new float[12]; // 12 bars across the screen
    private final Paint vizPaint = new Paint();
    private long lastVizTick = 0;

    public void setActiveMods(List<String> mods) {
        this.activeMods = mods != null ? mods : new ArrayList<>();
    }
    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (!isGameRunning) return;
            updateGameFrame();
            if (listener != null) listener.onGameLoopTick();
            handler.postDelayed(this, 16);
        }
    };

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_game, this, true);
        laneLeft = findViewById(R.id.lane_left);
        laneRight = findViewById(R.id.lane_right);
        targetLeft = findViewById(R.id.target_left);
        targetRight = findViewById(R.id.target_right);
        lanePressed.put(0, false);
        lanePressed.put(1, false);
    }

    public void setGameEventListener(GameEventListener listener) {
        this.listener = listener;
    }

    public void startGame(List<Note> notes, Skin skin) {
        this.equippedSkin = skin;
        this.allNotes = new ArrayList<>(notes);
        this.activeNotes.clear();
        this.activeSliders.clear();
        this.isGameRunning = true;
        applySkin();
        setupInputListeners();
        handler.post(gameLoop);
    }

    public void syncTime(long timeMs) {
        this.currentSongTime = timeMs;
    }

    public void stop() {
        isGameRunning = false;
        handler.removeCallbacks(gameLoop);
        List<ActiveNote> toClean = new ArrayList<>(activeNotes);
        activeNotes.clear();
        for (ActiveNote an : toClean) {
            if (an.view != null && an.view.getParent() != null) {
                ((ViewGroup) an.view.getParent()).removeView(an.view);
            }
        }
        activeSliders.clear();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // --- LIVE VISUALIZER LOGIC (Upgraded) ---
        if (isGameRunning) {
            vizPaint.setStrokeWidth(25); // Thicker bars
            vizPaint.setStrokeCap(Paint.Cap.ROUND);
            int barWidth = getWidth() / (vizHeights.length + 1);
            
            for (int i = 0; i < vizHeights.length; i++) {
                // Decay height
                vizHeights[i] *= 0.94f; // Slower decay for better visibility
                
                // Noise bounce
                if (System.currentTimeMillis() - lastVizTick > 80) {
                    if (random.nextInt(4) == 0) vizHeights[i] += random.nextFloat() * 80f;
                }
                
                int color = (equippedSkin != null) ? equippedSkin.targetColor : Color.CYAN;
                vizPaint.setColor(color);
                vizPaint.setAlpha(160); // Much more visible
                
                float x = barWidth * (i + 1);
                float startY = getHeight();
                // Taller bars (up to 500px high)
                float endY = getHeight() - 20 - Math.min(500f, vizHeights[i]); 
                canvas.drawLine(x, startY, x, endY, vizPaint);
            }
            if (System.currentTimeMillis() - lastVizTick > 80) lastVizTick = System.currentTimeMillis();
            invalidate(); 
        }

        // Draw Static Noise Mod (HARDER - Full Screen Flashes)
        if (activeMods.contains("STATIC")) {
            staticPaint.setColor(Color.WHITE);
            // Dynamic noise
            for (int i = 0; i < 80; i++) {
                staticPaint.setAlpha(random.nextInt(100));
                float x = random.nextFloat() * getWidth();
                float y = random.nextFloat() * getHeight();
                float size = random.nextFloat() * 300f;
                canvas.drawRect(x, y, x + size, y + 4, staticPaint);
            }
            // Random full screen static "blindness"
            if (random.nextInt(20) == 0) {
                staticPaint.setAlpha(150);
                canvas.drawRect(0, 0, getWidth(), getHeight(), staticPaint);
            }
            invalidate(); 
        }
    }

    private void applySkin() {
        if (equippedSkin == null) return;
        float density = getResources().getDisplayMetrics().density;

        // Background for the whole GameView
        this.setBackgroundColor(equippedSkin.backgroundColor);

        // Targets: Outline/Stroke ONLY (Hollow)
        int strokeWidth = (int) (4 * density);
        if (targetLeft != null) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setColor(Color.TRANSPARENT); // Empty inside
            gd.setStroke(strokeWidth, equippedSkin.targetColor);
            gd.setCornerRadius(15 * density);
            targetLeft.setBackground(gd);
        }
        if (targetRight != null) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setColor(Color.TRANSPARENT); // Empty inside
            gd.setStroke(strokeWidth, equippedSkin.targetColor);
            gd.setCornerRadius(15 * density);
            targetRight.setBackground(gd);
        }

        // Lanes: Transparent to avoid colored squares at the bottom
        if (laneLeft != null) laneLeft.setBackgroundColor(Color.TRANSPARENT);
        if (laneRight != null) laneRight.setBackgroundColor(Color.TRANSPARENT);
    }

    private void setupInputListeners() {
        View.OnTouchListener tl = (v, event) -> {
            int lane = (v == targetLeft) ? 0 : 1;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lanePressed.put(lane, true);
                handleTap(lane);
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                lanePressed.put(lane, false);
            }
            return true;
        };
        if (targetLeft != null) targetLeft.setOnTouchListener(tl);
        if (targetRight != null) targetRight.setOnTouchListener(tl);
    }

    private void updateGameFrame() {
        if (getHeight() == 0 || !isGameRunning) return;

        // --- GLITCH MOD LOGIC (HARDER) ---
        if (activeMods.contains("GLITCH")) {
            if (System.currentTimeMillis() - lastGlitchTime > 50) { // Faster glitches
                offsetX = (random.nextFloat() - 0.5f) * 80; // Bigger shakes
                offsetY = (random.nextFloat() - 0.5f) * 80;
                lastGlitchTime = System.currentTimeMillis();
                setTranslationX(offsetX);
                setTranslationY(offsetY);
                // Randomly flash background color for distraction
                if (random.nextInt(10) == 0) setBackgroundColor(Color.argb(50, 255, 0, 0));
                else if (equippedSkin != null) setBackgroundColor(equippedSkin.backgroundColor);
            }
        } else {
            setTranslationX(0);
            setTranslationY(0);
        }

        // While spinner is active, we don't spawn new notes and we don't update existing ones
        if (isSpinnerActive) return;

        for (int i = 0; i < allNotes.size(); i++) {
            Note n = allNotes.get(i);
            if (currentSongTime >= n.getTimestamp() - VISIBLE_DURATION) {
                if (n.getType() == Note.Type.SPINNER) {
                    clearActiveNotes(); // Remove everything else for the spinner
                    isSpinnerActive = true;
                    if (listener != null) listener.onRequestSpinner(n.getDuration());
                } else {
                    spawnNote(n);
                }
                allNotes.remove(i);
                i--;
            } else break;
        }

        float density = getResources().getDisplayMetrics().density;
        float targetY = (targetLeft != null) ? (targetLeft.getTop() - laneLeft.getTop() + (targetLeft.getHeight() - (60 * density)) / 2f) : 0;
        float speed = targetY / VISIBLE_DURATION;

        for (int i = activeNotes.size() - 1; i >= 0; i--) {
            if (!isGameRunning) break;
            ActiveNote an = activeNotes.get(i);
            long timeDiff = an.data.getTimestamp() - currentSongTime;
            
            // --- GRAVITY WARP LOGIC (Acceleration) ---
            float progress = 1f - ((float)timeDiff / VISIBLE_DURATION);
            if (activeMods.contains("GRAVITY")) {
                // Exponential movement: notes speed up as they fall
                progress = (float) Math.pow(progress, 2.5); 
            }
            float noteHeadY = targetY * progress;

            if (an.data.isSlider()) {
                float sliderHeadY = noteHeadY;
                int sliderHeight = (int) (an.data.getDuration() * speed) + (int) (60 * density);
                an.view.setTranslationY(sliderHeadY - (sliderHeight - (60 * density)));
            } else {
                an.view.setTranslationY(noteHeadY);
            }

            if (!an.wasHit && timeDiff < -MISS_WINDOW) {
                an.wasMissed = true;
                handleMiss(an);
                if (isGameRunning && i < activeNotes.size()) activeNotes.remove(i);
                continue;
            }

            if (an.wasHit && an.data.isSlider()) {
                long sliderEndTime = an.data.getTimestamp() + an.data.getDuration();

                if (!Boolean.TRUE.equals(lanePressed.get(an.data.getLane()))) {
                    if (currentSongTime >= sliderEndTime - SLIDER_GRACE_PERIOD) {
                        handlePerfectSlider(an);
                        activeSliders.remove(an.data.getLane());
                        if (isGameRunning && i < activeNotes.size()) activeNotes.remove(i);
                    } else {
                        handleMiss(an, true);
                        activeSliders.remove(an.data.getLane());
                        if (isGameRunning && i < activeNotes.size()) activeNotes.remove(i);
                    }
                } else {
                    if (currentSongTime >= sliderEndTime - SLIDER_GRACE_PERIOD) {
                        handlePerfectSlider(an);
                        activeSliders.remove(an.data.getLane());
                        if (isGameRunning && i < activeNotes.size()) activeNotes.remove(i);
                    } else {
                        if (currentSongTime - lastTickTime > 250) { // Faster ticks for better feel
                            if (listener != null)
                                listener.onNoteHit(20); // Use 20 for slider ticks (score only, no acc weight)
                            lastTickTime = currentSongTime;
                        }
                        spawnBurstParticles(an.data.getLane() == 0 ? targetLeft : targetRight, equippedSkin.circleColor);
                    }
                }
            } else if (an.wasHit && !an.data.isSlider()) {
                if (isGameRunning && i < activeNotes.size()) activeNotes.remove(i);
            } else {
                // --- TRAIL EFFECTS AND BLIND MOD (HARDER) ---
                if (equippedSkin != null && !an.wasMissed) {
                    if (activeMods.contains("BLIND")) {
                        // Disappear when 600ms away from target (MUCH EARLIER)
                        float alpha = Math.max(0, Math.min(1, (timeDiff - 600) / 400f));
                        an.view.setAlpha(alpha);
                    }

                    if (!"none".equals(equippedSkin.effectType)) {
                        if (System.currentTimeMillis() % 3 == 0) {
                            listener.onRequestTrail(an.view, equippedSkin.effectType, equippedSkin.circleColor);
                        }
                    }
                }
            }
        }
    }

    private void clearActiveNotes() {
        for (ActiveNote an : activeNotes) {
            if (an.view != null && an.view.getParent() != null) {
                ((ViewGroup) an.view.getParent()).removeView(an.view);
            }
        }
        activeNotes.clear();
        activeSliders.clear();
    }

    public void setSpinnerActive(boolean active) {
        this.isSpinnerActive = active;
    }

    private void spawnNote(Note n) {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (60 * density);
        View v = new View(getContext());
        float targetY = (targetLeft != null) ? (targetLeft.getTop() - laneLeft.getTop() + (targetLeft.getHeight() - size) / 2f) : 0;
        float speed = targetY / VISIBLE_DURATION;

        if (n.isSlider()) {
            int mainColor = equippedSkin != null ? equippedSkin.circleColor : Color.CYAN;
            int sliderHeight = (int) (n.getDuration() * speed) + size;

            // 1. The Fading Body
            GradientDrawable body = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[]{mainColor, Color.TRANSPARENT}
            );
            body.setCornerRadius(size / 2f);
            body.setAlpha(180); // Semi-transparent body

            // 2. The Opaque Head (Circle at the bottom)
            GradientDrawable head = new GradientDrawable();
            head.setShape(GradientDrawable.OVAL);
            head.setColor(mainColor);
            head.setStroke((int) (3 * density), Color.WHITE);

            // Combine into LayerDrawable
            LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[]{body, head});
            // Position the head at the bottom of the view
            layers.setLayerInset(1, 0, sliderHeight - size, 0, 0);

            v.setBackground(layers);
            v.setLayoutParams(new FrameLayout.LayoutParams(size, sliderHeight));
        } else {
            v.setBackgroundResource(R.drawable.note_circle);
            if (equippedSkin != null) {
                v.getBackground().setTint(equippedSkin.circleColor);

                // --- ADDING SKIN VISUALS DURING FALL ---
                String effect = equippedSkin.effectType;
                if ("glow".equals(effect) || "electric".equals(effect)) {
                    v.setElevation(10 * density);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        v.setOutlineAmbientShadowColor(equippedSkin.circleColor);
                        v.setOutlineSpotShadowColor(equippedSkin.circleColor);
                    }
                } else if ("ghost".equals(effect)) {
                    v.setAlpha(0.6f);
                } else if ("bubbles".equals(effect)) {
                    GradientDrawable gd = new GradientDrawable();
                    gd.setShape(GradientDrawable.OVAL);
                    gd.setColor(equippedSkin.circleColor);
                    gd.setAlpha(180);
                    gd.setStroke(3, Color.WHITE);
                    v.setBackground(gd);
                }

                // Continuous Animation for some skins
                if ("electric".equals(effect)) {
                    v.animate().alpha(0.5f).setDuration(100).setUpdateListener(animation -> {
                        if (v.getAlpha() < 0.6f) v.setAlpha(1f);
                        else v.setAlpha(0.5f);
                    }).start();
                }
            }
            v.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        }

        ((FrameLayout.LayoutParams) v.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
        FrameLayout lane = (n.getLane() == 0) ? laneLeft : laneRight;
        lane.addView(v);

        long timeDiff = n.getTimestamp() - currentSongTime;
        float noteHeadY = targetY - (timeDiff * speed);
        if (n.isSlider()) {
            int sliderHeight = (int) (n.getDuration() * speed) + size;
            v.setTranslationY(noteHeadY - (sliderHeight - size));
        } else {
            v.setTranslationY(noteHeadY);
        }

        activeNotes.add(new ActiveNote(n, v));
    }

    private void handleTap(int lane) {
        if (!isGameRunning) return;
        ActiveNote best = null;
        long minDiff = Long.MAX_VALUE;

        for (int i = 0; i < activeNotes.size(); i++) {
            ActiveNote an = activeNotes.get(i);
            if (an.data.getLane() == lane && !an.wasHit && !an.wasMissed) {
                long diff = Math.abs(currentSongTime - an.data.getTimestamp());
                if (diff < minDiff) {
                    minDiff = diff;
                    best = an;
                }
            }
        }

        if (best != null && minDiff <= GOOD_WINDOW) {
            best.wasHit = true;
            
            // Big kick to visualizer on hit
            for (int i = 0; i < vizHeights.length; i++) {
                vizHeights[i] += random.nextFloat() * 250f; // Higher bounce
            }

            if (best.data.isSlider()) {
                activeSliders.put(lane, best);
                showFeedback("HOLD", Color.CYAN, lane);
                if (listener != null) listener.onSliderStarted();
            } else {
                int pts = (minDiff <= PERFECT_WINDOW) ? 300 : 100;
                if (listener != null) listener.onNoteHit(pts);
                showFeedback(pts == 300 ? "300" : "100", pts == 300 ? Color.YELLOW : Color.WHITE, lane);
                spawnBurstParticles(lane == 0 ? targetLeft : targetRight, equippedSkin.circleColor);
                if (best.view.getParent() != null)
                    ((ViewGroup) best.view.getParent()).removeView(best.view);
            }
        }
    }

    private void handleMiss(ActiveNote an, boolean wasAlreadyStarted) {
        showFeedback("MISS", Color.RED, an.data.getLane());
        if (listener != null) listener.onNoteMissed(wasAlreadyStarted);
        if (an.view != null) {
            an.view.setBackgroundColor(Color.GRAY);
            an.view.setAlpha(0.5f);
            handler.postDelayed(() -> {
                if (an.view.getParent() != null)
                    ((ViewGroup) an.view.getParent()).removeView(an.view);
            }, 150);
        }
    }

    private void handleMiss(ActiveNote an) {
        handleMiss(an, false);
    }

    private void handlePerfectSlider(ActiveNote an) {
        showFeedback("PERFECT", Color.YELLOW, an.data.getLane());
        if (listener != null) listener.onNoteHit(150);
        an.view.animate().alpha(0).scaleX(1.1f).scaleY(1.1f).setDuration(150).withEndAction(() -> {
            if (an.view.getParent() != null) ((ViewGroup) an.view.getParent()).removeView(an.view);
        }).start();
    }

    private void spawnBurstParticles(View target, int color) {
        if (equippedSkin == null || listener == null) return;
        listener.onRequestEffect(target, equippedSkin.effectType, color);
    }

    private void showFeedback(String text, int color, int lane) {
        FrameLayout parent = (lane == 0) ? laneLeft : laneRight;
        if (parent == null) return;
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(24);
        tv.setTypeface(null, Typeface.BOLD);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2);
        lp.gravity = Gravity.CENTER;
        tv.setLayoutParams(lp);
        parent.addView(tv);
        tv.animate().translationYBy(-200).alpha(0).setDuration(350).withEndAction(() -> parent.removeView(tv)).start();
    }

    public interface GameEventListener {
        void onNoteHit(int points);

        default void onSliderStarted() {
        }

        void onNoteMissed(boolean wasAlreadyStarted);

        void onGameLoopTick();

        void onRequestSpinner(long duration);

        void onRequestEffect(View anchor, String effectType, int color);

        void onRequestTrail(View anchor, String effectType, int color);
    }

    private static class ActiveNote {
        Note data;
        View view;
        boolean wasHit = false;
        boolean wasMissed = false;

        ActiveNote(Note data, View view) {
            this.data = data;
            this.view = view;
        }
    }
}
