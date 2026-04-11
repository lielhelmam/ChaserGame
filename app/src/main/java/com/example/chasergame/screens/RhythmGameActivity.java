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
    private TextView tvScore, tvSongName, tvAccuracy, tvHp, tvCurrentRank;
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
        float[] historyX = new float[6]; 
        float[] historyY = new float[6];
        boolean isDebris = false;

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
            this.rotationSpeed = (float)(Math.random() * 40 - 20);

            for(int i=0; i<6; i++) { historyX[i] = x; historyY[i] = y; }

            if ("bubbles".equals(type)) friction = 0.98f;
            else if ("electric".equals(type)) friction = 0.85f;
            else if ("retro".equals(type)) shapeType = 1;
            else if ("sparkle".equals(type) || "gold".equals(type)) {
                shapeType = 2;
                friction = 0.92f;
            } else if ("frozen".equals(type)) {
                shapeType = 3;
            } else if ("emerald".equals(type)) {
                shapeType = 4;
                friction = 0.94f;
            }
        }

        boolean update(long dt) {
            for(int i=5; i>0; i--) { historyX[i] = historyX[i-1]; historyY[i] = historyY[i-1]; }
            historyX[0] = x; historyY[0] = y;

            lifeTime -= dt;
            vx *= friction;
            vy *= friction;
            if ("gold".equals(type)) vy += 0.7f;
            
            x += vx;
            y += vy;
            rotation += rotationSpeed;
            
            float progress = (float) lifeTime / maxLife;
            alpha = Math.max(0, progress);
            scale = isDebris ? progress : (0.1f + (progress * 0.9f));
            
            return lifeTime > 0;
        }
    }

    private static class Shockwave {
        float x, y, radius, maxRadius;
        int color;
        float alpha = 1f;
        
        Shockwave(float x, float y, float maxRadius, int color) {
            this.x = x; this.y = y; this.maxRadius = maxRadius; this.color = color;
            this.radius = 0f;
        }
        
        boolean update(long dt) {
            radius += (maxRadius - radius) * 0.15f;
            alpha -= 0.05f;
            return alpha > 0;
        }
    }

    private class EffectOverlayView extends android.view.View {
        private final List<Particle> particles = new ArrayList<>();
        private final List<Shockwave> shockwaves = new ArrayList<>();
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final PorterDuffXfermode addMode = new PorterDuffXfermode(PorterDuff.Mode.ADD);
        private final Path tempPath = new Path();
        private long lastFrameTime = System.currentTimeMillis();
        private float screenFlashAlpha = 0f;
        private int flashColor = Color.WHITE;

        public EffectOverlayView(Context context) { super(context); }

        public void spawnBurst(float cx, float cy, String type, int color) {
            float d = getResources().getDisplayMetrics().density;
            
            // Primary Particles
            int count = "sparkle".equals(type) ? 60 : 30;
            for (int i = 0; i < count; i++) {
                float angle = (float) (Math.random() * 2 * Math.PI);
                float speed = (3 + (float) Math.random() * 15) * d;
                particles.add(new Particle(cx, cy, angle, speed, color, 600, type, 24 * d));
            }
            
            // Fast Debris (The "Pro" touch)
            for (int i = 0; i < 20; i++) {
                float angle = (float) (Math.random() * 2 * Math.PI);
                float speed = (15 + (float) Math.random() * 20) * d;
                Particle p = new Particle(cx, cy, angle, speed, Color.WHITE, 250, type, 6 * d);
                p.isDebris = true;
                particles.add(p);
            }

            // Shockwave
            shockwaves.add(new Shockwave(cx, cy, 150 * d, color));
            
            // Trigger Flash for high-impact skins
            if ("glow".equals(type) || "electric".equals(type) || "sparkle".equals(type)) {
                screenFlashAlpha = 0.15f;
                flashColor = color;
            }

            shakeScreen("glow".equals(type) || "electric".equals(type) ? 12 : 6);
            invalidate();
        }

        public void spawnTrail(float cx, float cy, String type, int color) {
            float d = getResources().getDisplayMetrics().density;
            float rx = (float)(Math.random() - 0.5) * 45 * d;
            particles.add(new Particle(cx + rx, cy, (float)(Math.PI*1.5), (float)Math.random() * 4 * d, color, 350, type, 14 * d));
            invalidate();
        }

        private void shakeScreen(float intensity) {
            float d = getResources().getDisplayMetrics().density;
            rootLayout.animate().translationX((float)((Math.random()-0.5)*2*intensity*d))
                .translationY((float)((Math.random()-0.5)*2*intensity*d)).setDuration(30)
                .withEndAction(() -> rootLayout.animate().translationX(0).translationY(0).setDuration(30).start()).start();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            long now = System.currentTimeMillis();
            long dt = Math.min(32, now - lastFrameTime); 
            lastFrameTime = now;

            // Draw Screen Flash
            if (screenFlashAlpha > 0) {
                paint.setXfermode(addMode);
                paint.setColor(flashColor);
                paint.setAlpha((int)(screenFlashAlpha * 255));
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
                screenFlashAlpha -= 0.02f;
            }

            // Draw Shockwaves first
            paint.setXfermode(addMode);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            for (int i = shockwaves.size() - 1; i >= 0; i--) {
                Shockwave s = shockwaves.get(i);
                if (!s.update(dt)) { shockwaves.remove(i); continue; }
                paint.setColor(s.color);
                paint.setAlpha((int)(s.alpha * 150));
                paint.setStrokeWidth(10 * getResources().getDisplayMetrics().density * s.alpha);
                canvas.drawCircle(s.x, s.y, s.radius, paint);
            }

            paint.setStyle(android.graphics.Paint.Style.FILL);
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                if (!p.update(dt)) { particles.remove(i); continue; }
                
                float r = (p.size / 2f) * p.scale;
                paint.setXfermode(addMode);
                
                // Draw Tapered Motion Trail
                if (!p.isDebris && !"bubbles".equals(p.type)) {
                    tempPath.reset();
                    tempPath.moveTo(p.x, p.y);
                    paint.setStrokeWidth(r * 0.8f);
                    paint.setColor(p.color);
                    paint.setAlpha((int)(p.alpha * 120));
                    canvas.drawLine(p.x, p.y, p.historyX[3], p.historyY[3], paint);
                }

                // Core & Bloom
                paint.setColor(p.isDebris ? Color.WHITE : p.color);
                paint.setAlpha((int) (p.alpha * 50));
                drawShape(canvas, p, r * 2.5f, paint); // Bloom
                
                paint.setAlpha((int) (p.alpha * 255));
                drawShape(canvas, p, r, paint); // Core

                // Electric Arcs
                if ("electric".equals(p.type) && Math.random() > 0.88 && i > 2) {
                    Particle p2 = particles.get(i-1);
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(3);
                    canvas.drawLine(p.x, p.y, (p.x+p2.x)/2 + (float)(Math.random()-0.5)*30, (p.y+p2.y)/2 + (float)(Math.random()-0.5)*30, paint);
                    canvas.drawLine((p.x+p2.x)/2 + (float)(Math.random()-0.5)*30, (p.y+p2.y)/2 + (float)(Math.random()-0.5)*30, p2.x, p2.y, paint);
                }
            }
            paint.setXfermode(null);
            if (!particles.isEmpty() || !shockwaves.isEmpty()) postInvalidateOnAnimation();
        }

        private void drawShape(android.graphics.Canvas canvas, Particle p, float r, android.graphics.Paint paint) {
            canvas.save();
            canvas.translate(p.x, p.y);
            canvas.rotate(p.rotation);
            if (p.shapeType == 1) { // Square (Retro)
                canvas.drawRect(-r, -r, r, r, paint);
            } else if (p.shapeType == 2) { // Diamond (Sparkle)
                tempPath.reset();
                tempPath.moveTo(0, -r * 1.6f);
                tempPath.lineTo(r, 0);
                tempPath.lineTo(0, r * 1.6f);
                tempPath.lineTo(-r, 0);
                tempPath.close();
                canvas.drawPath(tempPath, paint);
            } else if (p.shapeType == 3) { // Hexagon (Frozen)
                tempPath.reset();
                for (int i = 0; i < 6; i++) {
                    float angle = (float) (i * Math.PI / 3);
                    float px = (float) (Math.cos(angle) * r);
                    float py = (float) (Math.sin(angle) * r);
                    if (i == 0) tempPath.moveTo(px, py);
                    else tempPath.lineTo(px, py);
                }
                tempPath.close();
                canvas.drawPath(tempPath, paint);
            } else if (p.shapeType == 4) { // Shard (Emerald)
                tempPath.reset();
                tempPath.moveTo(0, -r * 1.8f);
                tempPath.lineTo(r * 0.6f, r * 0.8f);
                tempPath.lineTo(-r * 0.6f, r * 0.8f);
                tempPath.close();
                canvas.drawPath(tempPath, paint);
            } else { // Circle (Default)
                canvas.drawCircle(0, 0, r, paint);
            }
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
        tvCurrentRank = findViewById(R.id.tv_game_current_rank);
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

                // Get exact duration from audio file before generating beatmap
                int duration = audioService.prepareSong(songData.getResName());
                if (duration <= 0) duration = 180000; // Fallback
                
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
        
        // Visual feedback for combo
        if (tvScore != null) {
            tvScore.animate().scaleX(1.1f).scaleY(1.1f).setDuration(50)
                .withEndAction(() -> tvScore.animate().scaleX(1f).scaleY(1f).setDuration(50).start()).start();
        }
        
        if (gameManager.getCurrentCombo() > 0) {
            tvSongName.setText("COMBO: " + gameManager.getCurrentCombo());
        }

        updateAccuracy();
        updateHpUI();
        if (points >= 300) vibrate(40);
        else if (points >= 100) vibrate(20);
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
        tvSongName.setText(songData != null ? songData.getName() : "");
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
        double acc = gameManager.getAccuracy();
        tvAccuracy.setText(String.format(Locale.US, "Acc: %.1f%%", acc * 100));
        
        if (tvCurrentRank != null) {
            String rank = gameManager.getRank();
            tvCurrentRank.setText("Rank: " + rank);
            
            // Dynamic Rank Colors
            if (rank.equals("S")) tvCurrentRank.setTextColor(Color.parseColor("#FFEB3B")); // Gold
            else if (rank.equals("A")) tvCurrentRank.setTextColor(Color.parseColor("#4CAF50")); // Green
            else if (rank.equals("B")) tvCurrentRank.setTextColor(Color.parseColor("#2196F3")); // Blue
            else if (rank.equals("C")) tvCurrentRank.setTextColor(Color.parseColor("#FF9800")); // Orange
            else tvCurrentRank.setTextColor(Color.parseColor("#FF5252")); // Red
        }
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
        int score = !isDead ? gameManager.getCurrentScore() : 0;
        int earned = gameManager.calculateEarnedPoints();
        updateScores(score, earned);

        User currentUser = authService.getCurrentUser();
        if (!isDead && songData != null && currentUser != null) {
            String songId = getIntent().getStringExtra("SONG_ID");
            boolean updated = false;

            // 1. Update Personal Best (PB)
            int currentPB = currentUser.songHighScores != null && currentUser.songHighScores.containsKey(songData.getName()) 
                ? currentUser.songHighScores.get(songData.getName()) : 0;
            
            if (score > currentPB) {
                if (currentUser.songHighScores == null) currentUser.songHighScores = new java.util.HashMap<>();
                if (currentUser.songRanks == null) currentUser.songRanks = new java.util.HashMap<>();
                if (currentUser.songAccuracies == null) currentUser.songAccuracies = new java.util.HashMap<>();
                
                currentUser.songHighScores.put(songData.getName(), score);
                currentUser.songRanks.put(songData.getName(), gameManager.getRank());
                currentUser.songAccuracies.put(songData.getName(), gameManager.getAccuracy());
                
                authService.updateUserAndSync(currentUser, null);
            }

            // 2. Update World Record (WR)
            if (score > songData.getTopScore()) {
                songData.setTopScore(score);
                songData.setTopAccuracy(gameManager.getAccuracy());
                songData.setTopRank(gameManager.getRank());
                songData.setTopPlayerName(currentUser.username);
                if (songId != null) {
                    databaseService.updateSong(songId, songData, null);
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(isDead ? "Game Over!" : "Level Complete!")
                .setMessage(String.format(Locale.US, "Rank: %s\nScore: %d\nAccuracy: %.1f%%\nMax Combo: %d\nPoints Earned: %d\n%s",
                        gameManager.getRank(), gameManager.getCurrentScore(), gameManager.getAccuracy(), 
                        gameManager.getMaxCombo(), earned,
                        isDead ? "(You failed, earned partial points)" : "(Survival Bonus Included!)"))
                .setCancelable(false)
                .setPositiveButton("Main Menu", (d, i) -> finish()).show();
    }

    private void updateScores(int score, int points) {
        User user = authService.getCurrentUser();
        if (user != null) {
            if (score > 0) user.totalRhythmScore += score;
            user.points += points;
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
