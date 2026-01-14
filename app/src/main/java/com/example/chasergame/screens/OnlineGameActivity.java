package com.example.chasergame.screens;

import android.content.Intent;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class OnlineGameActivity extends BaseActivity {

    private String roomId;
    private String playerId;

    private TextView tvTurnInfo, tvTimer, tvScore, tvQuestion;
    private Button btnAnswerA, btnAnswerB, btnAnswerC, btnEndGame;

    private DatabaseReference roomRef;
    private boolean finishing = false;
    private boolean isPlayer1 = false;

    private CountDownTimer turnTimer;
    private long totalTurnTimeMs = 120_000; // updated from DB

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

        roomRef = FirebaseDatabase.getInstance().getReference("games").child(roomId);

        roomRef.onDisconnect().removeValue();

        initViews();
        setupListeners();
    }

    private void initViews() {
        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        tvTimer = findViewById(R.id.tvTimer);
        tvScore = findViewById(R.id.tvScore);
        tvQuestion = findViewById(R.id.tvQuestion);

        btnAnswerA = findViewById(R.id.btnAnswerA);
        btnAnswerB = findViewById(R.id.btnAnswerB);
        btnAnswerC = findViewById(R.id.btnAnswerC);
        btnEndGame = findViewById(R.id.btnEndGame);

        btnEndGame.setOnClickListener(v -> endGameAndDeleteRoom());
    }

    private void setupListeners() {

        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    Toast.makeText(OnlineGameActivity.this, "Room not found", Toast.LENGTH_SHORT).show();
                    goHome();
                    return;
                }

                Long timeMs = snapshot.child("timeMs").getValue(Long.class);
                if (timeMs != null) totalTurnTimeMs = timeMs;

                String p1 = snapshot.child("player1Id").getValue(String.class);

                isPlayer1 = p1 != null && p1.equals(playerId);

                if (isPlayer1) {
                    createNewQuestion();
                    roomRef.child("currentTurn").setValue("P1");
                    roomRef.child("p1Score").setValue(0);
                    roomRef.child("p2Score").setValue(0);
                }

                listenToGameChanges();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void listenToGameChanges() {

        roomRef.child("currentTurn").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                String turn = snapshot.getValue(String.class);

                if (turn == null) return;

                boolean myTurn = (turn.equals("P1") && isPlayer1) || (turn.equals("P2") && !isPlayer1);

                tvTurnInfo.setText(myTurn ? "Your Turn" : "Opponent's Turn");

                if (myTurn) startTurnTimer();
                else stopTimer();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        roomRef.child("question").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) return;

                String text = snapshot.child("text").getValue(String.class);
                String A = snapshot.child("A").getValue(String.class);
                String B = snapshot.child("B").getValue(String.class);
                String C = snapshot.child("C").getValue(String.class);

                tvQuestion.setText(text);

                btnAnswerA.setText(A);
                btnAnswerB.setText(B);
                btnAnswerC.setText(C);

                setupAnswerButtons();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        roomRef.child("p1Score").addValueEventListener(scoreListener);
        roomRef.child("p2Score").addValueEventListener(scoreListener);
    }

    private final ValueEventListener scoreListener =
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    roomRef.get().addOnSuccessListener(roomSnap -> {

                        Integer p1 = roomSnap.child("p1Score").getValue(Integer.class);
                        Integer p2 = roomSnap.child("p2Score").getValue(Integer.class);

                        if (p1 == null) p1 = 0;
                        if (p2 == null) p2 = 0;

                        tvScore.setText("P1: " + p1 + " | P2: " + p2);
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };

    private void setupAnswerButtons() {
        btnAnswerA.setOnClickListener(v -> onAnswerSelected("A"));
        btnAnswerB.setOnClickListener(v -> onAnswerSelected("B"));
        btnAnswerC.setOnClickListener(v -> onAnswerSelected("C"));
    }

    private void onAnswerSelected(String answer) {

        roomRef.child("currentTurn").get().addOnSuccessListener(snapshot -> {
            String turn = snapshot.getValue(String.class);

            boolean myTurn = (turn.equals("P1") && isPlayer1) || (turn.equals("P2") && !isPlayer1);

            if (!myTurn) return;

            roomRef.child("question").child("correct").get().addOnSuccessListener(correctSnap -> {

                String correct = correctSnap.getValue(String.class);

                if (correct != null && correct.equals(answer)) {
                    scorePoint();
                }

                switchTurn();
                createNewQuestion();
            });
        });
    }

    private void switchTurn() {
        roomRef.child("currentTurn").setValue(isPlayer1 ? "P2" : "P1");
    }

    private void scorePoint() {
        if (isPlayer1) {
            roomRef.child("p1Score").get().addOnSuccessListener(s -> {
                int sc = s.getValue(Integer.class) == null ? 0 : s.getValue(Integer.class);
                roomRef.child("p1Score").setValue(sc + 1);
            });
        } else {
            roomRef.child("p2Score").get().addOnSuccessListener(s -> {
                int sc = s.getValue(Integer.class) == null ? 0 : s.getValue(Integer.class);
                roomRef.child("p2Score").setValue(sc + 1);
            });
        }
    }

    private void createNewQuestion() {

        if (!isPlayer1) return;

        Random r = new Random();
        int x = r.nextInt(9) + 1;
        int y = r.nextInt(9) + 1;

        int correct = x + y;

        Map<String, Object> q = new HashMap<>();
        q.put("text", x + " + " + y + " = ?");
        q.put("A", String.valueOf(correct));
        q.put("B", String.valueOf(correct + 1));
        q.put("C", String.valueOf(correct - 1));
        q.put("correct", "A");

        roomRef.child("question").setValue(q);
    }

    private void startTurnTimer() {
        stopTimer();

        turnTimer = new CountDownTimer(totalTurnTimeMs, 1000) {
            @Override
            public void onTick(long msLeft) {
                tvTimer.setText(formatTime(msLeft));
            }

            @Override
            public void onFinish() {
                switchTurn();
            }
        }.start();
    }

    private void stopTimer() {
        if (turnTimer != null) turnTimer.cancel();
    }

    private String formatTime(long ms) {
        long totalSec = ms / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void endGameAndDeleteRoom() {
        if (finishing) return;
        finishing = true;

        roomRef.removeValue().addOnCompleteListener(task -> {
            Toast.makeText(this, "Game ended.", Toast.LENGTH_SHORT).show();
            goHome();
        });
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        endGameAndDeleteRoom();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!finishing) {
            finishing = true;
            roomRef.removeValue();
        }
    }
}
