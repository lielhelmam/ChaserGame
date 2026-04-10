package com.example.chasergame.screens;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
    private EffectOverlayView effectOverlay;

    // --- CANVAS EFFECTS CLASSES ---
    private static class Particle {
        float x, y, vx, vy, alpha, size, scale;
        int color;
        long lifeTime, maxLife;
        String type;

        Particle(float x, float y, float angle, float speed, int color, long life, String type, float size) {
            this.x = x; this.y = y;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            this.color = color;
            this.maxLife = life;
            this.lifeTime = life;
            this.alpha = 1f;
            this.type = type;
            this.size = size;
            this.scale = 1f;
        }

        boolean update(long dt) {
            lifeTime -= dt;
            x += vx * (dt / 16f);
            y += vy * (dt / 16f);
            alpha = Math.max(0, (float) lifeTime / maxLife);
            if ("sparkle".equals(type) || "gold".equals(type)) scale = alpha;
            return lifeTime > 0;
        }
    }

    private class EffectOverlayView extends android.view.View {
        private final List<Particle> particles = new ArrayList<>();
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private long lastFrameTime = System.currentTimeMillis();

        public EffectOverlayView(Context context) { super(context); }

        public void spawnBurst(float cx, float cy, String type, int color) {
            int count = 15;
            long life = 400;
            float baseSize = 12 * getResources().getDisplayMetrics().density;
            float speedMult = 1.0f;

            switch (type) {
                case "glow":
                case "electric":
                case "retro":
                    count = 15; life = 500; baseSize *= 1.5f; break;
                case "particles":
                case "ghost":
                case "emerald":
                    count = 25; life = 350; speedMult = 1.5f; break;
                case "sparkle":
                case "gold":
                    count = 40; life = 250; baseSize *= 0.6f; speedMult = 2.0f; break;
                case "bubbles":
                case "frozen":
                    count = 8; life = 800; baseSize *= 1.2f; speedMult = 0.6f; break;
            }

            for (int i = 0; i < count; i++) {
                float angle = (float) (Math.random() * 2 * Math.PI);
                float speed = (5 + (float) Math.random() * 10) * getResources().getDisplayMetrics().density * speedMult;
                particles.add(new Particle(cx, cy, angle, speed, color, life, type, baseSize));
            }
            invalidate();
        }

        public void spawnTrail(float cx, float cy, String type, int color) {
            float baseSize = 8 * getResources().getDisplayMetrics().density;
            long life = 300;
            float vx = 0, vy = 0;

            // Trail particles usually float up or stay put while the note moves down
            switch (type) {
                case "glow":
                case "electric":
                    life = 400; baseSize *= 1.2f; break;
                case "particles":
                case "emerald":
                    vx = (float) (Math.random() - 0.5) * 4; break;
                case "bubbles":
                    life = 600; baseSize *= 0.8f; vy = -2; break;
                case "retro":
                    life = 200; baseSize *= 2f; break;
                case "sparkle":
                case "gold":
                    life = 250; baseSize *= 0.5f; vx = (float) (Math.random() - 0.5) * 6; break;
            }

            Particle p = new Particle(cx, cy, 0, 0, color, life, type, baseSize);
            p.vx = vx; p.vy = vy;
            particles.add(p);
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            long now = System.currentTimeMillis();
            long dt = now - lastFrameTime;
            lastFrameTime = now;

            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                if (!p.update(dt)) {
                    particles.remove(i);
                    continue;
                }
                paint.setColor(p.color);
                paint.setAlpha((int) (p.alpha * 255));
                
                if ("bubbles".equals(p.type) || "frozen".equals(p.type)) {
                    paint.setStyle(android.graphics.Paint.Style.STROKE);
                    paint.setStrokeWidth(3 * getResources().getDisplayMetrics().density);
                } else {
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                }

                float r = (p.size / 2f) * p.scale;
                canvas.drawCircle(p.x, p.y, r, paint);
            }

            if (!particles.isEmpty()) {
                postInvalidateOnAnimation();
            }
        }
    }

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

        // Add Canvas Overlay
        effectOverlay = new EffectOverlayView(this);
        rootLayout.addView(effectOverlay, new ConstraintLayout.LayoutParams(-1, -1));
    }

    private void resolveSkin() {
        User user = authService.getCurrentUser();
        if (user != null) {
            String skinId = user.getEquippedSkin();
            equippedSkin = "custom".equals(skinId) ? SkinManager.getCustomSkinForUser(user) : SkinManager.getSkinById(skinId);
        } else equippedSkin = SkinManager.getSkinById("default");
    }

    private void applySkin() {
        // Handled by GameView
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

                int duration = audioService.getDuration();
                if (duration == 0) duration = 180000;
                
                List<Note> dynamicNotes = BeatmapGenerator.generate(songData.getBpm(), duration, songData.getDifficulty());
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
        gameView.startGame(notes, equippedSkin);
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
        if (gameManager.isGameOver()) handleGameOver();
    }

    private void updateHpUI() {
        int hp = gameManager.getCurrentHp();
        if (tvHp != null) tvHp.setText("HP: " + hp);
        if (pbHp != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) pbHp.setProgress(hp, true);
            else pbHp.setProgress(hp);
        }
    }

    private void handleGameOver() {
        audioService.pause();
        gameView.stop();
        endGame();
    }

    @Override
    public void onGameLoopTick() {
        if (audioService == null) return;
        int pos = audioService.getCurrentPosition();
        if (pbSongProgress != null) pbSongProgress.setProgress(pos);
        if (gameView != null) gameView.syncTime(pos);
    }

    @Override
    public void onRequestEffect(android.view.View anchor, String effectType, int color) {
        if (effectOverlay == null || effectType == null || "none".equals(effectType)) return;

        int[] pos = new int[2];
        anchor.getLocationOnScreen(pos);
        int[] rootPos = new int[2];
        rootLayout.getLocationOnScreen(rootPos);

        float cx = (pos[0] - rootPos[0]) + anchor.getWidth() / 2f;
        float cy = (pos[1] - rootPos[1]) + anchor.getHeight() / 2f;

        effectOverlay.spawnBurst(cx, cy, effectType, color);
    }

    @Override
    public void onRequestTrail(android.view.View anchor, String effectType, int color) {
        if (effectOverlay == null || effectType == null || "none".equals(effectType)) return;

        int[] pos = new int[2];
        anchor.getLocationOnScreen(pos);
        int[] rootPos = new int[2];
        rootLayout.getLocationOnScreen(rootPos);

        float cx = (pos[0] - rootPos[0]) + anchor.getWidth() / 2f;
        float cy = (pos[1] - rootPos[1]) + anchor.getHeight() / 2f;

        effectOverlay.spawnTrail(cx, cy, effectType, color);
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
        updateScores(!isDead ? gameManager.getCurrentScore() : 0, earned);

        new AlertDialog.Builder(this)
                .setTitle(isDead ? "Game Over!" : "Level Complete!")
                .setMessage(String.format(Locale.US, "Score: %d\nAccuracy: %.1f%%\nPoints Earned: %d\n%s",
                        gameManager.getCurrentScore(), gameManager.getAccuracy(), earned,
                        isDead ? "(You failed, earned 1/3 points)" : "(Survival Bonus Included!)"))
                .setCancelable(false)
                .setPositiveButton("Main Menu", (d, i) -> finish()).show();
    }

    private void updateScores(int score, int points) {
        User user = authService.getCurrentUser();
        if (user != null) {
            if (score > 0) user.setTotalRhythmScore(user.getTotalRhythmScore() + score);
            user.setPoints(user.getPoints() + points);
            authService.updateUserAndSync(user, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioService.release();
        if (gameView != null) gameView.stop();
    }
}
