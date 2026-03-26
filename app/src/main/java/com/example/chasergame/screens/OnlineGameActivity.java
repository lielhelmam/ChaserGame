package com.example.chasergame.screens;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.services.OnlineGameService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OnlineGameActivity extends BaseActivity {

    private static final String TAG = "OnlineGameActivity";
    private static final long WRONG_COOLDOWN_MS = 2_000L;

    private String playerId;
    private boolean isPlayer1 = false;
    private boolean isMyTurn = false;
    private boolean answerLocked = false;
    private boolean finishing = false;
    private long defaultTurnMs = 60_000L;
    private Long lastQuestionNonceSeen = null;
    private boolean inCooldown = false;
    private long serverTimeOffsetMs = 0L;

    private TextView tvTurnInfo, tvTimer, tvScore, tvQuestion;
    private Button btnA, btnB, btnC;

    private OnlineGameService gameService;
    private CountDownTimer turnTimer, cooldownTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        playerId = getIntent().getStringExtra("PLAYER_ID");
        String roomId = getIntent().getStringExtra("ROOM_ID");

        if (roomId == null || playerId == null) {
            finish();
            return;
        }

        initUI();
        initGame(roomId);
        listenToServerTime();
    }

    private void initUI() {
        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        tvTimer = findViewById(R.id.tvTimer);
        tvScore = findViewById(R.id.tvScore);
        tvQuestion = findViewById(R.id.tvQuestion);
        btnA = findViewById(R.id.btnAnswerA);
        btnB = findViewById(R.id.btnAnswerB);
        btnC = findViewById(R.id.btnAnswerC);

        btnA.setOnClickListener(v -> onAnswerClicked("A", btnA));
        btnB.setOnClickListener(v -> onAnswerClicked("B", btnB));
        btnC.setOnClickListener(v -> onAnswerClicked("C", btnC));
        findViewById(R.id.btnEndGame).setOnClickListener(v -> deleteAndExit());
    }

    private void initGame(String roomId) {
        gameService = new OnlineGameService(roomId, playerId);
        FirebaseDatabase.getInstance().getReference("rooms").child(roomId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!snap.exists()) {
                            goHome();
                            return;
                        }
                        isPlayer1 = playerId.equals(snap.child("hostId").getValue(String.class));
                        Long t = snap.child("timeMs").getValue(Long.class);
                        if (t != null) defaultTurnMs = t;

                        gameService.startListening(new OnlineGameService.GameStateListener() {
                            @Override
                            public void onGameStateChanged(DataSnapshot snapshot) {
                                handleStateChange(snapshot);
                            }

                            @Override
                            public void onGameOver(String winner) {
                                showGameOverDialog(winner);
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(OnlineGameActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void handleStateChange(DataSnapshot snap) {
        long p1 = getLong(snap.child("p1Score"));
        long p2 = getLong(snap.child("p2Score"));
        tvScore.setText("P1: " + p1 + " | P2: " + p2);

        String turn = snap.child("currentTurn").getValue(String.class);
        isMyTurn = (isPlayer1 && "p1".equals(turn)) || (!isPlayer1 && "p2".equals(turn));
        tvTurnInfo.setText(isMyTurn ? "Your turn" : "Opponent's turn");

        if (snap.hasChild("question")) {
            DataSnapshot q = snap.child("question");
            tvQuestion.setText(q.child("text").getValue(String.class));
            btnA.setText(q.child("a").getValue(String.class));
            btnB.setText(q.child("b").getValue(String.class));
            btnC.setText(q.child("c").getValue(String.class));

            Long nonce = q.child("nonce").getValue(Long.class);
            if (nonce != null && !nonce.equals(lastQuestionNonceSeen)) {
                lastQuestionNonceSeen = nonce;
                answerLocked = false;
                resetButtonColors();
            }
        }

        long serverNow = System.currentTimeMillis() + serverTimeOffsetMs;
        Long startAt = snap.child("turnStartedAt").getValue(Long.class);
        long duration = getLong(snap.child("turnDurationMs"));
        long remaining = Math.max(0, (startAt == null ? 0 : startAt) + (duration == 0 ? defaultTurnMs : duration) - serverNow);

        startLocalTimer(remaining);
        setButtonsEnabled(isMyTurn && !answerLocked && !inCooldown);
    }

    private void onAnswerClicked(String key, Button clickedBtn) {
        if (!isMyTurn || answerLocked || inCooldown) return;
        answerLocked = true;
        setButtonsEnabled(false);

        FirebaseDatabase.getInstance().getReference("rooms").child(getIntent().getStringExtra("ROOM_ID")).child("game/question/correct")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String correct = s.getValue(String.class);
                        boolean ok = key.equals(correct);
                        showAnswerColors(key, correct, clickedBtn);
                        if (ok)
                            gameService.submitAnswer(isPlayer1 ? "p1" : "p2", true, questionService, new DatabaseService.DatabaseCallback<Void>() {
                                @Override
                                public void onCompleted(Void unused) {
                                }

                                @Override
                                public void onFailed(Exception e) {
                                    answerLocked = false;
                                }
                            });
                        else startWrongCooldown();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        answerLocked = false;
                    }
                });
    }

    private void startWrongCooldown() {
        inCooldown = true;
        tvTurnInfo.setText("⏳ Cooldown...");
        cooldownTimer = new CountDownTimer(WRONG_COOLDOWN_MS, 1000) {
            @Override
            public void onTick(long ms) {
                tvTimer.setText("⏳ " + (ms / 1000 + 1));
            }

            @Override
            public void onFinish() {
                inCooldown = false;
                answerLocked = false;
                if (isMyTurn) gameService.setNewQuestion(questionService);
            }
        }.start();
    }

    private void startLocalTimer(long ms) {
        if (turnTimer != null) turnTimer.cancel();
        turnTimer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long r) {
                if (!inCooldown)
                    tvTimer.setText(String.format("%02d:%02d", r / 60000, (r / 1000) % 60));
            }

            @Override
            public void onFinish() {
                if (isMyTurn) gameService.endTurn(defaultTurnMs);
            }
        }.start();
    }

    private void showGameOverDialog(String w) {
        if (finishing) return;
        finishing = true;
        boolean win = (isPlayer1 && "P1".equals(w)) || (!isPlayer1 && "P2".equals(w));
        if (win)
            databaseService.updateUserWins(authService.getCurrentUser().getId(), "onlineWins", null);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage(win ? "🏆 You Win!" : "DRAW".equals(w) ? "🤝 Draw" : "😢 You Lose")
                .setCancelable(false)
                .setPositiveButton("OK", (d, i) -> {
                    gameService.deleteRoom();
                    goHome();
                })
                .show();
    }

    private void listenToServerTime() {
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                serverTimeOffsetMs = s.getValue(Long.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });
    }

    private void setButtonsEnabled(boolean e) {
        btnA.setEnabled(e);
        btnB.setEnabled(e);
        btnC.setEnabled(e);
    }

    private void resetButtonColors() {
        ColorStateList n = ColorStateList.valueOf(Color.parseColor("#333333"));
        btnA.setBackgroundTintList(n);
        btnB.setBackgroundTintList(n);
        btnC.setBackgroundTintList(n);
    }

    private void showAnswerColors(String ch, String co, Button b) {
        resetButtonColors();
        b.setBackgroundTintList(ColorStateList.valueOf(ch.equals(co) ? Color.GREEN : Color.RED));
    }

    private long getLong(DataSnapshot s) {
        Object v = s.getValue();
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private void deleteAndExit() {
        finishing = true;
        gameService.deleteRoom();
        goHome();
    }

    private void goHome() {
        navigateTo(MainActivity.class, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameService != null) gameService.stopListening();
        if (turnTimer != null) turnTimer.cancel();
        if (cooldownTimer != null) cooldownTimer.cancel();
    }
}
