package com.example.chasergame.screens;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.chasergame.R;
import com.example.chasergame.models.Note;
import com.example.chasergame.models.Skin;
import com.example.chasergame.models.SongData;
import com.example.chasergame.models.User;
import com.example.chasergame.utils.SharedPreferencesUtil;
import com.example.chasergame.utils.SkinManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SecretGameActivity extends BaseActivity {
    private static final String TAG = "SecretGameActivity";

    private SongData songData;
    private MediaPlayer mediaPlayer;
    private TextView tvScore, tvSongName, tvAccuracy;
    private ProgressBar pbSongProgress;
    private FrameLayout laneLeft, laneRight;
    private View targetLeft, targetRight;
    private ConstraintLayout rootLayout;
    private Vibrator vibrator;
    private Skin equippedSkin;
    private Random random = new Random();

    private int currentScore = 0;
    private int totalNotesPassed = 0;
    private double notesHitWeight = 0; 
    private long startTime;
    private Handler gameHandler = new Handler(Looper.getMainLooper());
    private List<Note> remainingNotes;
    private boolean isGameRunning = false;

    private static final long NOTE_FALL_DURATION = 2000L; 
    private static final int PERFECT_THRESHOLD = 150;
    private static final int GOOD_THRESHOLD = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_secret_game);

            rootLayout = findViewById(R.id.game_layout);
            tvScore = findViewById(R.id.tv_game_score);
            tvSongName = findViewById(R.id.tv_game_song_name);
            tvAccuracy = findViewById(R.id.tv_game_accuracy);
            pbSongProgress = findViewById(R.id.pb_song_progress);
            laneLeft = findViewById(R.id.lane_left);
            laneRight = findViewById(R.id.lane_right);
            targetLeft = findViewById(R.id.target_left);
            targetRight = findViewById(R.id.target_right);

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            User user = SharedPreferencesUtil.getUser(this);
            String skinId = (user != null) ? user.getEquippedSkin() : "default";
            equippedSkin = SkinManager.getSkinById(skinId);
            applySkin();

            String songId = getIntent().getStringExtra("SONG_ID");
            if (songId != null) {
                loadSongData(songId);
            } else {
                finish();
            }
        } catch (Exception e) {
            finish();
        }
    }

    private void applySkin() {
        if (equippedSkin == null) return;
        rootLayout.setBackgroundColor(equippedSkin.backgroundColor);
        targetLeft.getBackground().setTint(equippedSkin.targetColor);
        targetRight.getBackground().setTint(equippedSkin.targetColor);
    }

    private void loadSongData(String songId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("rhythm_songs").child(songId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    songData = snapshot.getValue(SongData.class);
                    if (songData != null) {
                        tvSongName.setText(songData.getName());
                        List<Note> allNotes = songData.getNotes();
                        remainingNotes = new ArrayList<>();
                        if (allNotes != null) {
                            for (Note n : allNotes) {
                                if (n != null && n.getTimestamp() >= NOTE_FALL_DURATION) {
                                    remainingNotes.add(n);
                                }
                            }
                        }
                        setupInputListeners();
                        gameHandler.postDelayed(SecretGameActivity.this::startGame, 500);
                    } else {
                        finish();
                    }
                } catch (Exception e) {
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                finish();
            }
        });
    }

    private void setupInputListeners() {
        if (targetLeft != null) targetLeft.setOnClickListener(v -> checkHit(0));
        if (targetRight != null) targetRight.setOnClickListener(v -> checkHit(1));
    }

    private void startGame() {
        if (isFinishing() || isDestroyed() || songData == null) return;
        String resName = songData.getResName();
        int resId = getResources().getIdentifier(resName, "raw", getPackageName());
        if (resId == 0) {
            finish();
            return;
        }
        try {
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer == null) {
                finish();
                return;
            }
            pbSongProgress.setMax(mediaPlayer.getDuration());
            mediaPlayer.setOnCompletionListener(mp -> endGame());
            mediaPlayer.start();
            startTime = System.currentTimeMillis();
            isGameRunning = true;
            gameHandler.post(gameLoop);
        } catch (Exception e) {
            finish();
        }
    }

    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            if (isGameRunning) {
                updateGame();
                updateProgressBar();
                gameHandler.postDelayed(this, 16);
            }
        }
    };

    private void updateProgressBar() {
        if (mediaPlayer != null && isGameRunning) {
            pbSongProgress.setProgress(mediaPlayer.getCurrentPosition());
        }
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

    private void spawnNoteView(Note note) {
        final View noteView = new View(this);
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
                    // חישוב מיקום אבסולוטי ביחס ל-rootLayout
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
                    totalNotesPassed++;
                    updateAccuracyDisplay();
                    showFeedback("MISS", Color.RED, note.getLane());
                    vibrate(100); 
                    parentLane.removeView(noteView);
                }
            }
        });
        
        animator.start();
        noteView.setTag(note.getTimestamp());
    }

    private void spawnTrailParticle(float x, float y, int color, String type) {
        final View p = new View(this);
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
        
        p.setX(x - pSize/2f);
        p.setY(y - pSize/2f);
        
        rootLayout.addView(p, 1); // הוספה מעל הרקע אבל מתחת לשאר האלמנטים

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
                .withEndAction(() -> rootLayout.removeView(p))
                .start();
    }

    private void spawnParticle(float x, float y, int color, String type, FrameLayout parent, boolean isBurst) {
        final View p = new View(this);
        float density = getResources().getDisplayMetrics().density;
        
        int pSize = (int) ((isBurst ? 40 : 20) * density);
        p.setLayoutParams(new FrameLayout.LayoutParams(pSize, pSize));
        
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        p.setBackground(shape);
        
        p.setX(x - pSize/2f);
        p.setY(y - pSize/2f);
        
        parent.addView(p);

        float tx = (random.nextFloat() * 800 - 400) * density;
        float ty = (random.nextFloat() * 800 - 400) * density;
        
        p.animate()
                .translationXBy(tx)
                .translationYBy(ty)
                .alpha(0)
                .scaleX(0.1f)
                .scaleY(0.1f)
                .setDuration(1000)
                .withEndAction(() -> parent.removeView(p))
                .start();
    }

    private void checkHit(int lane) {
        if (!isGameRunning) return;
        long currentTime = System.currentTimeMillis() - startTime;
        FrameLayout laneView = (lane == 0) ? laneLeft : laneRight;
        if (laneView == null) return;
        View bestNote = null;
        long minDiff = Long.MAX_VALUE;
        for (int i = 0; i < laneView.getChildCount(); i++) {
            View v = laneView.getChildAt(i);
            if (v.getTag() instanceof Long) {
                long noteTimestamp = (long) v.getTag();
                long diff = Math.abs(currentTime - noteTimestamp);
                if (diff < minDiff) {
                    minDiff = diff;
                    bestNote = v;
                }
            }
        }
        if (bestNote != null && minDiff < GOOD_THRESHOLD) {
            totalNotesPassed++;
            
            if (equippedSkin != null) {
                float hitX = laneView.getWidth() / 2f;
                float hitY = bestNote.getY() + bestNote.getHeight()/2f;
                for(int i=0; i<30; i++) spawnParticle(hitX, hitY, equippedSkin.circleColor, equippedSkin.effectType, laneView, true);
            }

            if (minDiff < PERFECT_THRESHOLD) {
                currentScore += 300;
                notesHitWeight += 1.0; 
                showFeedback("300", Color.YELLOW, lane);
            } else {
                currentScore += 100;
                notesHitWeight += 0.5;
                showFeedback("100", Color.WHITE, lane);
            }
            tvScore.setText("Score: " + currentScore);
            updateAccuracyDisplay();
            vibrate(30); 
            laneView.removeView(bestNote);
        }
    }

    private void vibrate(long duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    private void updateAccuracyDisplay() {
        if (totalNotesPassed == 0) return;
        double accuracy = (notesHitWeight / totalNotesPassed) * 100.0;
        tvAccuracy.setText(String.format(Locale.US, "Acc: %.1f%%", accuracy));
    }

    private void showFeedback(String text, int color, int lane) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(32);
        tv.setTypeface(null, Typeface.BOLD);
        FrameLayout parent = (lane == 0) ? laneLeft : laneRight;
        if (parent == null) return;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        tv.setLayoutParams(params);
        parent.addView(tv);
        tv.animate().translationYBy(-300).alpha(0).setDuration(600).withEndAction(() -> parent.removeView(tv)).start();
    }

    private void endGame() {
        isGameRunning = false;
        gameHandler.removeCallbacks(gameLoop);
        
        final double finalAcc = totalNotesPassed > 0 ? (notesHitWeight / totalNotesPassed) * 100.0 : 0;
        int earnedPoints = currentScore / 10;
        if (finalAcc >= 95.0) earnedPoints += 500;
        
        updateUserPoints(earnedPoints);
        
        String msg = (currentScore >= (songData != null ? songData.getTargetScore() : 0)) ? "Level Passed!" : "Level Failed!";
        new AlertDialog.Builder(this)
                .setTitle(msg)
                .setMessage(String.format(Locale.US, "Score: %d\nAccuracy: %.1f%%\nPoints Earned: %d", currentScore, finalAcc, earnedPoints))
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(SecretGameActivity.this, SongSelectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void updateUserPoints(int earnedPoints) {
        User user = SharedPreferencesUtil.getUser(this);
        if (user != null) {
            user.setPoints(user.getPoints() + earnedPoints);
            SharedPreferencesUtil.saveUser(this, user);
            FirebaseDatabase.getInstance().getReference("users").child(user.getId()).child("points").setValue(user.getPoints());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isGameRunning = false;
        gameHandler.removeCallbacks(gameLoop);
    }
}
