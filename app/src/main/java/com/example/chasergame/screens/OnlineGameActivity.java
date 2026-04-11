package com.example.chasergame.screens;

import android.app.AlertDialog;
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

    private TextView tvTurnInfo, tvTimer, tvScore, tvQuestion;
    private Button btnA, btnB, btnC;

    private OnlineGameService gameService;
    private CountDownTimer turnTimer, cooldownTimer;

    /**
     * Called when the activity is first created.
     * Initializes the player ID, room ID, and sets up the UI and game service.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
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
    }

    /**
     * Initializes the UI components and sets up click listeners for answer buttons and game exit.
     */
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

        findViewById(R.id.btnExitGame).setOnClickListener(v -> deleteAndExit());
    }

    /**
     * Initializes the game service, determines if the player is the host (Player 1),
     * and starts listening for game state updates.
     *
     * @param roomId The ID of the room to join.
     */
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

                        if (isPlayer1 && !snap.hasChild("game")) {
                            gameService.initializeGame(defaultTurnMs, questionService);
                        }

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
                                Toast.makeText(OnlineGameActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    /**
     * Handles updates to the game state received from the database.
     * Updates scores, turn information, question text, and synchronizes the turn timer.
     *
     * @param snap The data snapshot of the current game state.
     */
    private void handleStateChange(DataSnapshot snap) {
        if (finishing) return;

        // Check if game is over (forfeit or natural end)
        Boolean isOver = snap.child("gameOver").getValue(Boolean.class);
        if (isOver != null && isOver) {
            String winner = snap.child("winner").getValue(String.class);
            showGameOverDialog(winner);
            return;
        }

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

        long serverNow = gameService.getServerTime();
        Long startAt = snap.child("turnStartedAt").getValue(Long.class);

        // בדיקה כפולה: גם שהערך קיים ב-DB וגם שהאפליקציה הספיקה לסנכרן זמן מול השרת
        if (startAt != null && OnlineGameService.isTimeSynced()) {
            long duration = getLong(snap.child("turnDurationMs"));
            long remaining = Math.max(0, startAt + (duration == 0 ? defaultTurnMs : duration) - serverNow);

            // אם נשאר זמן, מתחילים טיימר. אם הזמן באמת נגמר (והסנכרון תקין), הטיימר יסיים את התור
            startLocalTimer(remaining);
            setButtonsEnabled(isMyTurn && !answerLocked && !inCooldown);
        } else {
            // בזמן שמחכים לסנכרון או לנתונים, מציגים מצב המתנה
            tvTimer.setText("--:--");
            setButtonsEnabled(false);
        }
    }

    /**
     * Called when an answer button is clicked. Validates the answer and updates the game.
     *
     * @param key        The key of the selected answer ("A", "B", or "C").
     * @param clickedBtn The button that was clicked.
     */
    private void onAnswerClicked(String key, Button clickedBtn) {
        if (!isMyTurn || answerLocked || inCooldown) return;
        answerLocked = true;
        setButtonsEnabled(false);

        gameService.validateAnswer(key, (isCorrect, correct) -> {
            showAnswerColors(key, correct, clickedBtn);
            if (isCorrect) {
                gameService.submitAnswer(isPlayer1 ? "p1" : "p2", questionService, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void unused) {
                    }

                    @Override
                    public void onFailed(Exception e) {
                        answerLocked = false;
                        setButtonsEnabled(true);
                    }
                });
            } else {
                startWrongCooldown();
            }
        });
    }

    /**
     * Starts a cooldown period after a wrong answer, during which the player cannot answer.
     */
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

    /**
     * Starts the local countdown timer for the current turn.
     *
     * @param ms The remaining time for the turn in milliseconds.
     */
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

    /**
     * Displays a dialog when the game is over, showing the result and providing an exit option.
     *
     * @param w The winner of the game.
     */
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
                    if (isPlayer1) gameService.deleteRoom();
                    goHome();
                })
                .show();
    }

    /**
     * Enables or disables all answer buttons.
     *
     * @param e True to enable, false to disable.
     */
    private void setButtonsEnabled(boolean e) {
        btnA.setEnabled(e);
        btnB.setEnabled(e);
        btnC.setEnabled(e);
    }

    /**
     * Resets the background color of all answer buttons to the default.
     */
    private void resetButtonColors() {
        ColorStateList n = ColorStateList.valueOf(Color.parseColor("#333333"));
        btnA.setBackgroundTintList(n);
        btnB.setBackgroundTintList(n);
        btnC.setBackgroundTintList(n);
    }

    /**
     * Highlights the clicked button based on whether the answer was correct or wrong.
     *
     * @param ch The chosen answer key.
     * @param co The correct answer key.
     * @param b  The button that was clicked.
     */
    private void showAnswerColors(String ch, String co, Button b) {
        resetButtonColors();
        b.setBackgroundTintList(ColorStateList.valueOf(ch.equals(co) ? Color.GREEN : Color.RED));
    }

    /**
     * Safely retrieves a long value from a DataSnapshot.
     *
     * @param s The DataSnapshot containing the value.
     * @return The long value or 0 if invalid.
     */
    private long getLong(DataSnapshot s) {
        Object v = s.getValue();
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    /**
     * Deletes the room if the user is the host and navigates back to the home screen.
     */
    private void deleteAndExit() {
        if (finishing) return;

        new AlertDialog.Builder(this)
                .setTitle("Exit Game")
                .setMessage("Are you sure you want to quit? This will be counted as a loss.")
                .setPositiveButton("Exit", (dialog, which) -> {
                    finishing = true;
                    gameService.forfeitGame(isPlayer1 ? "p1" : "p2", new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void unused) {
                            gameService.stopListening();
                            goHome();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            goHome();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Navigates the user back to the MainActivity.
     */
    private void goHome() {
        navigateTo(MainActivity.class, true);
    }

    /**
     * Cleans up listeners and timers when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameService != null) gameService.stopListening();
        if (turnTimer != null) turnTimer.cancel();
        if (cooldownTimer != null) cooldownTimer.cancel();
    }
}
