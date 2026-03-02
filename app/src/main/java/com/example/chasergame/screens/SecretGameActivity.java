package com.example.chasergame.screens;

import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;

import com.example.chasergame.R;
import com.example.chasergame.models.Note;
import com.example.chasergame.models.SongData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SecretGameActivity extends BaseActivity {
    private static final String TAG = "SecretGameActivity";

    private SongData songData;
    private MediaPlayer mediaPlayer;
    private TextView tvScore, tvSongName, tvAccuracy;
    private FrameLayout laneLeft, laneRight;
    private View targetLeft, targetRight;

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

        Log.d(TAG, "onCreate: Activity started");

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_secret_game);

            tvScore = findViewById(R.id.tv_game_score);
            tvSongName = findViewById(R.id.tv_game_song_name);
            tvAccuracy = findViewById(R.id.tv_game_accuracy);
            laneLeft = findViewById(R.id.lane_left);
            laneRight = findViewById(R.id.lane_right);
            targetLeft = findViewById(R.id.target_left);
            targetRight = findViewById(R.id.target_right);

            String songId = getIntent().getStringExtra("SONG_ID");
            if (songId != null) {
                loadSongData(songId);
            } else {
                Toast.makeText(this, "No song selected!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Crash in onCreate", e);
            finish();
        }
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
                        
                        // Filter notes: Remove nulls and notes that are too early to be hit correctly
                        List<Note> allNotes = songData.getNotes();
                        remainingNotes = new ArrayList<>();
                        if (allNotes != null) {
                            for (Note n : allNotes) {
                                if (n != null && n.getTimestamp() >= NOTE_FALL_DURATION) {
                                    remainingNotes.add(n);
                                } else if (n != null) {
                                    Log.d(TAG, "Skipping early note at: " + n.getTimestamp());
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
        if (isFinishing() || isDestroyed()) return;
        if (songData == null) return;

        String resName = songData.getResName();
        int resId = getResources().getIdentifier(resName, "raw", getPackageName());

        if (resId == 0) {
            Toast.makeText(this, "Audio file not found: " + resName, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer == null) {
                finish();
                return;
            }
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
                gameHandler.postDelayed(this, 16);
            }
        }
    };

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
        View noteView = new View(this);
        noteView.setBackgroundResource(R.drawable.note_circle);
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (60 * density);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        noteView.setLayoutParams(params);

        FrameLayout parentLane = (note.getLane() == 0) ? laneLeft : laneRight;
        if (parentLane == null) return;

        parentLane.addView(noteView);
        noteView.setTranslationY(-size);

        float targetY;
        if (targetLeft != null && laneLeft != null && targetLeft.getHeight() > 0) {
            float relativeTargetTop = targetLeft.getTop() - laneLeft.getTop();
            float targetHeight = targetLeft.getHeight();
            targetY = relativeTargetTop + (targetHeight - size) / 2.0f;
        } else {
            targetY = parentLane.getHeight() - (100 * density);
        }

        float endY = parentLane.getHeight() + size;
        float distanceToTarget = targetY + size;
        float totalDistance = endY + size;
        long totalDuration = (long) (totalDistance * NOTE_FALL_DURATION / distanceToTarget);

        noteView.animate()
                .translationY(endY)
                .setDuration(totalDuration)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> {
                    if (parentLane != null && parentLane.indexOfChild(noteView) != -1) {
                        totalNotesPassed++;
                        updateAccuracyDisplay();
                        showFeedback("MISS", Color.RED, note.getLane());
                        parentLane.removeView(noteView);
                    }
                })
                .start();

        noteView.setTag(note.getTimestamp());
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
            laneView.removeView(bestNote);
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

    private void endGame() {
        isGameRunning = false;
        gameHandler.removeCallbacks(gameLoop);
        if (songData != null) {
            double finalAcc = totalNotesPassed > 0 ? (notesHitWeight / totalNotesPassed) * 100.0 : 0;
            String msg = (currentScore >= songData.getTargetScore()) ? "Level Passed!" : "Level Failed!";
            Toast.makeText(this, msg + "\nScore: " + currentScore + "\nAcc: " + String.format(Locale.US, "%.1f%%", finalAcc), Toast.LENGTH_LONG).show();
        }
        finish();
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
