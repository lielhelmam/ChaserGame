package com.example.chasergame.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private static final int SLIDER_GRACE_PERIOD = 500; 

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private FrameLayout laneLeft, laneRight;
    private View targetLeft, targetRight;

    private Skin equippedSkin;
    private List<Note> allNotes = new ArrayList<>();
    private final List<ActiveNote> activeNotes = new ArrayList<>();
    private final Map<Integer, Boolean> lanePressed = new HashMap<>();
    private final Map<Integer, ActiveNote> activeSliders = new HashMap<>();

    private boolean isGameRunning = false;
    private long currentSongTime = 0;
    private long lastTickTime = 0;
    private GameEventListener listener;
    private final List<View> particlePool = new ArrayList<>();

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

    public GameView(Context context) { super(context); init(context); }
    public GameView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public GameView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(context); }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_game, this, true);
        laneLeft = findViewById(R.id.lane_left);
        laneRight = findViewById(R.id.lane_right);
        targetLeft = findViewById(R.id.target_left);
        targetRight = findViewById(R.id.target_right);
        lanePressed.put(0, false);
        lanePressed.put(1, false);
    }

    public void setGameEventListener(GameEventListener listener) { this.listener = listener; }

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

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (!isGameRunning) return;
            updateGameFrame();
            if (listener != null) listener.onGameLoopTick();
            handler.postDelayed(this, 16);
        }
    };

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

    private void applySkin() {
        if (equippedSkin == null) return;
        if (targetLeft != null) targetLeft.getBackground().setTint(equippedSkin.targetColor);
        if (targetRight != null) targetRight.getBackground().setTint(equippedSkin.targetColor);
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

        for (int i = 0; i < allNotes.size(); i++) {
            Note n = allNotes.get(i);
            if (currentSongTime >= n.getTimestamp() - VISIBLE_DURATION) {
                spawnNote(n);
                allNotes.remove(i);
                i--;
            } else break;
        }

        float density = getResources().getDisplayMetrics().density;
        float targetY = (targetLeft != null) ? (targetLeft.getTop() - laneLeft.getTop() + (targetLeft.getHeight() - (60*density)) / 2f) : 0;
        float speed = targetY / VISIBLE_DURATION;

        for (int i = activeNotes.size() - 1; i >= 0; i--) {
            if (!isGameRunning) break; 
            ActiveNote an = activeNotes.get(i);
            long timeDiff = an.data.getTimestamp() - currentSongTime;
            float noteHeadY = targetY - (timeDiff * speed);
            
            if (an.data.isSlider()) {
                float sliderHeadY = noteHeadY;
                int sliderHeight = (int) (an.data.getDuration() * speed) + (int)(60 * density);
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
                         if (currentSongTime - lastTickTime > 500) {
                             if (listener != null) listener.onNoteHit(2);
                             lastTickTime = currentSongTime;
                         }
                         spawnBurstParticles(an.data.getLane() == 0 ? targetLeft : targetRight, equippedSkin.circleColor);
                     }
                 }
            } else if (an.wasHit && !an.data.isSlider()) {
                if (isGameRunning && i < activeNotes.size()) activeNotes.remove(i);
            }
        }
    }

    private void spawnNote(Note n) {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (60 * density);
        View v = new View(getContext());
        float targetY = (targetLeft != null) ? (targetLeft.getTop() - laneLeft.getTop() + (targetLeft.getHeight() - size) / 2f) : 0;
        float speed = targetY / VISIBLE_DURATION;

        if (n.isSlider()) {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(equippedSkin != null ? equippedSkin.circleColor : Color.CYAN);
            gd.setCornerRadius(size / 2f);
            gd.setStroke(4, Color.WHITE);
            v.setBackground(gd);
            int sliderHeight = (int) (n.getDuration() * speed) + size;
            v.setLayoutParams(new FrameLayout.LayoutParams(size, sliderHeight));
        } else {
            v.setBackgroundResource(R.drawable.note_circle);
            if (equippedSkin != null) v.getBackground().setTint(equippedSkin.circleColor);
            v.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        }

        ((FrameLayout.LayoutParams)v.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
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
                if (diff < minDiff) { minDiff = diff; best = an; }
            }
        }

        if (best != null && minDiff <= GOOD_WINDOW) {
            best.wasHit = true;
            if (best.data.isSlider()) {
                activeSliders.put(lane, best);
                showFeedback("HOLD", Color.CYAN, lane);
                if (listener != null) listener.onSliderStarted();
            } else {
                int pts = (minDiff <= PERFECT_WINDOW) ? 300 : 100;
                if (listener != null) listener.onNoteHit(pts);
                showFeedback(pts == 300 ? "300" : "100", pts == 300 ? Color.YELLOW : Color.WHITE, lane);
                spawnBurstParticles(lane == 0 ? targetLeft : targetRight, equippedSkin.circleColor);
                if (best.view.getParent() != null) ((ViewGroup)best.view.getParent()).removeView(best.view);
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
                if (an.view.getParent() != null) ((ViewGroup)an.view.getParent()).removeView(an.view);
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
            if (an.view.getParent() != null) ((ViewGroup)an.view.getParent()).removeView(an.view);
        }).start();
    }

    private int particleCounter = 0;
    private void spawnBurstParticles(View target, int color) {
        if (equippedSkin == null) return;
        if ("none".equals(equippedSkin.effectType)) return;
        
        // Effect intensity logic
        int count = 2;
        long duration = 200;
        float scale = 0.1f;
        
        if ("particles".equals(equippedSkin.effectType) || "ghost".equals(equippedSkin.effectType) || "emerald".equals(equippedSkin.effectType)) {
            count = 8;
            duration = 400;
        } else if ("glow".equals(equippedSkin.effectType) || "retro".equals(equippedSkin.effectType) || "electric".equals(equippedSkin.effectType)) {
            count = 12; // More "glowy" burst
            duration = 300;
            scale = 0.5f; // Larger particles for glow
        } else if ("bubbles".equals(equippedSkin.effectType) || "frozen".equals(equippedSkin.effectType)) {
            count = 5;
            duration = 600; // Slower bubbles
        } else if ("sparkle".equals(equippedSkin.effectType) || "gold".equals(equippedSkin.effectType)) {
            count = 15;
            duration = 150; // Quick sparkles
        }

        if (count == 2 && ++particleCounter % 4 != 0) return;

        ViewGroup root = (ViewGroup) getRootView();
        if (root == null) return;
        float density = getResources().getDisplayMetrics().density;
        int[] tPos = new int[2], rPos = new int[2];
        target.getLocationOnScreen(tPos); root.getLocationOnScreen(rPos);
        float cx = (tPos[0] - rPos[0]) + target.getWidth() / 2f;
        float cy = (tPos[1] - rPos[1]) + target.getHeight() / 2f;

        for (int i = 0; i < count; i++) {
            final View p = particlePool.isEmpty() ? new View(getContext()) : particlePool.remove(particlePool.size() - 1);
            int pSize = (int) (12 * density);
            p.setLayoutParams(new FrameLayout.LayoutParams(pSize, pSize));
            GradientDrawable gd = new GradientDrawable(); gd.setShape(GradientDrawable.OVAL); gd.setColor(color); p.setBackground(gd);
            p.setX(cx - pSize / 2f); p.setY(cy - pSize / 2f);
            root.addView(p);
            float ang = (float) (random.nextFloat() * 2 * Math.PI), dist = (20 + random.nextFloat() * 40) * density;
            p.animate()
                    .translationXBy((float)Math.cos(ang)*dist)
                    .translationYBy((float)Math.sin(ang)*dist)
                    .alpha(0)
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(duration)
                    .withEndAction(() -> {
                        root.removeView(p);
                        p.setAlpha(1f);
                        p.setScaleX(1f);
                        p.setScaleY(1f);
                        particlePool.add(p);
                    }).start();
        }
    }

    private void showFeedback(String text, int color, int lane) {
        FrameLayout parent = (lane == 0) ? laneLeft : laneRight;
        if (parent == null) return;
        TextView tv = new TextView(getContext());
        tv.setText(text); tv.setTextColor(color); tv.setTextSize(24); tv.setTypeface(null, Typeface.BOLD);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2); lp.gravity = Gravity.CENTER; tv.setLayoutParams(lp);
        parent.addView(tv);
        tv.animate().translationYBy(-200).alpha(0).setDuration(350).withEndAction(() -> parent.removeView(tv)).start();
    }

    public interface GameEventListener {
        void onNoteHit(int points);
        default void onSliderStarted() {}
        void onNoteMissed(boolean wasAlreadyStarted);
        void onGameLoopTick();
    }
}
