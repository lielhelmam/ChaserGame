package com.example.chasergame.screens;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import com.example.chasergame.views.GameView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SecretGameActivity extends BaseActivity implements GameView.GameEventListener {

    // -------------------------------------------------------------------------
    // Views
    // -------------------------------------------------------------------------
    private GameView        gameView;
    private TextView        tvScore, tvSongName, tvAccuracy;
    private ProgressBar     pbSongProgress;
    private ConstraintLayout rootLayout;

    // -------------------------------------------------------------------------
    // Game data
    // -------------------------------------------------------------------------
    private SongData songData;
    private Skin     equippedSkin;
    private MediaPlayer mediaPlayer;
    private Vibrator    vibrator;

    // -------------------------------------------------------------------------
    // Score tracking (lives here, not in GameView)
    // -------------------------------------------------------------------------
    private int    currentScore     = 0;
    private int    totalNotesPassed = 0;
    private double notesHitWeight   = 0;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_secret_game);

            bindViews();
            resolveEquippedSkin();
            applyBackgroundSkin();

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (gameView != null) {
            gameView.stop();
        }
    }

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------
    private void bindViews() {
        rootLayout     = findViewById(R.id.game_layout);
        tvScore        = findViewById(R.id.tv_game_score);
        tvSongName     = findViewById(R.id.tv_game_song_name);
        tvAccuracy     = findViewById(R.id.tv_game_accuracy);
        pbSongProgress = findViewById(R.id.pb_song_progress);

        // GameView is declared in XML — it inflates and finds its own children
        gameView = findViewById(R.id.game_view);
        gameView.setGameEventListener(this);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void resolveEquippedSkin() {
        User user = SharedPreferencesUtil.getUser(this);
        if (user != null) {
            String skinId = user.getEquippedSkin();
            equippedSkin = "custom".equals(skinId)
                    ? SkinManager.getCustomSkinForUser(user)
                    : SkinManager.getSkinById(skinId);
        } else {
            equippedSkin = SkinManager.getSkinById("default");
        }
    }

    private void applyBackgroundSkin() {
        if (equippedSkin != null && rootLayout != null) {
            rootLayout.setBackgroundColor(equippedSkin.backgroundColor);
        }
    }

    // -------------------------------------------------------------------------
    // Firebase → load song
    // -------------------------------------------------------------------------
    private void loadSongData(String songId) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("rhythm_songs").child(songId);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    songData = snapshot.getValue(SongData.class);
                    if (songData == null) { finish(); return; }

                    tvSongName.setText(songData.getName());

                    // Filter out notes that are too early to animate properly
                    List<Note> allNotes = songData.getNotes();
                    List<Note> validNotes = new ArrayList<>();
                    if (allNotes != null) {
                        for (Note n : allNotes) {
                            if (n != null && n.getTimestamp() >= 2000L) {
                                validNotes.add(n);
                            }
                        }
                    }

                    startMediaPlayer(validNotes);
                } catch (Exception e) {
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { finish(); }
        });
    }

    // -------------------------------------------------------------------------
    // MediaPlayer
    // -------------------------------------------------------------------------
    private void startMediaPlayer(List<Note> validNotes) {
        if (isFinishing() || isDestroyed() || songData == null) return;

        int resId = getResources().getIdentifier(
                songData.getResName(), "raw", getPackageName());
        if (resId == 0) { finish(); return; }

        try {
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer == null) { finish(); return; }

            pbSongProgress.setMax(mediaPlayer.getDuration());
            mediaPlayer.setOnCompletionListener(mp -> endGame());
            mediaPlayer.start();

            // Hand off to GameView now that the song is playing
            gameView.startGame(validNotes, equippedSkin);
        } catch (Exception e) {
            finish();
        }
    }

    // -------------------------------------------------------------------------
    // GameView.GameEventListener
    // -------------------------------------------------------------------------
    @Override
    public void onNoteHit(int points) {
        totalNotesPassed++;
        notesHitWeight += (points == 300) ? 1.0 : 0.5;
        currentScore   += points;

        tvScore.setText("Score: " + currentScore);
        updateAccuracyDisplay();
        vibrate(30);
    }

    @Override
    public void onNoteMissed() {
        totalNotesPassed++;
        updateAccuracyDisplay();
        vibrate(100);
    }

    @Override
    public void onGameLoopTick() {
        if (mediaPlayer != null) {
            pbSongProgress.setProgress(mediaPlayer.getCurrentPosition());
        }
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------
    private void updateAccuracyDisplay() {
        if (totalNotesPassed == 0) return;
        double accuracy = (notesHitWeight / totalNotesPassed) * 100.0;
        tvAccuracy.setText(String.format(Locale.US, "Acc: %.1f%%", accuracy));
    }

    private void vibrate(long duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    // -------------------------------------------------------------------------
    // End game
    // -------------------------------------------------------------------------
    private void endGame() {
        if (gameView != null) gameView.stop();

        final double finalAcc = totalNotesPassed > 0
                ? (notesHitWeight / totalNotesPassed) * 100.0
                : 0;

        int targetScore  = (songData != null) ? songData.getTargetScore() : 0;
        boolean passed   = currentScore >= targetScore;

        int earnedPoints;
        if (passed) {
            earnedPoints = currentScore / 10;
            if (finalAcc >= 95.0) earnedPoints += 500;
            updateUserRhythmScore(currentScore);
        } else {
            earnedPoints = (currentScore / 10) / 3;
        }

        updateUserPoints(earnedPoints);

        String title = passed ? "Level Passed!" : "Level Failed!";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(String.format(Locale.US,
                        "Score: %d\nAccuracy: %.1f%%\nPoints Earned: %d",
                        currentScore, finalAcc, earnedPoints))
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(this, SongSelectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    // -------------------------------------------------------------------------
    // Firebase score updates
    // -------------------------------------------------------------------------
    private void updateUserRhythmScore(int score) {
        User user = SharedPreferencesUtil.getUser(this);
        if (user != null) {
            user.setTotalRhythmScore(user.getTotalRhythmScore() + score);
            SharedPreferencesUtil.saveUser(this, user);
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getId()).child("totalRhythmScore")
                    .setValue(user.getTotalRhythmScore());
        }
    }

    private void updateUserPoints(int earnedPoints) {
        User user = SharedPreferencesUtil.getUser(this);
        if (user != null) {
            user.setPoints(user.getPoints() + earnedPoints);
            SharedPreferencesUtil.saveUser(this, user);
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getId()).child("points")
                    .setValue(user.getPoints());
        }
    }
}