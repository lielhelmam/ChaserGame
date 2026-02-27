package com.example.chasergame.screens;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chasergame.R;
import com.example.chasergame.models.Note;
import com.example.chasergame.models.SongData;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class SecretGameActivity extends AppCompatActivity {
    private static final String TAG = "SecretGameActivity";

    private SongData songData;
    private MediaPlayer mediaPlayer;
    private TextView tvScore, tvSongName;
    private FrameLayout laneLeft, laneRight;
    private View targetLeft, targetRight;

    private int currentScore = 0;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_secret_game);

        tvScore = findViewById(R.id.tv_game_score);
        tvSongName = findViewById(R.id.tv_game_song_name);
        laneLeft = findViewById(R.id.lane_left);
        laneRight = findViewById(R.id.lane_right);
        targetLeft = findViewById(R.id.target_left);
        targetRight = findViewById(R.id.target_right);

        String json = getIntent().getStringExtra("SONG_DATA_JSON");
        Log.d(TAG, "Received JSON: " + json);

        if (json != null) {
            try {
                songData = new Gson().fromJson(json, SongData.class);
                if (songData != null) {
                    tvSongName.setText(songData.getName());
                    remainingNotes = (songData.getNotes() != null) ? new ArrayList<>(songData.getNotes()) : new ArrayList<>();
                    Log.d(TAG, "Song loaded: " + songData.getName() + ", Notes count: " + remainingNotes.size());
                    
                    setupInputListeners();
                    // Wait for layout to be ready before starting
                    findViewById(R.id.game_layout).post(this::startGame);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing JSON", e);
            }
        }
    }

    private void setupInputListeners() {
        targetLeft.setOnClickListener(v -> checkHit(0));
        targetRight.setOnClickListener(v -> checkHit(1));
    }

    private void startGame() {
        if (songData == null || songData.getResName() == null) {
            Log.e(TAG, "Song data or resource name is missing!");
            return;
        }

        int resId = getResources().getIdentifier(songData.getResName(), "raw", getPackageName());
        Log.d(TAG, "Resource name: " + songData.getResName() + ", resId: " + resId);

        if (resId == 0) {
            Toast.makeText(this, "Audio file not found: " + songData.getResName(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer == null) {
                Log.e(TAG, "MediaPlayer is null after creation");
                finish();
                return;
            }
            mediaPlayer.setOnCompletionListener(mp -> endGame());
            mediaPlayer.start();
            
            startTime = System.currentTimeMillis();
            isGameRunning = true;
            
            gameHandler.post(gameLoop);
            Log.d(TAG, "Game loop started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting game", e);
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
        int size = (int) (60 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        noteView.setLayoutParams(params);

        FrameLayout parentLane = (note.getLane() == 0) ? laneLeft : laneRight;
        parentLane.addView(noteView);
        noteView.setTranslationY(-size);

        float targetY = targetLeft.getY();
        // Fallback if getY is not ready
        if (targetY == 0) targetY = laneLeft.getHeight() - (120 * getResources().getDisplayMetrics().density);

        noteView.animate()
                .translationY(targetY)
                .setDuration(NOTE_FALL_DURATION)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> parentLane.removeView(noteView))
                .start();
                
        noteView.setTag(note.getTimestamp());
    }

    private void checkHit(int lane) {
        if (!isGameRunning) return;
        long currentTime = System.currentTimeMillis() - startTime;
        FrameLayout laneView = (lane == 0) ? laneLeft : laneRight;
        
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
            if (minDiff < PERFECT_THRESHOLD) {
                currentScore += 300;
            } else {
                currentScore += 100;
            }
            tvScore.setText("Score: " + currentScore);
            laneView.removeView(bestNote);
        }
    }

    private void endGame() {
        isGameRunning = false;
        gameHandler.removeCallbacks(gameLoop);
        String msg = (currentScore >= songData.getTargetScore()) ? "Level Passed!" : "Level Failed!";
        Toast.makeText(this, msg + " Final Score: " + currentScore, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        isGameRunning = false;
        gameHandler.removeCallbacks(gameLoop);
    }
}
