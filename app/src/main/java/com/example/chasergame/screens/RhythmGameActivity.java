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
    private TextView tvScore, tvSongName, tvAccuracy;
    private ProgressBar pbSongProgress;
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
        pbSongProgress = findViewById(R.id.pb_song_progress);
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
        FirebaseDatabase.getInstance().getReference("rhythm_songs").child(songId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        songData = snapshot.getValue(SongData.class);
                        if (songData == null) {
                            finish();
                            return;
                        }

                        gameManager = new RhythmGameManager(songData);
                        tvSongName.setText(songData.getName());

                        List<Note> validNotes = new ArrayList<>();
                        if (songData.getNotes() != null) {
                            for (Note n : songData.getNotes()) {
                                if (n != null && n.getTimestamp() >= 2000L) validNotes.add(n);
                            }
                        }
                        startPlay(validNotes);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        finish();
                    }
                });
    }

    private void startPlay(List<Note> notes) {
        audioService.playSong(songData.getResName(), this::endGame);
        pbSongProgress.setMax(audioService.getDuration());
        gameView.startGame(notes, equippedSkin);
    }

    @Override
    public void onNoteHit(int points) {
        gameManager.onNoteHit(points);
        tvScore.setText("Score: " + gameManager.getCurrentScore());
        updateAccuracy();
        vibrate(30);
    }

    @Override
    public void onNoteMissed() {
        gameManager.onNoteMissed();
        updateAccuracy();
        vibrate(100);
    }

    @Override
    public void onGameLoopTick() {
        pbSongProgress.setProgress(audioService.getCurrentPosition());
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
        gameView.stop();
        boolean passed = gameManager.isLevelPassed();
        int earned = gameManager.calculateEarnedPoints();

        updateScores(passed ? gameManager.getCurrentScore() : 0, earned);

        new AlertDialog.Builder(this)
                .setTitle(passed ? "Level Passed!" : "Level Failed!")
                .setMessage(String.format(Locale.US, "Score: %d\nAccuracy: %.1f%%\nPoints: %d",
                        gameManager.getCurrentScore(), gameManager.getAccuracy(), earned))
                .setCancelable(false)
                .setPositiveButton("OK", (d, i) -> {
                    startActivity(new Intent(this, SongSelectionActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
