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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OnlineGameActivity extends BaseActivity {

    private String roomId;
    private String playerId;
    private boolean isPlayer1 = false;
    private boolean isMyTurn = false;
    private boolean answerLocked = false;
    private boolean finishing = false;
    private long lastQuestionVersion = -1;

    private long defaultTurnMs = 60000;

    private TextView tvTurnInfo, tvTimer, tvScore, tvQuestion;
    private Button btnA, btnB, btnC, btnEndGame;

    private DatabaseReference roomRef;
    private DatabaseReference gameRef;
    private ValueEventListener gameListener;

    private CountDownTimer turnTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        roomId   = getIntent().getStringExtra("ROOM_ID");
        playerId = getIntent().getStringExtra("PLAYER_ID");

        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        tvTimer    = findViewById(R.id.tvTimer);
        tvScore    = findViewById(R.id.tvScore);
        tvQuestion = findViewById(R.id.tvQuestion);

        btnA = findViewById(R.id.btnAnswerA);
        btnB = findViewById(R.id.btnAnswerB);
        btnC = findViewById(R.id.btnAnswerC);
        btnEndGame = findViewById(R.id.btnEndGame);

        roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId);
        gameRef = roomRef.child("game");

        roomRef.onDisconnect().removeValue();

        btnA.setOnClickListener(v -> onAnswerClicked("A", btnA));
        btnB.setOnClickListener(v -> onAnswerClicked("B", btnB));
        btnC.setOnClickListener(v -> onAnswerClicked("C", btnC));
        btnEndGame.setOnClickListener(v -> deleteRoomAndExit());

        initFromRoom();
    }

    private void initFromRoom() {
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    goHome();
                    return;
                }

                String hostId = snap.child("hostId").getValue(String.class);
                isPlayer1 = playerId.equals(hostId);

                Long timeMs = snap.child("timeMs").getValue(Long.class);
                if (timeMs != null) defaultTurnMs = timeMs;

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
        Map<String, Object> base = new HashMap<>();
        base.put("p1Score", 0);
        base.put("p2Score", 0);
        base.put("currentTurn", "p1");
        base.put("questionVersion", 0);
        base.put("timeMs", defaultTurnMs);

        long now = System.currentTimeMillis();
        base.put("turnEndAt", now + defaultTurnMs);

        gameRef.setValue(base);
        setNewQuestionInGame();
    }

    private void setNewQuestionInGame() {
        DatabaseReference qRef = FirebaseDatabase.getInstance().getReference("questions");

        qRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                long count = snap.getChildrenCount();
                if (count == 0) return;

                int index = (int) (Math.random() * count);
                DataSnapshot qSnap = snap.child(String.valueOf(index));

                String q = qSnap.child("question").getValue(String.class);
                String right = qSnap.child("rightAnswer").getValue(String.class);

                ArrayList<String> wrong = new ArrayList<>();
                for (DataSnapshot w : qSnap.child("wrongAnswers").getChildren()) {
                    wrong.add(w.getValue(String.class));
                }

                ArrayList<String> all = new ArrayList<>();
                all.add(right);
                all.addAll(wrong);
                Collections.shuffle(all);

                Map<String, Object> qMap = new HashMap<>();
                qMap.put("text", q);
                qMap.put("a", all.get(0));
                qMap.put("b", all.get(1));
                qMap.put("c", all.get(2));

                String correctKey = all.indexOf(right) == 0 ? "A" :
                        all.indexOf(right) == 1 ? "B" : "C";

                qMap.put("correct", correctKey);

                gameRef.child("question").setValue(qMap);
                gameRef.child("questionVersion").setValue(System.currentTimeMillis());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startGameListener() {
        gameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {

                if (!snap.exists()) return;

                long p1 = getLong(snap.child("p1Score"), 0);
                long p2 = getLong(snap.child("p2Score"), 0);
                tvScore.setText("P1: " + p1 + " | P2: " + p2);

                String turn = snap.child("currentTurn").getValue(String.class);
                if (turn == null) turn = "p1";

                isMyTurn = (isPlayer1 && turn.equals("p1")) || (!isPlayer1 && turn.equals("p2"));
                tvTurnInfo.setText(isMyTurn ? "Your turn" : "Opponent's turn");

                // ✔ תמיד טוען שאלה כשהיא קיימת
                if (snap.hasChild("question")) {
                    loadNewQuestionUI(snap);
                }

                // ✔ מפעיל טיימר
                Long endAt = snap.child("turnEndAt").getValue(Long.class);
                if (endAt != null) {
                    long now = System.currentTimeMillis();
                    startTimer(Math.max(0, endAt - now));
                }

                // ✔ לחצנים רק בתור שלי
                setButtonsEnabled(isMyTurn && !answerLocked);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        gameRef.addValueEventListener(gameListener);
    }


    private void onAnswerClicked(String key, Button clicked) {
        if (!isMyTurn || answerLocked || finishing) return;

        answerLocked = true;
        setButtonsEnabled(false);

        gameRef.child("question").child("correct")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        String correct = s.getValue(String.class);
                        showAnswerColors(key, correct, clicked);
                        updateScore(isPlayer1 ? "p1" : "p2", key.equals(correct));
                        if (isPlayer1) setNewQuestionInGame();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateScore(String playerKey, boolean addPoint) {
        gameRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData data) {

                if (addPoint) {
                    long score = 0;
                    if (data.child(playerKey + "Score").getValue(Long.class) != null)
                        score = data.child(playerKey + "Score").getValue(Long.class);
                    data.child(playerKey + "Score").setValue(score + 1);
                }
                return com.google.firebase.database.Transaction.success(data);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {}
        });
    }

    private void startTimer(long ms) {
        if (turnTimer != null) turnTimer.cancel();

        turnTimer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long l) {
                long sec = l / 1000;
                tvTimer.setText(String.format("%02d:%02d", sec / 60, sec % 60));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                answerLocked = true;

                if (isMyTurn) {
                    gameRef.child("currentTurn").setValue(isPlayer1 ? "p2" : "p1");
                    long endAt = System.currentTimeMillis() + defaultTurnMs;
                    gameRef.child("turnEndAt").setValue(endAt);
                    if (!isPlayer1) setNewQuestionInGame();
                }
            }
        };

        turnTimer.start();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnA.setEnabled(enabled);
        btnB.setEnabled(enabled);
        btnC.setEnabled(enabled);
    }

    private void loadNewQuestionUI(DataSnapshot snap) {
        String text = snap.child("question").child("text").getValue(String.class);
        tvQuestion.setText(text == null ? "" : text);

        btnA.setText(snap.child("question").child("a").getValue(String.class));
        btnB.setText(snap.child("question").child("b").getValue(String.class));
        btnC.setText(snap.child("question").child("c").getValue(String.class));

        resetButtonColors();
        answerLocked = false;
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

        Button correctBtn = correct.equals("A") ? btnA :
                correct.equals("B") ? btnB : btnC;

        if (chosen.equals(correct)) {
            clicked.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        } else {
            clicked.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            correctBtn.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
        }
    }

    private long getLong(DataSnapshot snap, long def) {
        Long v = snap.getValue(Long.class);
        return v == null ? def : v;
    }

    private void deleteRoomAndExit() {
        if (finishing) return;
        finishing = true;

        if (turnTimer != null) turnTimer.cancel();

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
        if (gameListener != null) gameRef.removeEventListener(gameListener);
    }
}
