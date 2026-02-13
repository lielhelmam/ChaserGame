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

    // ================= STATE =================
    private String roomId;
    private String playerId;
    private boolean isPlayer1 = false;
    private boolean isMyTurn = false;
    private boolean answerLocked = false;
    private boolean finishing = false;

    private static final long DEFAULT_TURN_MS = 60_000;
    private static final long WRONG_COOLDOWN_MS = 2000;

    private long defaultTurnMs = DEFAULT_TURN_MS;

    private String lastTurnSeen = null;
    private Long lastQuestionNonceSeen = null;

    private boolean inCooldown = false;

    // ================= UI =================
    private TextView tvTurnInfo, tvTimer, tvScore, tvQuestion;
    private Button btnA, btnB, btnC, btnEndGame;

    // ================= FIREBASE =================
    private DatabaseReference roomRef;
    private DatabaseReference gameRef;
    private ValueEventListener gameListener;

    private CountDownTimer turnTimer;
    private CountDownTimer cooldownTimer;

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

    // ================= INIT =================
    private void initRoom() {
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    goHome();
                    return;
                }

                String hostId = snap.child("hostId").getValue(String.class);
                isPlayer1 = playerId.equals(hostId);

                Long t = snap.child("timeMs").getValue(Long.class);
                if (t != null && t > 0) defaultTurnMs = t;

                if (isPlayer1) {
                    roomRef.onDisconnect().removeValue();
                }

                if (!snap.hasChild("game") && isPlayer1) {
                    createInitialGame();
                }

                startGameListener();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createInitialGame() {
        long now = System.currentTimeMillis();

        Map<String, Object> base = new HashMap<>();
        base.put("p1Score", 0);
        base.put("p2Score", 0);
        base.put("currentTurn", "p1");
        base.put("turnEndAt", now + defaultTurnMs);
        base.put("gameOver", false);
        base.put("winner", "");

        gameRef.setValue(base);
        setNewQuestionInGame();
    }

    // ================= QUESTIONS =================
    private void setNewQuestionInGame() {
        DatabaseReference qRef = FirebaseDatabase.getInstance().getReference("questions");

        qRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) return;

                int index = (int) (Math.random() * snap.getChildrenCount());
                int i = 0;
                DataSnapshot qSnap = null;

                for (DataSnapshot c : snap.getChildren()) {
                    if (i++ == index) {
                        qSnap = c;
                        break;
                    }
                }

                if (qSnap == null) return;

                String q = qSnap.child("question").getValue(String.class);
                String right = qSnap.child("rightAnswer").getValue(String.class);

                ArrayList<String> wrong = new ArrayList<>();
                for (DataSnapshot w : qSnap.child("wrongAnswers").getChildren()) {
                    wrong.add(w.getValue(String.class));
                }

                if (q == null || right == null || wrong.size() < 2) return;

                ArrayList<String> all = new ArrayList<>();
                all.add(right);
                all.addAll(wrong);
                Collections.shuffle(all);

                int correctIdx = all.indexOf(right);
                String correctKey = correctIdx == 0 ? "A" : correctIdx == 1 ? "B" : "C";

                Map<String, Object> qm = new HashMap<>();
                qm.put("nonce", System.currentTimeMillis());
                qm.put("text", q);
                qm.put("a", all.get(0));
                qm.put("b", all.get(1));
                qm.put("c", all.get(2));
                qm.put("correct", correctKey);

                gameRef.child("question").setValue(qm);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ================= LISTENER =================
    private void startGameListener() {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {

                Boolean over = snap.child("gameOver").getValue(Boolean.class);
                if (over != null && over) {
                    showGameOverDialog(snap.child("winner").getValue(String.class));
                    return;
                }

                long p1 = getLong(snap.child("p1Score"), 0);
                long p2 = getLong(snap.child("p2Score"), 0);
                tvScore.setText("P1: " + p1 + " | P2: " + p2);

                String turn = snap.child("currentTurn").getValue(String.class);
                if (turn == null) turn = "p1";

                boolean newTurn = !turn.equals(lastTurnSeen);
                lastTurnSeen = turn;

                isMyTurn = (isPlayer1 && turn.equals("p1")) ||
                        (!isPlayer1 && turn.equals("p2"));

                tvTurnInfo.setText(isMyTurn ? "Your turn" : "Opponent's turn");

                if (snap.hasChild("question")) {
                    loadQuestionFromSnapshot(snap);

                    Long nonce = snap.child("question").child("nonce").getValue(Long.class);
                    if (nonce != null && !nonce.equals(lastQuestionNonceSeen)) {
                        lastQuestionNonceSeen = nonce;
                        answerLocked = false;
                        resetButtonColors();
                    }
                }

                if (newTurn && isMyTurn) {
                    answerLocked = false;
                    inCooldown = false;
                    resetButtonColors();
                }

                Long endAt = snap.child("turnEndAt").getValue(Long.class);
                if (endAt != null) {
                    startLocalTimer(Math.max(0, endAt - System.currentTimeMillis()));
                }

                setButtonsEnabled(isMyTurn && !answerLocked && !inCooldown);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        gameRef.addValueEventListener(gameListener);
    }

    private void loadQuestionFromSnapshot(DataSnapshot snap) {
        tvQuestion.setText(snap.child("question/text").getValue(String.class));
        btnA.setText(snap.child("question/a").getValue(String.class));
        btnB.setText(snap.child("question/b").getValue(String.class));
        btnC.setText(snap.child("question/c").getValue(String.class));
    }

    // ================= ANSWERS =================
    private void onAnswerClicked(String key, Button clickedBtn) {
        if (!isMyTurn || answerLocked || inCooldown || finishing) return;

        answerLocked = true;
        setButtonsEnabled(false);

        gameRef.child("question/correct")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String correct = s.getValue(String.class);
                        boolean ok = key.equals(correct);

                        showAnswerColors(key, correct, clickedBtn);

                        if (ok) {
                            incrementScore(isPlayer1 ? "p1" : "p2");
                            setNewQuestionInGame();
                        } else {
                            startWrongCooldown();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void startWrongCooldown() {
        inCooldown = true;
        tvTurnInfo.setText("⏳ Cooldown...");

        cooldownTimer = new CountDownTimer(WRONG_COOLDOWN_MS, 1000) {
            @Override
            public void onTick(long ms) {
                tvTimer.setText("⏳ " + ((ms / 1000) + 1));
            }

            @Override
            public void onFinish() {
                inCooldown = false;
                if (isMyTurn && !finishing) {
                    setNewQuestionInGame();
                }
            }
        };
        cooldownTimer.start();
    }

    private void incrementScore(String key) {
        gameRef.child(key + "Score").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData d) {
                Long v = d.getValue(Long.class);
                d.setValue((v == null ? 0 : v) + 1);
                return Transaction.success(d);
            }

            @Override
            public void onComplete(DatabaseError e, boolean c, DataSnapshot s) {}
        });
    }

    // ================= TIMER =================
    private void startLocalTimer(long ms) {
        if (turnTimer != null) turnTimer.cancel();

        turnTimer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long ms) {
                if (!inCooldown) {
                    long sec = ms / 1000;
                    tvTimer.setText(String.format("%02d:%02d", sec / 60, sec % 60));
                }
            }

            @Override
            public void onFinish() {
                if (isMyTurn) endTurn();
            }
        };
        turnTimer.start();
    }

    private void endTurn() {
        gameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData d) {
                String turn = d.child("currentTurn").getValue(String.class);
                if ("p1".equals(turn)) {
                    d.child("currentTurn").setValue("p2");
                    d.child("turnEndAt").setValue(System.currentTimeMillis() + defaultTurnMs);
                } else {
                    d.child("gameOver").setValue(true);
                    long p1 = getLong(d.child("p1Score"), 0);
                    long p2 = getLong(d.child("p2Score"), 0);
                    d.child("winner").setValue(p1 > p2 ? "P1" : p2 > p1 ? "P2" : "DRAW");
                }
                return Transaction.success(d);
            }

            @Override
            public void onComplete(DatabaseError e, boolean c, DataSnapshot s) {}
        });
    }

    // ================= UI HELPERS =================
    private void setButtonsEnabled(boolean e) {
        btnA.setEnabled(e);
        btnB.setEnabled(e);
        btnC.setEnabled(e);
    }

    private long getLong(DataSnapshot s, long d) {
        Long v = s.getValue(Long.class);
        return v == null ? d : v;
    }

    private long getLong(MutableData d, long def) {
        Long v = d.getValue(Long.class);
        return v == null ? def : v;
    }

    private void resetButtonColors() {
        ColorStateList n = ColorStateList.valueOf(Color.parseColor("#333333"));
        btnA.setBackgroundTintList(n);
        btnB.setBackgroundTintList(n);
        btnC.setBackgroundTintList(n);
    }

    private void showAnswerColors(String chosen, String correct, Button clicked) {
        resetButtonColors();
        Button c = "A".equals(correct) ? btnA : "B".equals(correct) ? btnB : btnC;
        if (chosen.equals(correct)) {
            clicked.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        } else {
            clicked.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            c.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    private void showGameOverDialog(String w) {
        if (finishing) return;
        finishing = true;

        String msg =
                "DRAW".equals(w) ? "🤝 Draw" :
                        ("P1".equals(w) == isPlayer1) ? "🏆 You Win!" : "😢 You Lose";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", (d, i) -> {
                    roomRef.removeValue();
                    goHome();
                })
                .show();
    }

    private void deleteRoomAndExit() {
        finishing = true;
        roomRef.removeValue().addOnCompleteListener(t -> goHome());
    }

    private void goHome() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (turnTimer != null) turnTimer.cancel();
        if (cooldownTimer != null) cooldownTimer.cancel();
        if (gameListener != null) gameRef.removeEventListener(gameListener);
    }
}
