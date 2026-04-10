package com.example.chasergame.screens;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import com.example.chasergame.services.AudioService;
import com.example.chasergame.services.BeatmapGenerator;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.services.RhythmGameManager;
import com.example.chasergame.utils.SkinManager;
import com.example.chasergame.views.GameView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RhythmGameActivity extends BaseActivity implements GameView.GameEventListener {

    private GameView gameView;
    private TextView tvScore, tvSongName, tvAccuracy, tvHp;
    private ProgressBar pbSongProgress, pbHp;
    private ConstraintLayout rootLayout;

    private RhythmGameManager gameManager;
    private AudioService audioService;
    private SongData songData;
    private Skin equippedSkin;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rhythm_game);

        initViews();
        resolveSkin();
        applySkin();

        String songId = getIntent().getStringExtra("SONG_ID");
        if (songId != null) loadSong(songId);
        else finish();
    }

    private void initViews() {
        rootLayout = findViewById(R.id.game_layout);
        tvScore = findViewById(R.id.tv_game_score);
        tvSongName = findViewById(R.id.tv_game_song_name);
        tvAccuracy = findViewById(R.id.tv_game_accuracy);
        tvHp = findViewById(R.id.tv_game_hp);
        pbSongProgress = findViewById(R.id.pb_song_progress);
        pbHp = findViewById(R.id.pb_game_hp);
        if (pbHp != null) pbHp.setMax(10);
        gameView = findViewById(R.id.game_view);
        gameView.setGameEventListener(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioService = new AudioService(this);
    }

    private void resolveSkin() {
        User user = authService.getCurrentUser();
        if (user != null) {
            String skinId = user.getEquippedSkin();
            equippedSkin = "custom".equals(skinId) ? SkinManager.getCustomSkinForUser(user) : SkinManager.getSkinById(skinId);
        } else equippedSkin = SkinManager.getSkinById("default");
    }

    private void applySkin() {
        if (equippedSkin != null && rootLayout != null)
            rootLayout.setBackgroundColor(equippedSkin.backgroundColor);
    }

    private void loadSong(String songId) {
        databaseService.getSongById(songId, new DatabaseService.DatabaseCallback<SongData>() {
            @Override
            public void onCompleted(SongData song) {
                songData = song;
                if (songData == null) {
                    finish();
                    return;
                }

                gameManager = new RhythmGameManager(songData);
                tvSongName.setText(songData.getName());

                // Bug 2 Fix Part: Generate dynamic beatmap based on BPM and duration
                int duration = audioService.getDuration();
                if (duration == 0) {
                    // Fallback to average duration if MediaPlayer is not ready
                    duration = 180000; // 3 mins
                }
                
                List<Note> dynamicNotes = BeatmapGenerator.generate(
                        songData.getBpm(), 
                        duration, 
                        songData.getDifficulty()
                );
                
                startPlay(dynamicNotes);
            }

            @Override
            public void onFailed(Exception e) {
                finish();
            }
        });
    }

    private void startPlay(List<Note> notes) {
        audioService.playSong(songData.getResName(), this::endGame);
        int duration = audioService.getDuration();
        pbSongProgress.setMax(duration);
        
        // Re-generate if we didn't have accurate duration before
        List<Note> finalNotes = notes;
        if (notes.isEmpty() || notes.get(notes.size()-1).getTimestamp() < duration - 5000) {
             finalNotes = BeatmapGenerator.generate(songData.getBpm(), duration, songData.getDifficulty());
        }
        
        gameView.startGame(finalNotes, equippedSkin);
    }

    @Override
    public void onNoteHit(int points) {
        if (gameManager.isGameOver()) return;
        gameManager.onNoteHit(points);
        tvScore.setText("Score: " + gameManager.getCurrentScore());
        updateAccuracy();
        updateHpUI();
        if (points >= 100) vibrate(30);
    }

    @Override
    public void onSliderStarted() {
        if (gameManager.isGameOver()) return;
        gameManager.onSliderStarted();
        updateAccuracy();
    }

    @Override
    public void onNoteMissed(boolean wasAlreadyStarted) {
        if (gameManager.isGameOver()) return;
        gameManager.onNoteMissed(wasAlreadyStarted);
        updateAccuracy();
        updateHpUI();
        vibrate(100);
        
        if (gameManager.isGameOver()) {
            handleGameOver();
        }
    }

    private void updateHpUI() {
        int hp = gameManager.getCurrentHp();
        if (tvHp != null) tvHp.setText("HP: " + hp);
        if (pbHp != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pbHp.setProgress(hp, true);
            } else {
                pbHp.setProgress(hp);
            }
        }
    }

    private void handleGameOver() {
        audioService.pause(); // Stop music immediately
        gameView.stop();
        endGame();
    }

    @Override
    public void onGameLoopTick() {
        int pos = audioService.getCurrentPosition();
        pbSongProgress.setProgress(pos);
        gameView.syncTime(pos);
    }

    private void updateAccuracy() {
        tvAccuracy.setText(String.format(Locale.US, "Acc: %.1f%%", gameManager.getAccuracy()));
    }

    private void vibrate(long d) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createOneShot(d, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(d);
        }
    }

    private void endGame() {
        audioService.pause();
        gameView.stop();
        boolean isDead = gameManager.isGameOver();
        int earned = gameManager.calculateEarnedPoints();

        // If not dead, we consider it a pass
        updateScores(!isDead ? gameManager.getCurrentScore() : 0, earned);

        new AlertDialog.Builder(this)
                .setTitle(isDead ? "Game Over!" : "Level Complete!")
                .setMessage(String.format(Locale.US, "Score: %d\nAccuracy: %.1f%%\nPoints Earned: %d\n%s",
                        gameManager.getCurrentScore(), 
                        gameManager.getAccuracy(), 
                        earned,
                        isDead ? "(You failed, earned 1/3 points)" : "(Survival Bonus Included!)"))
                .setCancelable(false)
                .setPositiveButton("Main Menu", (d, i) -> {
                    finish();
                }).show();
    }

    private void updateScores(int score, int points) {
        User user = authService.getCurrentUser();
        if (user != null) {
            if (score > 0) user.setTotalRhythmScore(user.getTotalRhythmScore() + score);
            user.setPoints(user.getPoints() + points);
            databaseService.updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void unused) {
                    authService.syncUser(user);
                }

                @Override
                public void onFailed(Exception e) {
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioService.release();
        if (gameView != null) gameView.stop();
    }
}
