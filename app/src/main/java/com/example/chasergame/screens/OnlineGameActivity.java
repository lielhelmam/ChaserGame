package com.example.chasergame.screens;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.SharedPreferencesUtil;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OnlineGameActivity extends BaseActivity {

    private static final String TAG = "OnlineGameActivity";
    // ================= STATE =================
    private String roomId;
    private String playerId;
    private boolean isPlayer1 = false;
    private boolean isMyTurn = false;
    private boolean answerLocked = false;
    private boolean finishing = false;

    private static final long DEFAULT_TURN_MS = 60_000L;
    private static final long WRONG_COOLDOWN_MS = 2_000L;

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

    // server time offset listener
    private DatabaseReference offsetRef;
    private ValueEventListener offsetListener;
    private long serverTimeOffsetMs = 0L; // serverNow = System.currentTimeMillis() + serverTimeOffsetMs

    private CountDownTimer turnTimer;
    private CountDownTimer cooldownTimer;

    // NOTE: DatabaseService is provided by BaseActivity as protected field:
    // protected DatabaseService databaseService;

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

        // listen to Firebase serverTime offset
        offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object v = snapshot.getValue();
                if (v instanceof Number) {
                    serverTimeOffsetMs = ((Number) v).longValue();
                } else {
                    serverTimeOffsetMs = 0L;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                serverTimeOffsetMs = 0L;
            }
        };
        offsetRef.addValueEventListener(offsetListener);

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
                isPlayer1 = hostId != null && playerId.equals(hostId);

                Long t = snap.child("timeMs").getValue(Long.class);
                if (t != null && t > 0) defaultTurnMs = t;

                if (isPlayer1) {
                    // remove room on disconnect (host)
                    roomRef.onDisconnect().removeValue();
                }

                if (!snap.hasChild("game") && isPlayer1) {
                    createInitialGame();
                }

                startGameListener();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void createInitialGame() {
        // Use server timestamp and duration
        Map<String, Object> base = new HashMap<>();
        base.put("p1Score", 0);
        base.put("p2Score", 0);
        base.put("currentTurn", "p1");

        base.put("turnStartedAt", ServerValue.TIMESTAMP);
        base.put("turnDurationMs", defaultTurnMs);

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
                if (!snap.exists() || snap.getChildrenCount() == 0) {
                    Toast.makeText(OnlineGameActivity.this, "No questions in database.", Toast.LENGTH_SHORT).show();
                    return;
                }

                long count = snap.getChildrenCount();
                long index = (long) (Math.random() * count);

                DataSnapshot qSnap = null;
                long i = 0;
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
                DataSnapshot wrongsNode = qSnap.child("wrongAnswers");
                if (wrongsNode.exists()) {
                    for (DataSnapshot w : wrongsNode.getChildren()) {
                        String val = w.getValue(String.class);
                        if (val != null) wrong.add(val);
                    }
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
            public void onCancelled(@NonNull DatabaseError error) { }
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

                boolean newTurn = lastTurnSeen == null || !turn.equals(lastTurnSeen);
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

                if (newTurn) {
                    // reset state for new turn
                    answerLocked = false;
                    inCooldown = false;
                    resetButtonColors();

                    if (!snap.hasChild("question")) {
                        setNewQuestionInGame();
                    }
                }

                // --- compute remaining time using server time ---
                long serverNow = System.currentTimeMillis() + serverTimeOffsetMs;

                Long startAt = snap.child("turnStartedAt").getValue(Long.class);
                long duration = getLong(snap.child("turnDurationMs"), defaultTurnMs);

                long remaining;
                if (startAt != null) {
                    remaining = (startAt + duration) - serverNow;
                } else {
                    // fallback for older rooms that may still use turnEndAt
                    Long endAt = snap.child("turnEndAt").getValue(Long.class);
                    if (endAt != null) {
                        remaining = endAt - serverNow;
                    } else {
                        remaining = duration;
                    }
                }
                if (remaining < 0) remaining = 0;

                // If remaining is zero and it's our turn, endTurn (server authoritative)
                if (remaining == 0 && isMyTurn && !finishing) {
                    endTurn();
                    startLocalTimer(0);
                } else {
                    startLocalTimer(remaining);
                }
                // -----------------------------------------------------

                setButtonsEnabled(isMyTurn && !answerLocked && !inCooldown);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };

        gameRef.addValueEventListener(gameListener);
    }

    private void loadQuestionFromSnapshot(DataSnapshot snap) {
        DataSnapshot q = snap.child("question");
        if (q != null && q.exists()) {
            String text = q.child("text").getValue(String.class);
            tvQuestion.setText(text != null ? text : "");
            String a = q.child("a").getValue(String.class);
            String b = q.child("b").getValue(String.class);
            String c = q.child("c").getValue(String.class);
            btnA.setText(a != null ? a : "A");
            btnB.setText(b != null ? b : "B");
            btnC.setText(c != null ? c : "C");
        }
    }

    // ================= ANSWERS =================
    private void onAnswerClicked(String key, Button clickedBtn) {
        if (!isMyTurn || answerLocked || inCooldown || finishing) return;

        answerLocked = true;
        setButtonsEnabled(false);

        gameRef.child("question").child("correct")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String correct = s.getValue(String.class);
                        boolean ok = key != null && key.equals(correct);

                        showAnswerColors(key, correct, clickedBtn);

                        if (ok) {
                            incrementScore(isPlayer1 ? "p1" : "p2");
                            setNewQuestionInGame();
                        } else {
                            startWrongCooldown();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        answerLocked = false;
                        setButtonsEnabled(isMyTurn && !inCooldown);
                    }
                });
    }

    private void startWrongCooldown() {
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
            cooldownTimer = null;
        }

        inCooldown = true;
        tvTurnInfo.setText("⏳ Cooldown...");

        cooldownTimer = new CountDownTimer(WRONG_COOLDOWN_MS, 250) {
            @Override
            public void onTick(long ms) {
                long secs = (ms + 999) / 1000;
                tvTimer.setText("⏳ " + secs);
            }

            @Override
            public void onFinish() {
                inCooldown = false;
                answerLocked = false;
                tvTurnInfo.setText(isMyTurn ? "Your turn" : "Opponent's turn");

                if (isMyTurn && !finishing) {
                    setNewQuestionInGame();
                }
                setButtonsEnabled(isMyTurn && !answerLocked && !inCooldown);
            }
        };
        cooldownTimer.start();

        setButtonsEnabled(false);
    }

    private void incrementScore(String key) {
        gameRef.child(key + "Score").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData d) {
                Object raw = d.getValue();
                long current = 0;
                if (raw instanceof Number) {
                    current = ((Number) raw).longValue();
                } else {
                    Long v = d.getValue(Long.class);
                    current = v == null ? 0 : v;
                }
                d.setValue(current + 1);
                return Transaction.success(d);
            }

            @Override
            public void onComplete(DatabaseError e, boolean c, DataSnapshot s) {
                if (e != null) {
                    Log.w(TAG, "incrementScore transaction failed", e.toException());
                }
            }
        });
    }

    // ================= TIMER =================
    private void startLocalTimer(long ms) {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }

        if (ms <= 0) {
            tvTimer.setText("00:00");
            return;
        }

        turnTimer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long msRemaining) {
                if (!inCooldown) {
                    long sec = msRemaining / 1000;
                    tvTimer.setText(String.format("%02d:%02d", sec / 60, sec % 60));
                }
            }

            @Override
            public void onFinish() {
                if (isMyTurn && !finishing) {
                    endTurn();
                }
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
                    // set server timestamp for start of new turn and duration
                    d.child("turnStartedAt").setValue(ServerValue.TIMESTAMP);
                    d.child("turnDurationMs").setValue(defaultTurnMs);
                } else {
                    d.child("gameOver").setValue(true);
                    long p1 = getLong(d.child("p1Score"), 0);
                    long p2 = getLong(d.child("p2Score"), 0);
                    d.child("winner").setValue(p1 > p2 ? "P1" : p2 > p1 ? "P2" : "DRAW");
                }
                return Transaction.success(d);
            }

            @Override
            public void onComplete(DatabaseError e, boolean c, DataSnapshot s) {
                if (e != null) {
                    Log.w(TAG, "endTurn transaction failed", e.toException());
                }
            }
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
        if (v != null) return v;
        Object raw = s.getValue();
        if (raw instanceof Number) return ((Number) raw).longValue();
        return d;
    }

    private long getLong(MutableData d, long def) {
        Object raw = d.getValue();
        if (raw instanceof Number) return ((Number) raw).longValue();
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
        if (chosen != null && chosen.equals(correct)) {
            clicked.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        } else {
            clicked.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            if (c != null) c.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    private void showGameOverDialog(String w) {
        if (finishing) return;
        finishing = true;

        boolean isWinner = ("P1".equals(w) && isPlayer1) || ("P2".equals(w) && !isPlayer1);
        if (isWinner) {
            User user = SharedPreferencesUtil.getUser(this);
            if (user != null && user.getId() != null) {
                if (databaseService != null) {
                    databaseService.updateUserWins(user.getId(), "onlineWins", new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Log.d(TAG, "Online wins updated successfully.");
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Log.e(TAG, "Failed to update online wins.", e);
                            Toast.makeText(OnlineGameActivity.this, "Could not save your win.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.w(TAG, "DatabaseService is null — cannot update online wins. Make sure BaseActivity initialized it.");
                }
            } else {
                Log.e(TAG, "User not found, cannot update wins.");
            }
        }

        String msg = "DRAW".equals(w) ? "🤝 Draw" :
                (isWinner) ? "🏆 You Win!" : "😢 You Lose";

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
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        if (cooldownTimer != null) {
            cooldownTimer.cancel();
            cooldownTimer = null;
        }
        if (gameListener != null) {
            gameRef.removeEventListener(gameListener);
            gameListener = null;
        }
        if (offsetListener != null && offsetRef != null) {
            offsetRef.removeEventListener(offsetListener);
            offsetListener = null;
        }
    }
}