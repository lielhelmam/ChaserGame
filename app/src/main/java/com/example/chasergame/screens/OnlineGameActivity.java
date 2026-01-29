package com.example.chasergame.screens;

import android.content.Intent;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OnlineGameActivity extends BaseActivity {

    // -------- STATE --------
    private String roomId;
    private String playerId;
    private boolean isPlayer1 = false;   // host
    private boolean isMyTurn = false;
    private boolean answerLocked = false;
    private boolean finishing = false;

    private long defaultTurnMs = 60_000L;

    // Track changes so we can unlock locally
    private String lastTurnSeen = null;
    private Long lastQuestionNonceSeen = null;

    // -------- UI --------
    private TextView tvTurnInfo, tvTimer, tvScore, tvQuestion;
    private Button btnA, btnB, btnC, btnEndGame;

    // -------- FIREBASE --------
    private DatabaseReference roomRef;
    private DatabaseReference gameRef;
    private ValueEventListener gameListener;

    private CountDownTimer turnTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        roomId = getIntent().getStringExtra("ROOM_ID");
        playerId = getIntent().getStringExtra("PLAYER_ID");

        if (roomId == null || playerId == null) {
            Toast.makeText(this, "Missing room data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        tvTimer = findViewById(R.id.tvTimer);
        tvScore = findViewById(R.id.tvScore);
        tvQuestion = findViewById(R.id.tvQuestion);

        btnA = findViewById(R.id.btnAnswerA);
        btnB = findViewById(R.id.btnAnswerB);
        btnC = findViewById(R.id.btnAnswerC);
        btnEndGame = findViewById(R.id.btnEndGame);

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);
        gameRef = roomRef.child("game");

        btnA.setOnClickListener(v -> onAnswerClicked("A", btnA));
        btnB.setOnClickListener(v -> onAnswerClicked("B", btnB));
        btnC.setOnClickListener(v -> onAnswerClicked("C", btnC));
        btnEndGame.setOnClickListener(v -> deleteRoomAndExit());

        initRoom();
    }

    // =========================
    // 1) INIT ROOM + CREATE GAME
    // =========================
    private void initRoom() {
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    Toast.makeText(OnlineGameActivity.this, "Room not found", Toast.LENGTH_SHORT).show();
                    goHome();
                    return;
                }

                String hostId = snap.child("hostId").getValue(String.class);
                isPlayer1 = playerId.equals(hostId);

                Long timeMs = snap.child("timeMs").getValue(Long.class);
                if (timeMs != null && timeMs > 0) {
                    defaultTurnMs = timeMs;
                }

                // IMPORTANT: רק ה-host מוחק חדר על disconnect (אחרת כל ניתוק של אורח מוחק הכל)
                if (isPlayer1) {
                    roomRef.onDisconnect().removeValue();
                }

                // רק ה־host ייצור משחק אם אין עדיין
                if (!snap.hasChild("game") && isPlayer1) {
                    createInitialGame();
                }

                startGameListener();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void createInitialGame() {
        long now = System.currentTimeMillis();

        Map<String, Object> base = new HashMap<>();
        base.put("p1Score", 0);
        base.put("p2Score", 0);
        base.put("currentTurn", "p1");
        base.put("timeMs", defaultTurnMs);
        base.put("turnEndAt", now + defaultTurnMs);

        gameRef.setValue(base);

        // שאלה ראשונה לתור של P1
        setNewQuestionInGame();
    }

    // =========================
    // 2) LOAD RANDOM QUESTION
    // =========================
    private void setNewQuestionInGame() {
        DatabaseReference qRef = FirebaseDatabase.getInstance().getReference("questions");

        qRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {

                long count = snap.getChildrenCount();
                if (count == 0) {
                    tvQuestion.setText("No questions in DB");
                    return;
                }

                int index = (int) (Math.random() * count);
                DataSnapshot qSnap = snap.child(String.valueOf(index));

                String q = qSnap.child("question").getValue(String.class);
                String right = qSnap.child("rightAnswer").getValue(String.class);

                ArrayList<String> wrong = new ArrayList<>();
                for (DataSnapshot w : qSnap.child("wrongAnswers").getChildren()) {
                    String val = w.getValue(String.class);
                    if (val != null) wrong.add(val);
                }

                if (q == null || right == null || wrong.size() < 2) {
                    tvQuestion.setText("Bad question data");
                    return;
                }

                ArrayList<String> all = new ArrayList<>();
                all.add(right);
                all.addAll(wrong);
                Collections.shuffle(all);

                int correctIdx = all.indexOf(right);
                String correctKey = correctIdx == 0 ? "A" : (correctIdx == 1 ? "B" : "C");

                long nonce = System.currentTimeMillis(); // identify "new question" across devices

                Map<String, Object> qMap = new HashMap<>();
                qMap.put("nonce", nonce);
                qMap.put("text", q);
                qMap.put("a", all.get(0));
                qMap.put("b", all.get(1));
                qMap.put("c", all.get(2));
                qMap.put("correct", correctKey);

                gameRef.child("question").setValue(qMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // =========================
    // 3) GAME LISTENER – UI SYNC
    // =========================
    private void startGameListener() {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {

                if (snap.child("gameOver").getValue(Boolean.class) != null &&
                        snap.child("gameOver").getValue(Boolean.class)) {

                    String winner = snap.child("winner").getValue(String.class);
                    showGameOverDialog(winner);
                    return;
                }


                if (!snap.exists()) {
                    if (finishing) return;

                    Toast.makeText(OnlineGameActivity.this, "Game finished", Toast.LENGTH_SHORT).show();
                    goHome();
                    return;
                }

                // ניקוד
                long p1 = getLong(snap.child("p1Score"), 0);
                long p2 = getLong(snap.child("p2Score"), 0);
                tvScore.setText("P1: " + p1 + " | P2: " + p2);

                // מי בתור
                String turn = snap.child("currentTurn").getValue(String.class);
                if (turn == null) turn = "p1";

                boolean newTurn = (lastTurnSeen == null) || !turn.equals(lastTurnSeen);
                lastTurnSeen = turn;

                isMyTurn = (isPlayer1 && turn.equals("p1")) || (!isPlayer1 && turn.equals("p2"));

                tvTurnInfo.setText(isMyTurn ? "Your turn" : "Opponent's turn");

                // שאלה
                if (snap.hasChild("question")) {
                    loadQuestionFromSnapshot(snap);

                    Long nonce = snap.child("question").child("nonce").getValue(Long.class);
                    boolean newQuestion = (nonce != null) && (lastQuestionNonceSeen == null || !nonce.equals(lastQuestionNonceSeen));
                    if (newQuestion) {
                        lastQuestionNonceSeen = nonce;
                        answerLocked = false;     // IMPORTANT: unlock on new question
                        resetButtonColors();
                    }
                } else {
                    tvQuestion.setText("Loading question...");
                }

                // אם התור התחלף לתור שלי – נפתח נעילה
                if (newTurn && isMyTurn) {
                    answerLocked = false;
                    resetButtonColors();
                }
                // אם לא התור שלי – אין סיבה שאוכל ללחוץ
                if (!isMyTurn) {
                    answerLocked = true;
                }

                // טיימר
                Long endAt = snap.child("turnEndAt").getValue(Long.class);
                if (endAt != null) {
                    long now = System.currentTimeMillis();
                    long remain = endAt - now;
                    if (remain < 0) remain = 0;
                    startLocalTimer(remain);
                } else {
                    tvTimer.setText("--:--");
                }

                setButtonsEnabled(isMyTurn && !answerLocked);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        gameRef.addValueEventListener(gameListener);
    }

    private void loadQuestionFromSnapshot(DataSnapshot snap) {
        String text = snap.child("question").child("text").getValue(String.class);
        String a = snap.child("question").child("a").getValue(String.class);
        String b = snap.child("question").child("b").getValue(String.class);
        String c = snap.child("question").child("c").getValue(String.class);

        tvQuestion.setText(text == null ? "" : text);
        btnA.setText(a == null ? "" : a);
        btnB.setText(b == null ? "" : b);
        btnC.setText(c == null ? "" : c);
    }

    // =========================
    // 4) ANSWER CLICK
    // =========================
    private void onAnswerClicked(String key, Button clickedBtn) {
        if (!isMyTurn || answerLocked || finishing) return;

        answerLocked = true;
        setButtonsEnabled(false);

        final String playerKey = isPlayer1 ? "p1" : "p2";

        gameRef.child("question").child("correct")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String correct = s.getValue(String.class);
                        if (correct == null) correct = "A";

                        boolean isCorrect = key.equals(correct);

                        showAnswerColors(key, correct, clickedBtn);

                        if (isCorrect) {
                            incrementScore(playerKey);
                        }

                        // שאלה חדשה – רק מי שבתור מייצר, והיא תפתח את הנעילה דרך nonce בליסנר
                        if (isMyTurn) {
                            setNewQuestionInGame();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void incrementScore(String playerKey) {
        gameRef.child(playerKey + "Score")
                .runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Long v = currentData.getValue(Long.class);
                        if (v == null) v = 0L;
                        currentData.setValue(v + 1);
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                    }
                });
    }

    // =========================
    // 5) TIMER – END TURN
    // =========================
    private void startLocalTimer(long ms) {
        if (turnTimer != null) {
            turnTimer.cancel();
        }

        turnTimer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000;
                tvTimer.setText(String.format("%02d:%02d", sec / 60, sec % 60));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                answerLocked = true;
                setButtonsEnabled(false);

                if (!finishing && isMyTurn) {
                    endTurnInDatabase();
                }
            }
        };
        turnTimer.start();
    }

    private void endTurnInDatabase() {
        gameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {

                String turn = snap.child("currentTurn").getValue(String.class);
                if (turn == null) turn = "p1";

                if (turn.equals("p1")) {
                    long newEnd = System.currentTimeMillis() + defaultTurnMs;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("currentTurn", "p2");
                    updates.put("turnEndAt", newEnd);

                    gameRef.updateChildren(updates).addOnCompleteListener(t -> {
                        // שאלה ראשונה לתור של P2
                        setNewQuestionInGame();
                    });

                } else {
                    long p1 = getLong(snap.child("p1Score"), 0);
                    long p2 = getLong(snap.child("p2Score"), 0);

                    String message;
                    if (p1 > p2) {
                        message = "🏆 Player 1 Wins!";
                    } else if (p2 > p1) {
                        message = "🏆 Player 2 Wins!";
                    } else {
                        message = "🤝 It's a Draw!";
                    }

                    showGameOverDialog(message);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // =========================
    // 6) EXIT / CLEANUP
    // =========================
    private void deleteRoomAndExit() {
        if (finishing) return;
        finishing = true;

        if (turnTimer != null) {
            turnTimer.cancel();
        }

        roomRef.removeValue().addOnCompleteListener(t -> goHome());
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (turnTimer != null) turnTimer.cancel();
        if (gameListener != null && gameRef != null) {
            gameRef.removeEventListener(gameListener);
        }
    }

    // =========================
    // HELPERS
    // =========================
    private void setButtonsEnabled(boolean enabled) {
        btnA.setEnabled(enabled);
        btnB.setEnabled(enabled);
        btnC.setEnabled(enabled);
    }

    private long getLong(DataSnapshot snap, long def) {
        Long v = snap.getValue(Long.class);
        return v == null ? def : v;
    }

    private void resetButtonColors() {
        int normal = Color.parseColor("#333333");
        ColorStateList list = ColorStateList.valueOf(normal);
        btnA.setBackgroundTintList(list);
        btnB.setBackgroundTintList(list);
        btnC.setBackgroundTintList(list);
    }

    private void showAnswerColors(String chosen, String correct, Button clicked) {
        resetButtonColors();

        Button correctBtn =
                "A".equals(correct) ? btnA :
                        "B".equals(correct) ? btnB : btnC;

        if (chosen.equals(correct)) {
            clicked.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        } else {
            clicked.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            correctBtn.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    private void showGameOverDialog(String winner) {
        if (finishing) return;
        finishing = true;

        if (turnTimer != null) turnTimer.cancel();

        String message;
        if ("DRAW".equals(winner)) {
            message = "🤝 It's a Draw!";
        } else if ("P1".equals(winner)) {
            message = isPlayer1 ? "🏆 You Win!" : "😢 You Lose";
        } else {
            message = isPlayer1 ? "😢 You Lose" : "🏆 You Win!";
        }

        runOnUiThread(() -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Game Over")
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, w) -> {
                        roomRef.removeValue(); // כמו שרצית
                        goHome();
                    })
                    .show();
        });
    }

}
