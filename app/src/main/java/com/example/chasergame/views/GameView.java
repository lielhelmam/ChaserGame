package com.example.chasergame.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.chasergame.R;
import com.example.chasergame.models.Note;
import com.example.chasergame.models.Skin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends ConstraintLayout {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final long NOTE_FALL_DURATION = 2000L;
    private static final int PERFECT_THRESHOLD = 150;
    private static final int GOOD_THRESHOLD = 300;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    // -------------------------------------------------------------------------
    // Child views — found after inflation
    // -------------------------------------------------------------------------
    private FrameLayout laneLeft;
    private FrameLayout laneRight;
    private View targetLeft;
    private View targetRight;
    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private Skin equippedSkin;
    private List<Note> remainingNotes;
    private boolean isGameRunning = false;
    private long startTime;
    private GameEventListener listener;
    // -------------------------------------------------------------------------
    // Game loop
    // -------------------------------------------------------------------------
    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (!isGameRunning) return;
            updateGame();
            if (listener != null) listener.onGameLoopTick();
            handler.postDelayed(this, 16);
        }
    };

    // -------------------------------------------------------------------------
    // Constructors — all three needed for XML inflation
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Inflation
    // -------------------------------------------------------------------------
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_game, this, true);

        laneLeft = findViewById(R.id.lane_left);
        laneRight = findViewById(R.id.lane_right);
        targetLeft = findViewById(R.id.target_left);
        targetRight = findViewById(R.id.target_right);
    }

    // -------------------------------------------------------------------------
    // Wiring
    // -------------------------------------------------------------------------
    public void setGameEventListener(GameEventListener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Start the game loop. Call this after MediaPlayer has started in the Activity.
     */
    public void startGame(List<Note> notes, Skin skin) {
        this.equippedSkin = skin;
        this.remainingNotes = new ArrayList<>(notes);
        this.startTime = System.currentTimeMillis();
        this.isGameRunning = true;

        applySkin();
        setupInputListeners();
        handler.post(gameLoop);
    }

    /**
     * Stop the game loop and clear live note views.
     * Called by the Activity when the song finishes.
     */
    public void stop() {
        isGameRunning = false;
        handler.removeCallbacks(gameLoop);
        if (laneLeft != null) laneLeft.removeAllViews();
        if (laneRight != null) laneRight.removeAllViews();
    }

    // -------------------------------------------------------------------------
    // Skin
    // -------------------------------------------------------------------------
    private void applySkin() {
        if (equippedSkin == null) return;
        if (targetLeft != null) targetLeft.getBackground().setTint(equippedSkin.targetColor);
        if (targetRight != null) targetRight.getBackground().setTint(equippedSkin.targetColor);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------
    private void setupInputListeners() {
        if (targetLeft != null) targetLeft.setOnClickListener(v -> checkHit(0));
        if (targetRight != null) targetRight.setOnClickListener(v -> checkHit(1));
    }

    private void updateGame() {
        if (remainingNotes == null || remainingNotes.isEmpty()) return;
        long elapsed = System.currentTimeMillis() - startTime;

        List<Note> toSpawn = new ArrayList<>();
        for (Note note : remainingNotes) {
            if (elapsed >= (note.getTimestamp() - NOTE_FALL_DURATION)) {
                toSpawn.add(note);
            }
        }
        for (Note note : toSpawn) {
            spawnNoteView(note);
            remainingNotes.remove(note);
        }
    }

    // -------------------------------------------------------------------------
    // Note spawning & animation
    // -------------------------------------------------------------------------
    private void spawnNoteView(Note note) {
        final View noteView = new View(getContext());
        noteView.setBackgroundResource(R.drawable.note_circle);
        if (equippedSkin != null) {
            noteView.getBackground().setTint(equippedSkin.circleColor);
        }

        float density = getResources().getDisplayMetrics().density;
        final int size = (int) (60 * density);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        noteView.setLayoutParams(params);

        final FrameLayout parentLane = (note.getLane() == 0) ? laneLeft : laneRight;
        if (parentLane == null) return;
        parentLane.addView(noteView);
        noteView.setTranslationY(-size);

        float targetY;
        if (targetLeft != null && laneLeft != null && targetLeft.getHeight() > 0) {
            float relativeTargetTop = targetLeft.getTop() - laneLeft.getTop();
            targetY = relativeTargetTop + (targetLeft.getHeight() - size) / 2.0f;
        } else {
            targetY = parentLane.getHeight() - (100 * density);
        }

        float endY = parentLane.getHeight() + size;
        long totalDuration = (long) ((endY + size) * NOTE_FALL_DURATION / (targetY + size));

        ValueAnimator animator = ValueAnimator.ofFloat(-size, endY);
        animator.setDuration(totalDuration);
        animator.setInterpolator(new LinearInterpolator());

        final int[] trailCounter = {0};
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            noteView.setTranslationY(val);

            if (equippedSkin != null && !"none".equals(equippedSkin.effectType)) {
                trailCounter[0]++;
                if (trailCounter[0] % 2 == 0) {
                    float centerX = parentLane.getLeft() + (parentLane.getWidth() / 2f);
                    float currentY = parentLane.getTop() + val + (size / 2f);
                    spawnTrailParticle(centerX, currentY, equippedSkin.circleColor, equippedSkin.effectType);
                }
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (parentLane.indexOfChild(noteView) != -1) {
                    parentLane.removeView(noteView);
                    showFeedback("MISS", Color.RED, note.getLane());
                    if (listener != null) listener.onNoteMissed();
                }
            }
        });

        animator.start();
        noteView.setTag(note.getTimestamp());
    }

    // -------------------------------------------------------------------------
    // Hit detection
    // -------------------------------------------------------------------------
    private void checkHit(int lane) {
        if (!isGameRunning) return;
        long currentTime = System.currentTimeMillis() - startTime;

        FrameLayout laneView = (lane == 0) ? laneLeft : laneRight;
        View targetView = (lane == 0) ? targetLeft : targetRight;
        if (laneView == null || targetView == null) return;

        View bestNote = null;
        long minDiff = Long.MAX_VALUE;
        for (int i = 0; i < laneView.getChildCount(); i++) {
            View v = laneView.getChildAt(i);
            if (v.getTag() instanceof Long) {
                long diff = Math.abs(currentTime - (long) v.getTag());
                if (diff < minDiff) {
                    minDiff = diff;
                    bestNote = v;
                }
            }
        }

        if (bestNote != null && minDiff < GOOD_THRESHOLD) {
            if (equippedSkin != null) {
                spawnBurstParticles(targetView, equippedSkin.circleColor);
            }

            int points;
            if (minDiff < PERFECT_THRESHOLD) {
                points = 300;
                showFeedback("300", Color.YELLOW, lane);
            } else {
                points = 100;
                showFeedback("100", Color.WHITE, lane);
            }

            laneView.removeView(bestNote);
            if (listener != null) listener.onNoteHit(points);
        }
    }

    // -------------------------------------------------------------------------
    // Particles — drawn on the root view so they appear above everything
    // -------------------------------------------------------------------------
    private ConstraintLayout getOverlayRoot() {
        View root = getRootView();
        if (root instanceof ConstraintLayout) return (ConstraintLayout) root;
        // Fallback: draw on GameView itself
        return this;
    }

    private void spawnTrailParticle(float x, float y, int color, String type) {
        ConstraintLayout root = getOverlayRoot();
        final View p = new View(getContext());
        float density = getResources().getDisplayMetrics().density;
        int pSize = (int) (25 * density);

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(pSize, pSize);
        p.setLayoutParams(params);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        shape.setStroke(2, Color.WHITE);
        shape.setAlpha(180);
        p.setBackground(shape);

        p.setX(x - pSize / 2f);
        p.setY(y - pSize / 2f);
        root.addView(p, 1);

        float tx = (random.nextFloat() * 40 - 20) * density;
        float ty = (random.nextFloat() * 40 - 20) * density;
        float scaleTarget = 0.5f;
        long duration = 600;

        if ("bubbles".equals(type)) {
            ty = -(100 + random.nextFloat() * 100) * density;
            duration = 1000;
        } else if ("glow".equals(type)) {
            scaleTarget = 3.0f;
            duration = 400;
        }

        p.animate()
                .translationXBy(tx)
                .translationYBy(ty)
                .alpha(0)
                .scaleX(scaleTarget)
                .scaleY(scaleTarget)
                .setDuration(duration)
                .withEndAction(() -> root.removeView(p))
                .start();
    }

    private void spawnBurstParticles(View target, int color) {
        ConstraintLayout root = getOverlayRoot();
        float density = getResources().getDisplayMetrics().density;

        // Convert target coordinates to root's coordinate space
        int[] targetPos = new int[2];
        int[] rootPos = new int[2];
        target.getLocationOnScreen(targetPos);
        root.getLocationOnScreen(rootPos);

        float centerX = (targetPos[0] - rootPos[0]) + target.getWidth() / 2f;
        float centerY = (targetPos[1] - rootPos[1]) + target.getHeight() / 2f;

        for (int i = 0; i < 20; i++) {
            final View p = new View(getContext());
            int pSize = (int) (25 * density);

            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(pSize, pSize);
            p.setLayoutParams(lp);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(color);
            p.setBackground(shape);

            p.setX(centerX - pSize / 2f);
            p.setY(centerY - pSize / 2f);
            root.addView(p);

            float angle = (float) (random.nextFloat() * 2 * Math.PI);
            float distance = (60 + random.nextFloat() * 140) * density;
            float tx = (float) (Math.cos(angle) * distance);
            float ty = (float) (Math.sin(angle) * distance);

            p.animate()
                    .translationXBy(tx)
                    .translationYBy(ty)
                    .alpha(0)
                    .scaleX(0.1f)
                    .scaleY(0.1f)
                    .setDuration(500 + random.nextInt(300))
                    .withEndAction(() -> root.removeView(p))
                    .start();
        }
    }

    // -------------------------------------------------------------------------
    // Feedback text
    // -------------------------------------------------------------------------
    private void showFeedback(String text, int color, int lane) {
        FrameLayout parent = (lane == 0) ? laneLeft : laneRight;
        if (parent == null) return;

        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(32);
        tv.setTypeface(null, Typeface.BOLD);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        tv.setLayoutParams(params);

        parent.addView(tv);
        tv.animate()
                .translationYBy(-300)
                .alpha(0)
                .setDuration(600)
                .withEndAction(() -> parent.removeView(tv))
                .start();
    }

    // -------------------------------------------------------------------------
    // Callback interface — Activity implements this
    // -------------------------------------------------------------------------
    public interface GameEventListener {
        void onNoteHit(int points);  // 300 (perfect) or 100 (good)

        void onNoteMissed();

        void onGameLoopTick();       // called every ~16ms for progress bar
    }
}