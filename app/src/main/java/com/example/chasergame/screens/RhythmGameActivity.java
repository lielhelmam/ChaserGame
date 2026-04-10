package com.example.chasergame.screens;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
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
        float friction = 0.96f;
        float rotation = 0f;
        float rotationSpeed = 0f;
        int shapeType = 0; 
        float[] historyX = new float[5]; // For motion trails
        float[] historyY = new float[5];

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
            this.rotation = (float)(Math.random() * 360);
            this.rotationSpeed = (float)(Math.random() * 30 - 15);

            for(int i=0; i<5; i++) { historyX[i] = x; historyY[i] = y; }

            if ("bubbles".equals(type)) friction = 0.98f;
            else if ("electric".equals(type)) friction = 0.88f;
            else if ("retro".equals(type)) shapeType = 1;
            else if ("sparkle".equals(type) || "gold".equals(type)) {
                shapeType = 2;
                friction = 0.92f;
            }
        }

        boolean update(long dt) {
            // Update history
            for(int i=4; i>0; i--) { historyX[i] = historyX[i-1]; historyY[i] = historyY[i-1]; }
            historyX[0] = x; historyY[0] = y;

            lifeTime -= dt;
            vx *= friction;
            vy *= friction;
            if ("gold".equals(type)) vy += 0.6f;
            
            x += vx;
            y += vy;
            rotation += rotationSpeed;
            
            float progress = (float) lifeTime / maxLife;
            alpha = Math.max(0, progress);
            scale = 0.1f + (progress * 0.9f);
            
            return lifeTime > 0;
        }
    }

    private class EffectOverlayView extends android.view.View {
        private final List<Particle> particles = new ArrayList<>();
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final PorterDuffXfermode addMode = new PorterDuffXfermode(PorterDuff.Mode.ADD);
        private final Path tempPath = new Path();
        private long lastFrameTime = System.currentTimeMillis();

        public EffectOverlayView(Context context) { super(context); }

        public void spawnBurst(float cx, float cy, String type, int color) {
            int count = "sparkle".equals(type) ? 70 : 40;
            long life = "glow".equals(type) ? 800 : 500;
            float density = getResources().getDisplayMetrics().density;
            
            for (int i = 0; i < count; i++) {
                float angle = (float) (Math.random() * 2 * Math.PI);
                float speed = (2 + (float) Math.random() * 18) * density;
                particles.add(new Particle(cx, cy, angle, speed, color, life, type, 22 * density));
            }
            shakeScreen("glow".equals(type) ? 8 : 4);
            invalidate();
        }

        public void spawnTrail(float cx, float cy, String type, int color) {
            float density = getResources().getDisplayMetrics().density;
            float rx = (float)(Math.random() - 0.5) * 40 * density;
            particles.add(new Particle(cx + rx, cy, (float)(Math.PI*1.5), (float)Math.random() * 3 * density, color, 400, type, 12 * density));
            invalidate();
        }

        private void shakeScreen(float intensity) {
            float d = getResources().getDisplayMetrics().density;
            rootLayout.animate().translationX((float)((Math.random()-0.5)*2*intensity*d))
                .translationY((float)((Math.random()-0.5)*2*intensity*d)).setDuration(40)
                .withEndAction(() -> rootLayout.animate().translationX(0).translationY(0).setDuration(40).start()).start();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            long now = System.currentTimeMillis();
            long dt = Math.min(32, now - lastFrameTime); 
            lastFrameTime = now;

            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                if (!p.update(dt)) { particles.remove(i); continue; }
                
                float r = (p.size / 2f) * p.scale;
                
                // Draw Motion Trail (The "Bursts" tails)
                if (!"bubbles".equals(p.type)) {
                    paint.setXfermode(addMode);
                    paint.setStrokeWidth(r * 0.6f);
                    paint.setColor(p.color);
                    paint.setAlpha((int)(p.alpha * 100));
                    canvas.drawLine(p.x, p.y, p.historyX[2], p.historyY[2], paint);
                }

                // Core Drawing
                paint.setXfermode(addMode);
                paint.setColor(p.color);
                
                // Bloom Effect: Draw twice (Outer glow, then Inner core)
                paint.setAlpha((int) (p.alpha * 60));
                drawShape(canvas, p, r * 2.2f, paint);
                
                paint.setAlpha((int) (p.alpha * 255));
                drawShape(canvas, p, r, paint);

                // Electric Bolts
                if ("electric".equals(p.type) && Math.random() > 0.85 && i > 2) {
                    Particle p2 = particles.get(i-1);
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
                    canvas.drawLine(p.x, p.y, (p.x+p2.x)/2 + (float)(Math.random()-0.5)*20, (p.y+p2.y)/2 + (float)(Math.random()-0.5)*20, paint);
                    canvas.drawLine((p.x+p2.x)/2 + (float)(Math.random()-0.5)*20, (p.y+p2.y)/2 + (float)(Math.random()-0.5)*20, p2.x, p2.y, paint);
                }
            }
            paint.setXfermode(null);
            if (!particles.isEmpty()) postInvalidateOnAnimation();
        }

        private void drawShape(android.graphics.Canvas canvas, Particle p, float r, android.graphics.Paint paint) {
            canvas.save();
            canvas.translate(p.x, p.y);
            canvas.rotate(p.rotation);
            if (p.shapeType == 1) canvas.drawRect(-r, -r, r, r, paint);
            else if (p.shapeType == 2) {
                tempPath.reset(); tempPath.moveTo(0, -r*1.5f); tempPath.lineTo(r, 0); tempPath.lineTo(0, r*1.5f); tempPath.lineTo(-r, 0); tempPath.close();
                canvas.drawPath(tempPath, paint);
            } else canvas.drawCircle(0, 0, r, paint);
            canvas.restore();
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
